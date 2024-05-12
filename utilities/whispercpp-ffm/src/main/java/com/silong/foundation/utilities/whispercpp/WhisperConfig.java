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

import static com.silong.foundation.utilities.whispercpp.Utils.*;
import static com.silong.foundation.utilities.whispercpp.WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.*;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.C_POINTER;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.*;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.abort_callback_user_data;
import static java.lang.Runtime.getRuntime;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.utilities.whispercpp.generated.*;
import com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search;
import com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.greedy;
import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;

/**
 * Whisper语音识别配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-20 17:19
 */
@Data
public class WhisperConfig {

  /** 模型文件路径，绝对路径 */
  @NotEmpty private String modelPath;

  /** 上下文参数 */
  @Valid private WhisperContextParams contextParams;

  /** whisper 全参数 */
  @Valid private WhisperFullParams fullParams;

  /** whisper全参数(部分支持) */
  @Data
  public static class WhisperFullParams implements ForeignParams {

    /** 采样策略 */
    private WhisperSamplingStrategy strategy = WHISPER_SAMPLING_GREEDY;

    /**
     * called for every newly generated text segment. <br>
     * 回调函数类权限定名，必须实现接口: <br>
     * <ref>com.silong.foundation.utilities.whispercpp.generated.whisper_new_segment_callback.Function</ref>
     */
    private String whisperNewSegmentCallbackClassFQDN;

    /** 回调方法携带的用户数据类 */
    private String whisperNewSegmentCallbackUserDataClassFQDN;

    /**
     * called on each progress update. <br>
     * 回调函数类权限定名，必须实现接口: <br>
     * <ref>com.silong.foundation.utilities.whispercpp.generated.whisper_progress_callback.Function</ref>
     */
    private String whisperProgressCallbackClassFQDN;

    /** 回调方法携带的用户数据类 */
    private String whisperProgressCallbackUserDataClassFQDN;

    /**
     * called each time before the encoder starts. <br>
     * 回调函数类权限定名，必须实现接口: <br>
     * <ref>com.silong.foundation.utilities.whispercpp.generated.whisper_encoder_begin_callback.Function</ref>
     */
    private String whisperEncoderBeginCallbackClassFQDN;

    /** 回调方法携带的用户数据类 */
    private String whisperEncoderBeginCallbackUserDataClassFQDN;

    /**
     * called each time before ggml computation starts. <br>
     * 回调函数类权限定名，必须实现接口: <br>
     * <ref>com.silong.foundation.utilities.whispercpp.generated.ggml_abort_callback.Function</ref>
     */
    private String abortCallbackClassFQDN;

    /** 回调方法携带的用户数据类 */
    private String abortCallbackUserDataClassFQDN;

    /**
     * called by each decoder to filter obtained logits. <br>
     * 回调函数类权限定名，必须实现接口: <br>
     * <ref>com.silong.foundation.utilities.whispercpp.generated.whisper_logits_filter_callback.Function</ref>
     */
    private String whisperLogitsFilterCallbackClassFQDN;

    /** 回调方法携带的用户数据类 */
    private String whisperLogitsFilterCallbackUserDataClassFQDN;

    private WhisperGrammarElement[][] grammar_rules;

    private int n_grammar_rules;

    private int i_start_rule;

    private float grammar_penalty;

    /** number of threads to use during computation */
    private int n_threads = Math.min(4, getRuntime().availableProcessors());

    /** number of processors to use during computation */
    private int n_processors = 1;

    /** max tokens to use from past text as prompt for the decoder */
    private int n_max_text_ctx = 16384;

    private int offset_ms = 0; // start offset in ms
    private int duration_ms = 0; // audio duration to process in ms

    private boolean translate = false;
    private boolean no_context =
        true; // do not use past transcription (if any) as initial prompt for the decoder
    private boolean no_timestamps = false; // do not generate timestamps
    private boolean single_segment = false; // force single segment output (useful for streaming)
    private boolean print_special = false; // print special tokens (e.g. <SOT>, <EOT>, <BEG>, etc.)
    private boolean print_progress = true; // print progress information
    private boolean print_realtime =
        false; // print results from within whisper.cpp (avoid it, use callback instead)
    private boolean print_timestamps =
        true; // print timestamps for each text segment when printing realtime

    // [EXPERIMENTAL] token-level timestamps
    private boolean token_timestamps = false; // enable token-level timestamps
    private float thold_pt = 0.01f; // timestamp token probability threshold (~0.01)
    private float thold_ptsum = 0.01f; // timestamp token sum probability threshold (~0.01)
    private int max_len = 0; // max segment length in characters
    private boolean split_on_word =
        false; // split on word rather than on token (when used with max_len)
    private int max_tokens = 0; // max tokens per segment (0 = no limit)

    // [EXPERIMENTAL] speed-up techniques
    // note: these can significantly reduce the quality of the output
    private boolean speed_up = false; // speed-up the audio by 2x using Phase Vocoder
    private boolean debug_mode = false; // enable debug_mode provides extra info (eg. Dump log_mel)
    private int audio_ctx = 0; // overwrite the audio context size (0 = use default)

    // [EXPERIMENTAL] [TDRZ] tinydiarize
    private boolean tdrz_enable = false; // enable tinydiarize speaker turn detection

    private String suppress_regex; // A regular expression that matches tokens to suppress

    // tokens to provide to the whisper decoder as initial prompt
    // these are prepended to any existing text context from a previous call
    // use whisper_tokenize() to convert text to tokens
    // maximum of whisper_n_text_ctx()/2 tokens are used (typically 224)
    private String initial_prompt;
    private int[] prompt_tokens;
    private int prompt_n_tokens = 0;

    /** for auto-detection, set to nullptr, "" or "auto" */
    private String language = "en";

    /** exit after automatically detecting language */
    private boolean detect_language = false;

    /** common decoding parameters: */
    private boolean suppress_blank = true; // ref:

    // https://github.com/openai/whisper/blob/f82bc59f5ea234d4b97fb2860842ed38519f7e65/whisper/decoding.py#L89

    private boolean suppress_non_speech_tokens = false; // ref:
    // https://github.com/openai/whisper/blob/7858aa9c08d98f75575035ecd6481f462d66ca27/whisper/tokenizer.py#L224-L253

    private float temperature =
        0.0f; // initial decoding temperature, ref: https://ai.stackexchange.com/a/32478
    private float max_initial_ts = 1.0f; // ref:
    // https://github.com/openai/whisper/blob/f82bc59f5ea234d4b97fb2860842ed38519f7e65/whisper/decoding.py#L97
    private float length_penalty = -1.0f; // ref:
    // https://github.com/openai/whisper/blob/f82bc59f5ea234d4b97fb2860842ed38519f7e65/whisper/transcribe.py#L267

    // fallback parameters
    // ref:
    // https://github.com/openai/whisper/blob/f82bc59f5ea234d4b97fb2860842ed38519f7e65/whisper/transcribe.py#L274-L278
    private float temperature_inc = 0.2f;
    private float entropy_thold = 2.4f; // similar to OpenAI's "compression_ratio_threshold"
    private float logprob_thold = -1.0f;
    private float no_speech_thold = 0.6f; // TODO: not implemented

    private Greedy greedy = Greedy.DEFAULT_GREEDY; // ref:
    // https://github.com/openai/whisper/blob/f82bc59f5ea234d4b97fb2860842ed38519f7e65/whisper/transcribe.py#L264

    private BeamSearch beam_search = BeamSearch.DEFAULT_BEAM_SEARCH;

    static void printDebugWhisperFullParams(@NonNull MemorySegment params, @NonNull Logger log) {
      MemorySegment newSegmentCallbackUserData = new_segment_callback_user_data(params);
      MemorySegment progressCallbackUserData = progress_callback_user_data(params);
      MemorySegment encoderBeginCallbackUserData = encoder_begin_callback_user_data(params);
      MemorySegment abortCallbackUserData = abort_callback_user_data(params);
      MemorySegment logitsFilterCallbackUserData = logits_filter_callback_user_data(params);
      log.debug(
          "whisper_full_params: [language: {}, detect_language: {}, strategy: {}, n_threads: {}, n_max_text_ctx: {}{}offset_ms: {}, duration_ms: {}, translate: {}, no_context: {}, no_timestamps: {}{}single_segment: {}, print_special: {}, print_progress: {}, print_realtime: {}, print_timestamps: {}{}token_timestamps: {}, thold_pt: {}, thold_ptsum: {}, max_len: {}, split_on_word: {}{}max_tokens: {}, speed_up: {}, debug_mode: {}, audio_ctx: {}, tdrz_enable: {}{}suppress_regex: {}, initial_prompt: {}, prompt_tokens: {}, suppress_blank: {}, suppress_non_speech_tokens: {}{}temperature: {}, max_initial_ts: {}, length_penalty: {}, temperature_inc: {}, entropy_thold :{}{}logprob_thold: {}, no_speech_thold: {}, greedy: {}, beamSearch: {}, new_segment_callback: {}{}new_segment_callback_user_data: {}, progress_callback: {}, progress_callback_user_data: {}, encoder_begin_callback: {}, encoder_begin_callback_user_data: {}{}abort_callback: {}, abort_callback_user_data: {}, logits_filter_callback: {}, logits_filter_callback_user_data:{}]",
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
              : whisper_new_segment_callback.descriptor(),
          System.lineSeparator(),
          NULL.equals(newSegmentCallbackUserData)
              ? "null"
              : "address: " + newSegmentCallbackUserData.address(),
          NULL.equals(progress_callback(params)) ? "null" : whisper_progress_callback.descriptor(),
          NULL.equals(progressCallbackUserData)
              ? "null"
              : "address: " + progressCallbackUserData.address(),
          NULL.equals(encoder_begin_callback(params))
              ? "null"
              : whisper_encoder_begin_callback.descriptor(),
          NULL.equals(encoderBeginCallbackUserData)
              ? "null"
              : "address: " + encoderBeginCallbackUserData.address(),
          System.lineSeparator(),
          NULL.equals(abort_callback(params)) ? "null" : ggml_abort_callback.descriptor(),
          NULL.equals(abortCallbackUserData)
              ? "null"
              : "address: " + abortCallbackUserData.address(),
          NULL.equals(logits_filter_callback(params))
              ? "null"
              : whisper_logits_filter_callback.descriptor(),
          NULL.equals(logitsFilterCallbackUserData)
              ? "null"
              : "address: " + logitsFilterCallbackUserData.address());
    }

    @NonNull
    @Override
    @SneakyThrows(ClassNotFoundException.class)
    public MemorySegment convertTo(SegmentAllocator allocator) {
      MemorySegment ms = whisper_full_default_params(allocator, strategy.getValue()); // 以默认参数为底稿

      WhisperGrammarElement[][] grammarRules = getGrammar_rules();
      if (grammarRules != null && grammarRules.length != 0) {
        MemorySegment arrayPtr = allocator.allocate(C_POINTER, grammarRules.length);
        for (int i = 0; i < grammarRules.length; i++) {
          WhisperGrammarElement[] grammarRule = grammarRules[i];
          if (grammarRule != null && grammarRule.length != 0) {
            MemorySegment array =
                whisper_grammar_element.allocateArray(grammarRule.length, allocator);
            for (int j = 0; j < grammarRule.length; j++) {
              WhisperGrammarElement element = grammarRule[j];
              MemorySegment slice = whisper_grammar_element.asSlice(array, j);
              if (element != null) {
                whisper_grammar_element.type(slice, element.getType().getValue());
                whisper_grammar_element.value(slice, element.getValue());
              }
            }
            arrayPtr.setAtIndex(C_POINTER, i, array);
          }
        }
        grammar_rules(ms, arrayPtr);
        n_grammar_rules(ms, getN_grammar_rules());
        i_start_rule(ms, getI_start_rule());
        grammar_penalty(ms, getGrammar_penalty());
      }

      if (greedy != null) {
        whisper_full_params.greedy(ms, greedy.convertTo(allocator));
      }
      if (beam_search != null) {
        whisper_full_params.beam_search(ms, beam_search.convertTo(allocator));
      }
      if (suppress_regex != null && !suppress_regex.isEmpty()) {
        whisper_full_params.suppress_regex(ms, allocator.allocateFrom(suppress_regex, UTF_8));
      }
      if (prompt_tokens != null) {
        assert prompt_tokens.length == prompt_n_tokens;
        whisper_full_params.prompt_tokens(ms, allocator.allocateFrom(C_INT, prompt_tokens));
      }
      whisper_full_params.prompt_n_tokens(ms, prompt_n_tokens);

      configurePtr(
          getWhisperLogitsFilterCallbackUserDataClassFQDN(),
          ForeignParams.class,
          aClass -> {
            ForeignParams userData = newInstance(aClass);
            MemorySegment result = userData.convertTo(allocator);
            logits_filter_callback_user_data(ms, result); // 注册回调函数
          });

      configurePtr(
          getWhisperLogitsFilterCallbackClassFQDN(),
          whisper_logits_filter_callback.Function.class,
          aClass -> {
            whisper_logits_filter_callback.Function callback = newInstance(aClass);
            MemorySegment result =
                whisper_logits_filter_callback.allocate(callback, (Arena) allocator);
            logits_filter_callback(ms, result); // 注册回调函数
          });

      configurePtr(
          getAbortCallbackClassFQDN(),
          ggml_abort_callback.Function.class,
          aClass -> {
            ggml_abort_callback.Function callback = newInstance(aClass);
            MemorySegment result = ggml_abort_callback.allocate(callback, (Arena) allocator);
            abort_callback(ms, result); // 注册回调函数
          });

      configurePtr(
          getAbortCallbackUserDataClassFQDN(),
          ForeignParams.class,
          aClass -> {
            ForeignParams userData = newInstance(aClass);
            MemorySegment result = userData.convertTo(allocator);
            abort_callback_user_data(ms, result); // 注册回调函数
          });

      configurePtr(
          getWhisperEncoderBeginCallbackClassFQDN(),
          whisper_encoder_begin_callback.Function.class,
          aClass -> {
            whisper_encoder_begin_callback.Function callback = newInstance(aClass);
            MemorySegment result =
                whisper_encoder_begin_callback.allocate(callback, (Arena) allocator);
            encoder_begin_callback(ms, result); // 注册回调函数
          });

      configurePtr(
          getWhisperEncoderBeginCallbackUserDataClassFQDN(),
          ForeignParams.class,
          aClass -> {
            ForeignParams userData = newInstance(aClass);
            MemorySegment result = userData.convertTo(allocator);
            encoder_begin_callback_user_data(ms, result); // 注册回调函数
          });

      configurePtr(
          getWhisperProgressCallbackClassFQDN(),
          whisper_progress_callback.Function.class,
          aClass -> {
            whisper_progress_callback.Function callback = newInstance(aClass);
            MemorySegment result = whisper_progress_callback.allocate(callback, (Arena) allocator);
            progress_callback(ms, result); // 注册回调函数
          });

      configurePtr(
          getWhisperProgressCallbackUserDataClassFQDN(),
          ForeignParams.class,
          aClass -> {
            ForeignParams userData = newInstance(aClass);
            MemorySegment result = userData.convertTo(allocator);
            progress_callback_user_data(ms, result); // 注册回调函数
          });

      configurePtr(
          getWhisperNewSegmentCallbackClassFQDN(),
          whisper_new_segment_callback.Function.class,
          aClass -> {
            whisper_new_segment_callback.Function callback = newInstance(aClass);
            MemorySegment result =
                whisper_new_segment_callback.allocate(callback, (Arena) allocator);
            new_segment_callback(ms, result); // 注册回调函数
          });

      configurePtr(
          getWhisperNewSegmentCallbackUserDataClassFQDN(),
          ForeignParams.class,
          aClass -> {
            ForeignParams userData = newInstance(aClass);
            MemorySegment result = userData.convertTo(allocator);
            new_segment_callback_user_data(ms, result); // 注册回调函数
          });

      whisper_full_params.entropy_thold(ms, entropy_thold);
      whisper_full_params.temperature_inc(ms, temperature_inc);
      whisper_full_params.logprob_thold(ms, logprob_thold);
      whisper_full_params.no_speech_thold(ms, no_speech_thold);
      whisper_full_params.length_penalty(ms, length_penalty);
      whisper_full_params.temperature(ms, temperature);
      whisper_full_params.max_initial_ts(ms, max_initial_ts);
      whisper_full_params.suppress_non_speech_tokens(ms, suppress_non_speech_tokens);
      whisper_full_params.thold_ptsum(ms, thold_ptsum);
      whisper_full_params.suppress_blank(ms, suppress_blank);
      whisper_full_params.thold_pt(ms, thold_pt);
      whisper_full_params.tdrz_enable(ms, tdrz_enable);
      whisper_full_params.detect_language(ms, detect_language);

      if (language != null && !language.isEmpty()) {
        whisper_full_params.language(ms, allocator.allocateFrom(language, UTF_8));
      }

      if (initial_prompt != null && !initial_prompt.isEmpty()) {
        whisper_full_params.initial_prompt(ms, allocator.allocateFrom(initial_prompt, UTF_8));
      }

      whisper_full_params.audio_ctx(ms, audio_ctx);
      whisper_full_params.debug_mode(ms, debug_mode);
      whisper_full_params.speed_up(ms, speed_up);
      whisper_full_params.max_tokens(ms, max_tokens);
      whisper_full_params.split_on_word(ms, split_on_word);
      whisper_full_params.max_len(ms, max_len);
      whisper_full_params.token_timestamps(ms, token_timestamps);
      whisper_full_params.print_timestamps(ms, print_timestamps);
      whisper_full_params.print_realtime(ms, print_realtime);
      whisper_full_params.print_progress(ms, print_progress);
      whisper_full_params.print_special(ms, print_special);
      whisper_full_params.single_segment(ms, single_segment);
      whisper_full_params.no_timestamps(ms, no_timestamps);
      whisper_full_params.no_context(ms, no_context);
      whisper_full_params.translate(ms, translate);
      whisper_full_params.duration_ms(ms, duration_ms);
      whisper_full_params.offset_ms(ms, offset_ms);
      whisper_full_params.n_max_text_ctx(ms, n_max_text_ctx);
      whisper_full_params.n_threads(ms, n_threads);
      whisper_full_params.grammar_penalty(ms, grammar_penalty);
      whisper_full_params.i_start_rule(ms, i_start_rule);
      whisper_full_params.n_grammar_rules(ms, n_grammar_rules);
      return ms;
    }

    private static void configurePtr(
        String classFQDN, Class<?> superClass, Consumer<Class<?>> configurator)
        throws ClassNotFoundException {
      if (classFQDN == null || classFQDN.isEmpty()) {
        return;
      }

      Class<?> aClass = Class.forName(classFQDN);
      if (!superClass.isAssignableFrom(aClass)) {
        throw new IllegalArgumentException(
            String.format(
                "%s does not implement the interface %s.", classFQDN, superClass.getName()));
      }
      configurator.accept(aClass);
    }
  }

  /** whisper上下文参数(部分支持) */
  @Data
  public static class WhisperContextParams implements ForeignParams {
    /** 是否启用GPU */
    private boolean use_gpu;

    /** CUDA device */
    private int gpu_device;

    /** [EXPERIMENTAL] Token-level timestamps with DTW */
    private boolean dtw_token_timestamps;

    private WhisperAlignmentHeadsPreset dtw_aheads_preset;

    private int dtw_n_top;

    private long dtw_mem_size;

    private WhisperAheads dtw_aheads;

    @NonNull
    @Override
    public MemorySegment convertTo(SegmentAllocator allocator) {
      MemorySegment ms = whisper_context_params.allocate(allocator);
      whisper_context_params.dtw_n_top(ms, dtw_n_top);
      whisper_context_params.gpu_device(ms, gpu_device);
      whisper_context_params.dtw_aheads_preset(ms, dtw_aheads_preset.getValue());
      whisper_context_params.dtw_token_timestamps(ms, dtw_token_timestamps);
      if (dtw_aheads != null) {
        whisper_context_params.dtw_aheads(ms, dtw_aheads.convertTo(allocator));
      }
      return ms;
    }
  }

  /**
   * whisper 语法元素
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2024-05-01 11:57
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class WhisperGrammarElement implements ForeignParams {

    private WhisperGreType type;

    /** Unicode code point or rule ID */
    private int value;

    @NonNull
    @Override
    public MemorySegment convertTo(SegmentAllocator allocator) {
      MemorySegment ms = whisper_grammar_element.allocate(allocator);
      whisper_grammar_element.type(ms, type.getValue());
      whisper_grammar_element.value(ms, value);
      return ms;
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Greedy implements ForeignParams {

    private static final Greedy DEFAULT_GREEDY = new Greedy(5);

    /** number of best candidates to keep */
    private int best_of;

    @NonNull
    @Override
    public MemorySegment convertTo(SegmentAllocator allocator) {
      MemorySegment ms = greedy.allocate(allocator);
      greedy.best_of(ms, best_of);
      return ms;
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class BeamSearch implements ForeignParams {
    private static final BeamSearch DEFAULT_BEAM_SEARCH = new BeamSearch(5, -1.0f);

    /** beam size for beam search */
    private int beam_size; // ref:

    // https://github.com/openai/whisper/blob/f82bc59f5ea234d4b97fb2860842ed38519f7e65/whisper/transcribe.py#L265

    private float patience; // TODO: not implemented, ref: https://arxiv.org/pdf/2204.05424.pdf

    @NonNull
    @Override
    public MemorySegment convertTo(SegmentAllocator allocator) {
      MemorySegment ms = beam_search.allocate(allocator);
      beam_search.beam_size(ms, beam_size);
      beam_search.patience(ms, patience);
      return ms;
    }
  }

  @Data
  public static class WhisperAhead implements ForeignParams {
    private int n_text_layer;
    private int n_head;

    @NonNull
    @Override
    public MemorySegment convertTo(SegmentAllocator allocator) {
      MemorySegment ms = whisper_ahead.allocate(allocator);
      whisper_ahead.n_text_layer(ms, n_text_layer);
      whisper_ahead.n_head(ms, n_head);
      return ms;
    }
  }

  @Data
  public static class WhisperAheads implements ForeignParams {

    private long n_heads;

    private WhisperAhead[] heads;

    @NonNull
    @Override
    public MemorySegment convertTo(SegmentAllocator allocator) {
      MemorySegment ms = whisper_aheads.allocate(allocator);
      whisper_aheads.n_heads(ms, n_heads);
      MemorySegment array = whisper_ahead.allocateArray(n_heads, allocator);
      whisper_aheads.heads(ms, array);
      for (int i = 0; i < n_heads; i++) {
        MemorySegment element = whisper_ahead.asSlice(array, i);
        whisper_ahead.n_head(element, heads[i].n_head);
        whisper_ahead.n_text_layer(element, heads[i].n_text_layer);
      }
      return ms;
    }
  }
}
