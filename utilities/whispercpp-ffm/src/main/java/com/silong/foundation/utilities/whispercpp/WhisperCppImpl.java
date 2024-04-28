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
import static com.silong.foundation.utilities.whispercpp.Utils.*;
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
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.greedy.best_of;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.no_speech_thold;
import static java.lang.Runtime.getRuntime;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static javax.sound.sampled.AudioFileFormat.Type.WAVE;

import com.silong.foundation.utilities.whispercpp.WhisperConfig.BeamSearch;
import com.silong.foundation.utilities.whispercpp.WhisperConfig.WhisperContextParams;
import com.silong.foundation.utilities.whispercpp.WhisperConfig.WhisperFullParams;
import com.silong.foundation.utilities.whispercpp.generated.whisper_ahead;
import com.silong.foundation.utilities.whispercpp.generated.whisper_new_segment_callback;
import com.silong.foundation.utilities.whispercpp.generated.whisper_progress_callback;
import jakarta.annotation.Nullable;
import java.io.*;
import java.lang.foreign.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.sound.sampled.*;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.AudioInfo;

/**
 * 语音识别
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-20 15:33
 */
@Slf4j
class WhisperCppImpl implements Whisper {

  static {
    loadLibrary("libwhisper", "libs");
  }

  /** 缓存大小 */
  private static final int BUFFER_SIZE = 4 * 1024;

  /** wav文件使用的编码器 */
  private static final String TARGET_CODEC = "pcm_s16le";

  /** 音频数据缓存 */
  private static final ThreadLocal<ByteArrayOutputStream> BYTE_BUFFER_OUTPUT_STREAM =
      new ThreadLocal<>();

  /** 音频格式 */
  private static final ThreadLocal<AudioFormat> AUDIO_FORMAT = new ThreadLocal<>();

  /** byte buffer */
  private static final ThreadLocal<ByteArrayOutputStream> BYTE_BUFFER = new ThreadLocal<>();

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

  /**
   * 根据配置创建全量参数
   *
   * @param config 配置
   * @return 参数
   */
  @SneakyThrows
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
    best_of(memorySegment, config.getGreedy().getBest_of());
    greedy(params, memorySegment);

    MemorySegment memorySegment1 = beam_search.allocate(arena);
    BeamSearch beamSearch = config.getBeam_search();
    patience(memorySegment1, beamSearch.getPatience());
    beam_size(memorySegment1, beamSearch.getBeam_size());
    beam_search(params, memorySegment1);

    MemorySegment newSegmentCallbackPtr =
        configurePtr(
            config.getWhisperNewSegmentCallbackClassFQDN(),
            whisper_new_segment_callback.Function.class,
            aClass -> {
              whisper_new_segment_callback.Function callback = newInstance(aClass);
              MemorySegment ms = whisper_new_segment_callback.allocate(callback, arena);
              new_segment_callback(params, ms); // 注册回调函数
              return ms;
            });

    MemorySegment newSegmentUserDataPtr =
        configurePtr(
            config.getWhisperNewSegmentCallbackUserDataClassFQDN(),
            ForeignParams.class,
            aClass -> {
              ForeignParams userData = newInstance(aClass);
              MemorySegment userDataMemorySegment = userData.convertTo();
              new_segment_callback_user_data(params, userDataMemorySegment); // 注册回调函数
              return userDataMemorySegment;
            });

    MemorySegment progressCallbackPtr =
        configurePtr(
            config.getWhisperProgressCallbackClassFQDN(),
            whisper_progress_callback.Function.class,
            aClass -> {
              whisper_progress_callback.Function callback = newInstance(aClass);
              MemorySegment ms = whisper_progress_callback.allocate(callback, arena);
              progress_callback(params, ms); // 注册回调函数
              return ms;
            });

    MemorySegment progressUserDataPtr =
        configurePtr(
            config.getWhisperProgressCallbackUserDataClassFQDN(),
            ForeignParams.class,
            aClass -> {
              ForeignParams userData = newInstance(aClass);
              MemorySegment userDataMemorySegment = userData.convertTo();
              progress_callback_user_data(params, userDataMemorySegment); // 注册回调函数
              return userDataMemorySegment;
            });

    if (log.isDebugEnabled()) {
      log.debug(
          "whisper_full_params: [language: {}, detect_language: {}, strategy: {}, n_threads: {}, n_max_text_ctx: {}{}offset_ms: {}, duration_ms: {}, translate: {}, no_context: {}, no_timestamps: {}{}single_segment: {}, print_special: {}, print_progress: {}, print_realtime: {}, print_timestamps: {}{}token_timestamps: {}, thold_pt: {}, thold_ptsum: {}, max_len: {}, split_on_word: {}{}max_tokens: {}, speed_up: {}, debug_mode: {}, audio_ctx: {}, tdrz_enable: {}{}suppress_regex: {}, initial_prompt: {}, prompt_tokens: {}, suppress_blank: {}, suppress_non_speech_tokens: {}{}temperature: {}, max_initial_ts: {}, length_penalty: {}, temperature_inc: {}, entropy_thold :{}{}logprob_thold: {}, no_speech_thold: {}, greedy: {}, beamSearch: {}, whisper_new_segment_callback: {}{}new_segment_callback_user_data: {}, whisper_progress_callback: {}, progress_callback_user_data: {}]",
          language(params).getString(0, UTF_8),
          detect_language(params),
          WhisperSamplingStrategy.parse(strategy(params)),
          n_threads(params),
          n_max_text_ctx(params),
          System.lineSeparator(),
          offset_ms(params),
          duration_ms(params),
          translate(params),
          no_context(params),
          no_timestamps(params),
          System.lineSeparator(),
          single_segment(params),
          print_special(params),
          print_progress(params),
          print_realtime(params),
          print_timestamps(params),
          System.lineSeparator(),
          token_timestamps(params),
          thold_pt(params),
          thold_ptsum(params),
          max_len(params),
          split_on_word(params),
          System.lineSeparator(),
          max_tokens(params),
          speed_up(params),
          debug_mode(params),
          audio_ctx(params),
          tdrz_enable(params),
          System.lineSeparator(),
          charPtr2ToString(suppress_regex(params), "null"),
          charPtr2ToString(initial_prompt(params), "null"),
          valueArrayToString(prompt_tokens(params), C_INT, prompt_n_tokens(params)),
          suppress_blank(params),
          suppress_non_speech_tokens(params),
          System.lineSeparator(),
          temperature(params),
          max_initial_ts(params),
          length_penalty(params),
          temperature_inc(params),
          entropy_thold(params),
          System.lineSeparator(),
          logprob_thold(params),
          no_speech_thold(params),
          greedyToString(greedy(params)),
          beamSearchToString(beam_search(params)),
          NULL.equals(new_segment_callback(params))
              ? "null"
              : newSegmentCallbackPtr.address() == new_segment_callback(params).address()
                  ? config.getWhisperNewSegmentCallbackClassFQDN()
                  : "unknown",
          System.lineSeparator(),
          NULL.equals(new_segment_callback_user_data(params))
              ? "null"
              : newSegmentUserDataPtr.address() == new_segment_callback_user_data(params).address()
                  ? config.getWhisperNewSegmentCallbackClassFQDN()
                  : "unknown",
          NULL.equals(progress_callback(params))
              ? "null"
              : progressCallbackPtr.address() == progress_callback(params).address()
                  ? config.getWhisperProgressCallbackClassFQDN()
                  : "unknown",
          NULL.equals(progress_callback_user_data(params))
              ? "null"
              : progressUserDataPtr.address() == progress_callback_user_data(params).address()
                  ? config.getWhisperProgressCallbackUserDataClassFQDN()
                  : "unknown");
    }

    return params;
  }

  private static MemorySegment configurePtr(
      String classFQDN, Class<?> superClass, Function<Class<?>, MemorySegment> configurator)
      throws ClassNotFoundException {
    if (classFQDN == null || classFQDN.isEmpty()) {
      return NULL;
    }

    Class<?> aClass = Class.forName(classFQDN);
    if (!superClass.isAssignableFrom(aClass)) {
      throw new IllegalArgumentException(
          String.format(
              "%s does not implement the interface %s.", classFQDN, superClass.getName()));
    }
    return configurator.apply(aClass);
  }

  private static MemorySegment buildWhisperContext(WhisperContextParams config, String modelPath) {
    Arena arena = Arena.global();
    return whisper_init_from_file_with_params(
        arena.allocateFrom(modelPath, UTF_8), buildWhisperContextParams(arena, config));
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
   * 使用ffmpeg将任意音频文件转换为wav文件
   *
   * @param inputStream 输入流
   * @return 转后后的临时文件
   * @throws IOException 异常
   * @throws EncoderException 异常
   */
  private static File any2Wav(InputStream inputStream) throws IOException, EncoderException {
    requireNonNull(inputStream, "inputStream must not be null.");
    Path tempInputFile = createTempFile();
    if (log.isDebugEnabled()) {
      log.debug("Create temporary input audio file: {}", tempInputFile.toFile().getCanonicalPath());
    }
    Files.copy(inputStream, tempInputFile, REPLACE_EXISTING);
    return any2Wav(tempInputFile.toFile());
  }

  /**
   * 使用ffmpeg将任意音频文件转换为wav文件
   *
   * @param audioFile 待转换音频文件
   * @return 转后后的临时文件
   * @throws IOException 异常
   * @throws EncoderException 异常
   */
  private static File any2Wav(File audioFile) throws IOException, EncoderException {
    validate(audioFile, "Invalid audio file: ");

    // 检查解码器
    Encoder encoder = new Encoder();
    String[] audioDecoders = encoder.getAudioDecoders();
    if (audioDecoders == null
        || audioDecoders.length == 0
        || Arrays.stream(audioDecoders).parallel().noneMatch(TARGET_CODEC::equals)) {
      throw new IllegalStateException(
          String.format(
              "%s not found in the supported list: %s.",
              TARGET_CODEC, Utils.toString(audioDecoders)));
    }

    if (log.isDebugEnabled()) {
      log.debug("Supported decoders: {}", Utils.toString(audioDecoders));
    }

    File tempAudioFile = createTempFile().toFile();
    MultimediaObject multimediaObject = new MultimediaObject(audioFile);
    AudioInfo audioInfo = multimediaObject.getInfo().getAudio();
    int channels = Math.min(MAX_CHANNELS, audioInfo.getChannels());
    encoder.encode(
        multimediaObject,
        tempAudioFile,
        new EncodingAttributes()
            .setOutputFormat(WAVE.getExtension())
            .setAudioAttributes(
                new AudioAttributes()
                    .setCodec(TARGET_CODEC) // PCM__SIGNED 16bit little_endian
                    .setBitRate(channels * SUPPORTED_SAMPLED_BITS * SUPPORTED_SAMPLED_RATE)
                    .setChannels(channels)
                    .setSamplingRate(SUPPORTED_SAMPLED_RATE)));

    if (log.isInfoEnabled()) {
      log.info(
          "source:{} --convert--> target:{}",
          audioFile.getCanonicalPath(),
          tempAudioFile.getCanonicalPath());
    }

    return tempAudioFile;
  }

  /**
   * 音频文件转whisper cpp要求的wav数据
   *
   * @param audioFile 音频文件
   * @return wav buffer
   * @throws IOException 异常
   * @throws EncoderException 异常
   * @throws UnsupportedAudioFileException 异常
   */
  private static ByteBuffer convert2WhisperCppWav(File audioFile)
      throws IOException, EncoderException, UnsupportedAudioFileException {
    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
      return toByteBuffer(audioInputStream);
    } catch (Exception e) {
      // 对于java原生不支持的格式使用ffmpeg处理
      return toByteBuffer(AudioSystem.getAudioInputStream(any2Wav(audioFile)));
    }
  }

  /**
   * 读取音频文件并转换为whisper cpp 支持的wav文件格式
   *
   * @param audioInputStream 音频数据输入流
   * @return wav音频数据
   * @throws IOException IO操作异常
   */
  private static ByteBuffer convert2WhisperCppWav(InputStream audioInputStream)
      throws IOException, EncoderException, UnsupportedAudioFileException {
    try (AudioInputStream in =
        audioInputStream instanceof AudioInputStream
            ? (AudioInputStream) audioInputStream
            : audioInputStream instanceof BufferedInputStream
                ? AudioSystem.getAudioInputStream(audioInputStream)
                : AudioSystem.getAudioInputStream(new BufferedInputStream(audioInputStream))) {
      return toByteBuffer(in);
    } catch (Exception e) {
      // 对于java原生不支持的格式使用ffmpeg处理
      return toByteBuffer(AudioSystem.getAudioInputStream(any2Wav(audioInputStream)));
    }
  }

  private static ByteBuffer toByteBuffer(AudioInputStream in)
      throws IOException, UnsupportedAudioFileException {
    // 读取音频数据格式
    AudioFormat sourceFormat = in.getFormat();
    if (log.isDebugEnabled()) {
      log.debug("sourceFormat: {}", sourceFormat);
    }

    // 如果输入音频数据已经满足要求，则直接读取数据，无需转换
    AudioFormat whisperCppWavFormat =
        new AudioFormat(
            SUPPORTED_SAMPLED_RATE,
            SUPPORTED_SAMPLED_BITS,
            sourceFormat.getChannels(),
            true,
            false);
    if (Utils.equals(sourceFormat, whisperCppWavFormat)) {
      AUDIO_FORMAT.set(sourceFormat);
      log.debug("No need to convert audio data.");
      return readFrom(in);
    }

    // 先转换为与原音频数据采样频率的目标格式(依赖库不支持)
    AudioFormat targetFormat =
        new AudioFormat(
            SUPPORTED_SAMPLED_RATE,
            SUPPORTED_SAMPLED_BITS,
            sourceFormat.getChannels(),
            true,
            false);
    AUDIO_FORMAT.set(targetFormat);
    if (log.isDebugEnabled()) {
      log.debug("targetFormat: {}", targetFormat);
    }

    // 转换为目标格式
    try (AudioInputStream input = AudioSystem.getAudioInputStream(targetFormat, in)) {
      ByteArrayOutputStream outputStream = BYTE_BUFFER_OUTPUT_STREAM.get();
      if (outputStream == null) {
        outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        BYTE_BUFFER_OUTPUT_STREAM.set(outputStream);
      } else {
        outputStream.reset();
      }

      // 把读取的音频数据转换为wav
      AudioSystem.write(input, WAVE, outputStream);

      try (AudioInputStream inputStream =
          AudioSystem.getAudioInputStream(
              new ByteArrayInputStream(outputStream.buf(), 0, outputStream.size()))) {
        return readFrom(inputStream);
      }
    }
  }

  private static ByteBuffer readFrom(AudioInputStream inputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = read2ByteArrayOutputStream(inputStream);
    return ByteBuffer.wrap(byteArrayOutputStream.buf(), 0, byteArrayOutputStream.size());
  }

  private static ByteArrayOutputStream read2ByteArrayOutputStream(AudioInputStream inputStream)
      throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = BYTE_BUFFER.get();
    if (byteArrayOutputStream == null) {
      byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
      BYTE_BUFFER.set(byteArrayOutputStream);
    } else {
      byteArrayOutputStream.reset();
    }

    // 音频流无法一个字节一个字节读取，只能一帧一帧读取，所以此处需要根据帧大小分配小缓存
    byte[] bytes =
        new byte
            [requireNonNull(AUDIO_FORMAT.get(), "Target audioFormat must not be null.")
                .getFrameSize()];
    int readBytes;
    while ((readBytes = inputStream.read(bytes)) != -1) {
      byteArrayOutputStream.write(bytes, 0, readBytes);
    }
    return byteArrayOutputStream;
  }

  private static float[] convert2FloatArray(ByteBuffer byteBuffer) {
    try {
      AudioFormat audioFormat = requireNonNull(AUDIO_FORMAT.get());
      if (log.isDebugEnabled()) {
        log.debug("Converted AudioFormat: {}", audioFormat);
      }

      ShortBuffer pcm16sl =
          byteBuffer.order(audioFormat.isBigEndian() ? BIG_ENDIAN : LITTLE_ENDIAN).asShortBuffer();

      // 音频数据的帧数量
      int frames = byteBuffer.limit() / audioFormat.getFrameSize();
      float[] samples = new float[frames];
      if (audioFormat.getChannels() == 1) {
        for (int i = 0; i < frames; i++) {
          samples[i] = pcm16sl.get(i) * 1.0f / 32768.0f;
        }
      } else {
        for (int i = 0; i < frames; i++) {
          samples[i] = (pcm16sl.get(2 * i) + pcm16sl.get(2 * i + 1)) * 1.0f / 65536.0f;
        }
      }
      return samples;
    } finally {
      AUDIO_FORMAT.remove();
    }
  }

  /**
   * 分析音频数据
   *
   * @param whisperContext 上下文
   * @param whisperFullParams 参数
   * @param nProcessor 并行计算核数
   * @param audioDataSupplier 音频数据供应器
   * @param handler 结果处理器
   * @return 结果
   * @param <T> 结果类型
   */
  private <T> T analyze(
      MemorySegment whisperContext,
      MemorySegment whisperFullParams,
      int nProcessor,
      Supplier<float[]> audioDataSupplier,
      Function<MemorySegment, T> handler) {
    try (Arena arena = Arena.ofConfined()) {
      // 开始并行处理音频采样，非线程安全
      float[] samples = audioDataSupplier.get();
      synchronized (this) {
        // Split the input audio in chunks and process each chunk separately using
        // whisper_full_with_state()
        // Result is stored in the default state of the context
        // Not thread safe if executed in parallel on the same context.
        // It seems this approach can offer some speedup in some cases.
        // However, the transcription accuracy can be worse at the beginning and end of each chunk.
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
  }

  @Nullable
  @Override
  public String[] speech2Text(InputStream inputStream)
      throws UnsupportedAudioFileException, IOException, EncoderException {
    requireNonNull(inputStream, "inputStream must not be null.");
    float[] audioData = convert2FloatArray(convert2WhisperCppWav(inputStream));
    return analyze(
        defaultWhisperContext,
        defaultWhisperFullParams,
        config.getFullParams().getN_processors(), // 计算用核心数
        () -> audioData,
        WhisperCppImpl::extractAnalyzedResult);
  }

  @Override
  @Nullable
  public String[] speech2Text(File audioFile)
      throws IOException, UnsupportedAudioFileException, EncoderException {
    float[] audioData =
        convert2FloatArray(convert2WhisperCppWav(validate(audioFile, "Invalid audio file: ")));
    return analyze(
        defaultWhisperContext,
        defaultWhisperFullParams,
        config.getFullParams().getN_processors(), // 计算用核心数
        () -> audioData,
        WhisperCppImpl::extractAnalyzedResult);
  }

  /**
   * 从上下文中提取识别结果
   *
   * @param ctx whisper.cpp上下文
   * @return 识别结果
   */
  private static String[] extractAnalyzedResult(MemorySegment ctx) {
    int segments;
    if ((segments = whisper_full_n_segments(ctx)) > 0) {
      String[] result = new String[segments];
      for (int i = 0; i < segments; i++) {
        MemorySegment charPtr =
            whisper_full_get_segment_text(ctx, i); // 此处无需释放返回的const char * ，此指针为栈分配
        String text = charPtr2ToString(charPtr, "");
        logDebugResultWithTimes(ctx, i, text);
        result[i] = text;
      }
      return result;
    }
    return null;
  }

  @Override
  @Nullable
  public String recognizeLanguage(File audioFile)
      throws IOException, UnsupportedAudioFileException, EncoderException {
    float[] audioData =
        convert2FloatArray(convert2WhisperCppWav(validate(audioFile, "Invalid audio file: ")));
    return analyze(
        defaultWhisperContext,
        defaultWhisperFullParams,
        config.getFullParams().getN_processors(), // 计算用核心数
        () -> audioData,
        ctx ->
            // 此处无需释放返回的const char * ，此指针为栈分配
            charPtr2ToString(whisper_lang_str(whisper_full_lang_id(ctx)), null));
  }

  /**
   * 校验文件合法性
   *
   * @param f 文件
   * @param errorMsgPrefix 错误消息前缀
   * @return 文件
   * @throws IOException 异常
   */
  private static File validate(@NonNull File f, String errorMsgPrefix) throws IOException {
    if (!(f.exists() && f.isFile() && f.canRead())) {
      throw new IllegalArgumentException(String.format(errorMsgPrefix + f.getCanonicalPath()));
    }
    return f;
  }

  private static void logDebugResultWithTimes(MemorySegment ctx, int segment, String text) {
    if (log.isDebugEnabled()) {
      long startTime = whisper_full_get_segment_t0(ctx, segment);
      long endTime = whisper_full_get_segment_t1(ctx, segment);
      log.debug(
          "[{} --- {}] text: {}", format2HHmmssSSS(startTime), format2HHmmssSSS(endTime), text);
    }
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
}
