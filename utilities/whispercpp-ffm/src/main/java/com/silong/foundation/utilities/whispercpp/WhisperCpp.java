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
import static com.silong.foundation.utilities.whispercpp.WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp.*;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_ahead.n_head;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_ahead.n_text_layer;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_aheads.heads;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_aheads.n_heads;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_context_params.*;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.*;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search.beam_size;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search.patience;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.detect_language;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.silong.foundation.utilities.whispercpp.WhisperConfig.BeamSearch;
import com.silong.foundation.utilities.whispercpp.WhisperConfig.WhisperContextParams;
import com.silong.foundation.utilities.whispercpp.WhisperConfig.WhisperFullParams;
import com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1;
import com.silong.foundation.utilities.whispercpp.generated.whisper_ahead;
import com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search;
import com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.greedy;
import java.io.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
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
class WhisperCpp implements Whisper {

  static {
    loadLibrary("libwhisper", "libs");
  }

  /** whisper上下文 */
  private final MemorySegment whisperContext;

  /** whisper全量参数 */
  private final MemorySegment whisperFullParams;

  /**
   * 构造方法
   *
   * @param config whisper配置
   */
  WhisperCpp(@NonNull WhisperConfig config) {
    String modelPath = checkModelExist(config.getModelPath());
    config.setModelPath(modelPath); // 更新模型文件路径
    log.info("modelPath: {}", modelPath);
    try (Arena arena = Arena.ofConfined()) {
      whisperContext = buildWhisperContext(arena, config.getContextParams(), modelPath);
      whisperFullParams = buildWhisperFullParams(arena, config.getFullParams());
    }
  }

  private static MemorySegment buildWhisperContext(
      Arena arena, WhisperContextParams config, String modelPath) {
    return whisper_init_from_file_with_params(
            arena.allocateFrom(modelPath, UTF_8), buildWhisperContextParams(arena, config))
        .reinterpret(Arena.ofAuto(), WhisperCpp_1::whisper_free); // GC托管
  }

  private static MemorySegment buildWhisperFullParams(Arena arena, WhisperFullParams config) {
    MemorySegment params =
        whisper_full_default_params(arena, WHISPER_SAMPLING_GREEDY.ordinal())
            .reinterpret(Arena.ofAuto(), WhisperCpp_1::whisper_free_params); // GC 托管
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
    BeamSearch beamSearch = config.getBeam_search();
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
    File file = Path.of(modelPath).toFile();
    if (!(file.exists() && file.isFile() && file.canRead())) {
      throw new IllegalArgumentException("Invalid model file: " + modelPath);
    }
    return file.getCanonicalPath();
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
        parse(dtw_aheads_preset(whisperContextParams), WhisperAlignmentHeadsPreset.values()),
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

  @Override
  @SneakyThrows({IOException.class, UnsupportedAudioFileException.class})
  public String speech2Text(@NonNull File f) {
    // sample is a 16 bit int 16000hz little endian wav file
    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
        Arena arena = Arena.ofConfined()) {

      // read all the available data to a little endian capture buffer
      int available = audioInputStream.available();
      ByteBuffer captureBuffer = ByteBuffer.allocate(available);
      captureBuffer.order(LITTLE_ENDIAN);
      int read = audioInputStream.read(captureBuffer.array());
      if (read != available) {
        throw new IOException("Failed to read " + f.getCanonicalPath());
      }

      int length = captureBuffer.capacity() / 2;

      // obtain the 16 int audio samples, short type in java
      var shortBuffer = captureBuffer.asShortBuffer();
      // transform the samples to f32 samples
      MemorySegment samples = arena.allocate(sequenceLayout(length, C_FLOAT));
      var i = 0;
      while (shortBuffer.hasRemaining()) {
        samples.setAtIndex(
            C_FLOAT,
            i++,
            Float.max(-1f, Float.min(((float) shortBuffer.get()) / (float) Short.MAX_VALUE, 1f)));
      }

      // 开始并行处理音频采样
      if (whisper_full_parallel(whisperContext, whisperFullParams, samples, length, 1) != 0) {
        throw new IOException(String.format("Failed to process audio %s.", f.getCanonicalPath()));
      }

      int segments = whisper_full_n_segments(whisperContext);
      for (i = 0; i < segments; i++) {
        MemorySegment charPtr = whisper_full_get_segment_text(whisperContext, i);
        String text = charPtr.getString(0, UTF_8);
        log.info("text: {}", text);
        return text;
      }
      return "";
    }
  }

  private static WhisperAlignmentHeadsPreset parse(int v, WhisperAlignmentHeadsPreset[] values) {
    return Arrays.stream(values)
        .filter(value -> value.getValue() == v)
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "value: %d, values: %s",
                        v,
                        Arrays.stream(values).map(Enum::name).collect(joining(", ", "[", "]")))));
  }
}
