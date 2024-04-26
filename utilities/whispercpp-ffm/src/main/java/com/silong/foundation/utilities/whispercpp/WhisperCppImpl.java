/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package com.silong.foundation.utilities.whispercpp;

import static com.silong.foundation.utilities.nlloader.NativeLibLoader.loadLibrary;
import static com.silong.foundation.utilities.whispercpp.WhisperAlignmentHeadsPreset.parse;
import static com.silong.foundation.utilities.whispercpp.WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp.*;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.false_;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_ahead.n_head;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_ahead.n_text_layer;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_aheads.heads;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_aheads.n_heads;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_context_params.*;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.*;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search.beam_size;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search.patience;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.detect_language;
import static java.lang.Runtime.getRuntime;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.joining;

import com.silong.foundation.utilities.whispercpp.WhisperConfig.WhisperContextParams;
import com.silong.foundation.utilities.whispercpp.WhisperConfig.WhisperFullParams;
import com.silong.foundation.utilities.whispercpp.generated.whisper_ahead;
import java.io.*;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.sound.sampled.*;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 语音识别
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-20 15:33
 */
@Slf4j
class WhisperCppImpl implements Whisper {

  private static final Linker LINKER = Linker.nativeLinker();

  private static final MethodHandle FREE =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

  private static final ThreadLocal<byte[]> BYTE_BUFFER = new ThreadLocal<>();

  static {
    loadLibrary("libwhisper", "libs");
  }

  /** 默认whisper上下文 */
  private final MemorySegment defaultWhisperContext;

  /** 默认whisper全量参数 */
  private final MemorySegment defaultWhisperFullParams;

  /** 配置参数 */
  private final WhisperConfig config;

  /**
   * 构造方法
   *
   * @param config whisper配置
   */
  WhisperCppImpl(@NonNull WhisperConfig config) {
    MemorySegment systemInfo = whisper_print_system_info();
    try (Arena arena = Arena.ofConfined()) {
      // 模型是否支持多语言，如果不支持则回退到英文，也不支持翻译到英文
      WhisperFullParams fullParams = config.getFullParams();
      String language = fullParams.getLanguage();

      // 检测识别语言是否支持
      if (!"auto".equals(language) && whisper_lang_id(arena.allocateFrom(language, UTF_8)) == -1) {
        throw new IllegalArgumentException("unknown language: " + language);
      }

      log.info(
          "system_info: n_threads = {} / {} | {}",
          fullParams.getN_threads() * fullParams.getN_processors(),
          getRuntime().availableProcessors(),
          systemInfo.getString(0, UTF_8));

      String modelPath = checkModelExist(config.getModelPath());
      config.setModelPath(modelPath); // 更新模型文件路径
      log.info("modelPath: {}", modelPath);
      defaultWhisperContext = buildWhisperContext(config.getContextParams(), modelPath);

      // 检查模型是否支持多语言
      if (whisper_is_multilingual(defaultWhisperContext) == false_()) {
        if (!"en".equals(language) || fullParams.isTranslate()) {
          fullParams.setLanguage("en");
          fullParams.setTranslate(false);
        }
        log.warn("model is not multilingual, ignoring language and translation options.");
      }

      defaultWhisperFullParams = buildWhisperFullParams(fullParams);
      this.config = config;
    } finally {
      free(systemInfo);
    }
  }

  private static MemorySegment buildWhisperContext(WhisperContextParams config, String modelPath) {
    Arena arena = Arena.global();
    return whisper_init_from_file_with_params(
        arena.allocateFrom(modelPath, UTF_8), buildWhisperContextParams(arena, config));
  }

  private static MemorySegment buildWhisperFullParams(WhisperFullParams config) {
    Arena arena = Arena.global();
    MemorySegment params = whisper_full_default_params(arena, WHISPER_SAMPLING_GREEDY.getValue());
    language(params, arena.allocateFrom(config.getLanguage(), UTF_8));
    detect_language(params, config.isDetect_language());
    strategy(params, config.getStrategy().getValue());
    n_threads(params, config.getN_threads());
    n_max_text_ctx(params, config.getN_max_text_ctx());
    offset_ms(params, config.getOffset_ms());
    duration_ms(params, config.getDuration_ms());
    translate(params, config.isTranslate());
    no_context(params, config.isNo_context());
    no_timestamps(params, config.isNo_timestamps());
    single_segment(params, config.isSingle_segment());
    print_special(params, config.isPrint_special());
    print_progress(params, config.isPrint_progress());
    print_realtime(params, config.isPrint_realtime());
    print_timestamps(params, config.isPrint_timestamps());
    token_timestamps(params, config.isToken_timestamps());
    thold_pt(params, config.getThold_pt());
    thold_ptsum(params, config.getThold_ptsum());
    max_len(params, config.getMax_len());
    split_on_word(params, config.isSplit_on_word());
    max_tokens(params, config.getMax_tokens());
    speed_up(params, config.isSpeed_up());
    debug_mode(params, config.isDebug_mode());
    audio_ctx(params, config.getAudio_ctx());
    tdrz_enable(params, config.isTdrz_enable());
    String suppressRegex = config.getSuppress_regex();
    if (suppressRegex != null && !suppressRegex.isEmpty()) {
      suppress_regex(params, arena.allocateFrom(suppressRegex, UTF_8));
    }
    String initialPrompt = config.getInitial_prompt();
    if (initialPrompt != null && !initialPrompt.isEmpty()) {
      initial_prompt(params, arena.allocateFrom(initialPrompt, UTF_8));
    }
    int[] promptTokens = config.getPrompt_tokens();
    if (promptTokens != null && promptTokens.length != 0) {
      MemorySegment pts = arena.allocateFrom(C_INT, promptTokens);
      for (int i = 0; i < promptTokens.length; i++) {
        pts.setAtIndex(C_INT, i, promptTokens[i]);
      }
      prompt_tokens(params, pts);
    }
    prompt_n_tokens(params, config.getPrompt_n_tokens());
    suppress_blank(params, config.isSuppress_blank());
    suppress_non_speech_tokens(params, config.isSuppress_non_speech_tokens());
    temperature(params, config.getTemperature());

    max_initial_ts(params, config.getMax_initial_ts());
    length_penalty(params, config.getLength_penalty());

    temperature_inc(params, config.getTemperature_inc());
    entropy_thold(params, config.getEntropy_thold());
    logprob_thold(params, config.getLogprob_thold());

    no_speech_thold(params, config.getNo_speech_thold());

    MemorySegment memorySegment = greedy.allocate(arena);
    greedy.best_of(memorySegment, config.getGreedy().getBest_of());
    greedy(params, memorySegment);

    MemorySegment memorySegment1 = beam_search.allocate(arena);
    WhisperConfig.BeamSearch beamSearch = config.getBeam_search();
    patience(memorySegment1, beamSearch.getPatience());
    beam_size(memorySegment1, beamSearch.getBeam_size());
    beam_search(params, memorySegment1);

    log.info(
        "language: {}, detect_language: {}",
        language(params).getString(0, UTF_8),
        detect_language(params));

    return params;
  }

  /**
   * 检查模型文件是否可加载
   *
   * @param modelPath 模型文件路径
   * @return 模型文件
   */
  @SneakyThrows(IOException.class)
  private static String checkModelExist(String modelPath) {
    return validate(Path.of(modelPath).toFile(), "Invalid model file: ").getCanonicalPath();
  }

  /**
   * 根据配置生成上下文参数
   *
   * @param arena 内存区域
   * @param contextParamsConfig 上下文参数配置
   * @return 上下文参数
   */
  private static MemorySegment buildWhisperContextParams(
      Arena arena, WhisperContextParams contextParamsConfig) {
    MemorySegment whisperContextParams = whisper_context_default_params(arena);
    use_gpu(whisperContextParams, contextParamsConfig.isUse_gpu());
    gpu_device(whisperContextParams, contextParamsConfig.getGpu_device());
    dtw_mem_size(whisperContextParams, contextParamsConfig.getDtw_mem_size());
    dtw_aheads_preset(whisperContextParams, contextParamsConfig.getDtw_aheads_preset().getValue());
    dtw_n_top(whisperContextParams, contextParamsConfig.getDtw_n_top());
    MemorySegment dtwAheads = dtw_aheads(whisperContextParams);
    WhisperConfig.WhisperAheads dtwAheadsConfig = contextParamsConfig.getDtw_aheads();
    n_heads(dtwAheads, dtwAheadsConfig.getN_heads());
    WhisperConfig.WhisperAhead[] headsConfig = dtwAheadsConfig.getHeads();
    int headsSize = -1;
    if (headsConfig != null && (headsSize = headsConfig.length) != 0) {
      AtomicInteger index = new AtomicInteger(0);
      MemorySegment heads = whisper_ahead.allocateArray(headsConfig.length, arena);
      heads
          .elements(sequenceLayout(headsConfig.length, whisper_ahead.layout()))
          .forEachOrdered(
              head -> {
                WhisperConfig.WhisperAhead whisperAhead = headsConfig[index.getAndIncrement()];
                n_text_layer(head, whisperAhead.getN_text_layer());
                n_head(head, whisperAhead.getN_head());
              });
      heads(dtwAheads, heads);
    }

    log.info(
        "whisper_context_params: [use_gpu: {}, gpu_device: {}, dtw_mem_size: {}MB, dtw_n_top: {}, dtw_aheads_preset: {}, dtw_aheads: [n_heads: {}, heads:{}]]",
        use_gpu(whisperContextParams),
        gpu_device(whisperContextParams),
        dtw_mem_size(whisperContextParams) / 1024 / 1024,
        dtw_n_top(whisperContextParams),
        parse(dtw_aheads_preset(whisperContextParams)),
        n_heads(dtwAheads),
        NULL.equals(heads(dtwAheads))
            ? "NULL"
            : heads(dtwAheads)
                .elements(sequenceLayout(headsSize, whisper_ahead.layout()))
                .map(
                    head ->
                        String.format(
                            "[n_text_layer: %d, n_head: %d]", n_text_layer(head), n_head(head)))
                .collect(joining(",", "[", "]")));
    return whisperContextParams;
  }

  /**
   * 读取wav文件，转为pcm float数据
   *
   * @param audioInputStreamSupplier 语音数据流
   * @return pcm float数据
   */
  @SneakyThrows({IOException.class, UnsupportedAudioFileException.class})
  private static float[] readWav(Supplier<AudioInputStream> audioInputStreamSupplier) {
    try (AudioInputStream audioInputStream = audioInputStreamSupplier.get()) {
      AudioFormat format = audioInputStream.getFormat();

      if (format.getSampleRate() != SUPPORTED_SAMPLED_RATE) {
        throw new UnsupportedAudioFileException(
            String.format(
                "Only supports audio files with samplingRate: %dKHZ",
                ((int) (SUPPORTED_SAMPLED_RATE / 1000))));
      }

      if (format.getSampleSizeInBits() != SUPPORTED_SAMPLED_BITS) {
        throw new UnsupportedAudioFileException(
            String.format(
                "Only supports audio files with samplingBits: %d", SUPPORTED_SAMPLED_BITS));
      }

      // read all the available data to a buffer
      int available = audioInputStream.available();

      byte[] bytes = BYTE_BUFFER.get();
      if (!(bytes != null && bytes.length >= available)) {
        bytes = new byte[available];
      }

      if (audioInputStream.read(bytes) != available) {
        throw new IOException("Failed to read audioInputStream.");
      }

      ShortBuffer shortBuf =
          ByteBuffer.wrap(bytes, 0, available)
              .order(format.isBigEndian() ? BIG_ENDIAN : LITTLE_ENDIAN)
              .asShortBuffer();

      // 音频数据的帧数量
      int frames = available / format.getFrameSize();
      float[] samples = new float[frames];
      if (format.getChannels() == 1) {
        for (int i = 0; i < frames; i++) {
          samples[i] = shortBuf.get(i) * 1.0f / 32768.0f;
        }
      } else {
        for (int i = 0; i < frames; i++) {
          samples[i] = (shortBuf.get(2 * i) + shortBuf.get(2 * i + 1)) * 1.0f / 65536.0f;
        }
      }
      return samples;
    }
  }

  private static void logDebugResultWithTimes(MemorySegment ctx, int segment, String text) {
    if (log.isDebugEnabled()) {
      long startTime = whisper_full_get_segment_t0(ctx, segment);
      long endTime = whisper_full_get_segment_t1(ctx, segment);
      log.debug("[{} --- {}] text: {}", formatHHmmssSSS(startTime), formatHHmmssSSS(endTime), text);
    }
  }

  @Nullable
  @Override
  public String[] speech2Text(InputStream inputStream) {
    return analyzeAudioData(
        defaultWhisperContext,
        defaultWhisperFullParams,
        config.getFullParams().getN_processors(), // 计算用核心数
        () -> readWav(() -> getAudioInputStream(inputStream)),
        ctx -> {
          int segments;
          if ((segments = whisper_full_n_segments(ctx)) > 0) {
            String[] result = new String[segments];
            for (int i = 0; i < segments; i++) {
              MemorySegment charPtr =
                  whisper_full_get_segment_text(ctx, i); // 此处无需释放返回的const char * ，此指针为栈分配
              String text = NULL.equals(charPtr) ? "" : charPtr.getString(0, UTF_8);
              logDebugResultWithTimes(ctx, i, text);
              result[i] = text;
            }
            return result;
          }
          return null;
        });
  }

  @Override
  @Nullable
  public String[] speech2Text(File f) throws IOException {
    try (InputStream inputStream =
        new BufferedInputStream(new FileInputStream(validate(f, "Invalid wav file: ")))) {
      return speech2Text(inputStream);
    }
  }

  @Override
  @Nullable
  public String recognizeLanguage(File f) {
    return analyzeAudioData(
        defaultWhisperContext,
        defaultWhisperFullParams,
        config.getFullParams().getN_processors(), // 计算用核心数
        () -> readWav(() -> getAudioInputStream(f)),
        ctx -> {
          MemorySegment charPtr =
              whisper_lang_str(whisper_full_lang_id(ctx)); // 此处无需释放返回的const char * ，此指针为栈分配
          return NULL.equals(charPtr) ? null : charPtr.getString(0, UTF_8);
        });
  }

  private static <T> T analyzeAudioData(
      @NonNull MemorySegment whisperContext,
      @NonNull MemorySegment whisperFullParams,
      int nProcessor,
      @NonNull Supplier<float[]> audioDataSupplier,
      @NonNull Function<MemorySegment, T> handler) {
    try (Arena arena = Arena.ofConfined()) {
      // 开始并行处理音频采样，非线程安全
      float[] samples = audioDataSupplier.get();
      if (whisper_full_parallel(
              whisperContext,
              whisperFullParams,
              arena.allocateFrom(C_FLOAT, samples), // 采样数据
              samples.length,
              nProcessor)
          != 0) {
        throw new IllegalStateException("Failed to analyze audio data.");
      }
      return handler.apply(whisperContext);
    }
  }

  private static String formatHHmmssSSS(long millis) {
    long hours = MILLISECONDS.toHours(millis);
    long minutes = MILLISECONDS.toMinutes(millis);
    long seconds = MILLISECONDS.toSeconds(millis);
    return String.format(
        "%02d:%02d:%02d.%03d",
        hours,
        minutes - HOURS.toMinutes(hours),
        seconds - MINUTES.toSeconds(minutes),
        millis - SECONDS.toMillis(seconds));
  }

  private static File validate(@NonNull File f, String message) throws IOException {
    if (!(f.exists() && f.isFile() && f.canRead())) {
      throw new IllegalArgumentException(String.format(message + f.getCanonicalPath()));
    }
    return f;
  }

  @SneakyThrows({IOException.class, UnsupportedAudioFileException.class})
  private static AudioInputStream getAudioInputStream(InputStream inputStream) {
    return AudioSystem.getAudioInputStream(
        inputStream instanceof BufferedInputStream
            ? inputStream
            : new BufferedInputStream(inputStream));
  }

  @SneakyThrows({IOException.class, UnsupportedAudioFileException.class})
  private static AudioInputStream getAudioInputStream(File f) {
    return AudioSystem.getAudioInputStream(validate(f, "Invalid wav file: "));
  }

  /**
   * 释放内存空间
   *
   * @param ptr 指针
   */
  @SneakyThrows
  static void free(MemorySegment ptr) {
    FREE.invokeExact(ptr);
  }
}
