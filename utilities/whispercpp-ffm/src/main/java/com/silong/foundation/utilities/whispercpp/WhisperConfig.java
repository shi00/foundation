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

import static com.silong.foundation.utilities.whispercpp.WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY;
import static java.lang.Runtime.getRuntime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private String modelPath;

  /** 上下文参数 */
  private WhisperContextParams contextParams;

  /** whisper 全参数 */
  private WhisperFullParams fullParams;

  /** whisper全参数(部分支持) */
  @Data
  public static class WhisperFullParams {

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
  }

  /** whisper上下文参数(部分支持) */
  @Data
  public static class WhisperContextParams {
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
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Greedy {
    private static final Greedy DEFAULT_GREEDY = new Greedy(5);

    /** number of best candidates to keep */
    private int best_of;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class BeamSearch {
    private static final BeamSearch DEFAULT_BEAM_SEARCH = new BeamSearch(5, -1.0f);

    /** beam size for beam search */
    private int beam_size; // ref:

    // https://github.com/openai/whisper/blob/f82bc59f5ea234d4b97fb2860842ed38519f7e65/whisper/transcribe.py#L265

    private float patience; // TODO: not implemented, ref: https://arxiv.org/pdf/2204.05424.pdf
  }

  @Data
  public static class WhisperAhead {
    private int n_text_layer;
    private int n_head;
  }

  @Data
  public static class WhisperAheads {
    private long n_heads;
    private WhisperAhead[] heads;
  }
}
