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

import static com.silong.foundation.utilities.whispercpp.ParamsValidator.validateModelPath;
import static com.silong.foundation.utilities.whispercpp.ParamsValidator.validateSupportedLanguage;
import static com.silong.foundation.utilities.whispercpp.WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp.whisper_context_default_params;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp.whisper_full_default_params;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.C_POINTER;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.int32_t;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.utilities.whispercpp.generated.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Whisper配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
@Data
public class WhisperConfig {

  /** whisper_context上下文缓存配置 */
  @Valid private WhisperContextPoolConfig poolConfig = new WhisperContextPoolConfig();

  /** 采样策略，默认：WHISPER_SAMPLING_GREEDY */
  @NotNull private WhisperSamplingStrategy samplingStrategy = WHISPER_SAMPLING_GREEDY;

  /** 使用的线程数，至少为4 */
  @Min(4)
  private int nThreads = Math.min(4, Runtime.getRuntime().availableProcessors());

  /** 上下文窗口大小，单位为token，默认为16K */
  @Positive private int nMaxTextCtx = 16 * 1024;

  /** 音频处理的起始偏移时间，单位为毫秒，默认为0，表示从音频开头开始处理 */
  private int offsetMs;

  /** 音频处理的持续时间，单位为毫秒，默认为0 */
  private int durationMs;

  /** 是否将非英语语音翻译为英语文本，默认为false */
  private boolean translate;

  /** 是否不使用上下文信息，默认为true，即不使用上下文信息，提升处理速度和节省内存 如果需要更高的识别准确率，可以设置为false，启用上下文信息，但会增加处理时间和内存占用 */
  private boolean noContext = true;

  /** 是否不输出时间戳，默认为false，即输出时间戳 如果不需要时间戳信息，可以设置为true，节省内存和提升处理速度 */
  private boolean noTimestamps;

  /**
   * 是否将音频作为单一连续片段处理，默认为false，即按默认的语音活动检测（VAD）进行分割和处理
   * 如果设置为true，则忽略VAD，直接将整个音频作为一个片段进行处理，适用于单人连续讲话的场景 该选项可能会增加处理时间和内存占用，尤其是对于长音频文件
   */
  private boolean singleSegment;

  /** 是否打印特殊字符，如换行符、标点符号等，默认为false，即不打印特殊字符 如果需要保留文本的原始格式和标点，可以设置为true */
  private boolean printSpecial;

  /** 是否打印识别进度，默认为true，即打印识别进度 如果不需要看到识别进度信息，可以设置为false，减少控制台输出 */
  private boolean printProgress = true;

  /** 是否打印实时识别结果，默认为false，即不打印实时结果 如果需要看到实时的识别结果，可以设置为true，适用于实时转录场景 该选项可能会增加控制台输出量，影响性能 */
  private boolean printRealtime;

  /** 是否打印时间戳，默认为true，即打印时间戳 如果不需要时间戳信息，可以设置为false，减少控制台输出 */
  private boolean printTimestamps = true;

  // [EXPERIMENTAL] token-level timestamps
  /** 是否开启token时间戳，开启后每个token会带有时间戳信息，默认为false 开启后会增加内存占用和处理时间，适用于需要精确时间对齐的场景 */
  private boolean tokenTimestamps;

  /** 仅在tokenTimestamps=true时有效，表示时间戳token的概率阈值，默认为0.01 */
  private float tholdPt = 0.01f;

  /**
   * 仅在tokenTimestamps=true时有效，表示时间戳token的累计概率阈值，默认为0.01
   * 该阈值用于控制时间戳token的输出频率，较低的值会增加时间戳的密度，较高的值会减少时间戳的密度
   */
  private float tholdPtsum = 0.01f;

  /** 仅在tokenTimestamps=true时有效，表示每个片段的最大字符长度，默认为0，表示不限制 该参数用于控制输出文本的片段长度，较小的值会增加片段数量，较大的值会减少片段数量 */
  private int maxLen;

  /** 默认值为false，即按token分割 如果设置为true，则按单词分割，适用于需要按单词完整输出的场景 该选项可能会增加处理时间和内存占用，尤其是在长文本的情况下 */
  private boolean splitOnWord;

  /** 默认值为0，即不限制每个片段的最大token数量 如果设置为大于0的值，则每个片段的token数量不会超过该值，适用于需要控制内存占用和处理时间的场景 */
  private int maxTokens;

  // [EXPERIMENTAL] speed-up techniques
  // note: these can significantly reduce the quality of the output
  /** 是否开启调试模式，开启后会打印更多日志信息，默认为false */
  private boolean debugMode;

  /** 默认值为0，即使用模型的默认音频上下文大小 如果需要更大的上下文窗口，可以设置为大于0的值，但会增加内存占用和处理时间 该参数适用于需要处理长音频或需要更高识别准确率的场景 */
  private int audioCtx;

  // [EXPERIMENTAL] [TDRZ] tinydiarize
  /** 默认值为false，即不开启 开启后会增加内存占用和处理时间，适用于需要进行说话人分割的场景 该功能仍在实验阶段，可能不稳定或效果不佳，请谨慎使用 */
  private boolean tdrzEnable;

  /** 默认值为null，即不使用该功能 如果设置了该参数，则在解码过程中会抑制与该正则表达式匹配的token，适用于需要过滤特定词汇或符号的场景 该功能可能会影响识别结果的完整性和准确性 */
  private String suppressRegex;

  /**
   * 初始提示文本，默认为空字符串 该文本会被转换为token，并在解码时作为上下文的一部分提供给模型 适用于需要引导模型生成特定风格或内容的文本的场景
   * 注意：该文本的token数量不能超过上下文窗口大小的一半，以避免占用过多的上下文资源
   */
  private String initialPrompt;

  /**
   * 指向 whisper_token 数组的指针，数组中存储的是预编码的提示令牌（通过 whisper_tokenize() 函数将文本转换而来）。 与 initial_prompt
   * 功能相同（提供初始提示），但跳过了文本→令牌的编码步骤，直接使用预编码的令牌，提升效率。
   */
  private int[] promptTokens;

  /**
   * 识别语种，默认为"auto"，表示自动检测语种，其他值请参考whisper.cpp支持的语种列表，如"zh","en","es"等
   * 如果指定了具体语种，则不会进行自动检测，提升识别速度和准确率
   */
  private String language;

  /**
   * 是否启用语言检测功能，默认为false，即不启用语言检测 如果启用，则会在识别前先检测音频的语言，并将检测结果作为识别的语言参数 该功能适用于多语种音频的场景，但会增加处理时间和内存占用
   */
  private boolean detectLanguage;

  // common decoding parameters:
  /**
   * 是否抑制空白token的输出，默认为true，即抑制空白token的输出 如果设置为false，则不会抑制空白token的输出，可能会导致输出中出现较多的空白字符
   * 该参数适用于需要保留原始音频节奏和停顿信息的场景
   */
  private boolean suppressBlank = true;

  /**
   * 是否抑制非语音token的输出，默认为false，即不抑制非语音token的输出 如果设置为true，则会抑制非语音token的输出，减少无关内容的干扰 该参数适用于需要专注于语音内容的场景
   */
  private boolean suppressNst = false;

  /**
   * 初始解码温度（initial decoding temperature） 是控制模型输出随机性的核心参数，直接影响识别结果的多样性和确定性。 温度 =
   * 1.0：完全遵循模型预测的原始概率分布（最 “自然” 的输出）。 温度 → 0：模型会贪婪地选择概率最高的候选（输出最确定、最保守，但可能缺乏灵活性）。 温度 >
   * 1.0：增加输出的随机性，模型更倾向于选择概率较低的候选（输出更具创造性，但可能不连贯）。 典型值范围：0.1 -
   * 1.0。较低的值适用于需要高准确率和连贯性的场景（如正式文本转录），而较高的值适用于需要多样性和创造性的场景（如对话生成）。 选择合适的温度值需要根据具体应用场景进行权衡和实验。
   */
  private float temperature = 0.0f;

  /** max_initial_ts 是一个用于约束第一个时间戳（timestamp）最大值的参数，主要作用是确保模型生成的初始时间戳在合理的音频范围内，避免时间戳预测错乱。 */
  private float maxInitialTs = 1.0f;

  /**
   * length_penalty本质是对 “文本片段长度” 的惩罚系数，用于平衡模型生成文本的 “完整性” 和 “简洁性”。
   * 较高的length_penalty值会惩罚过长的文本片段，鼓励模型生成更简洁的输出，从而减少冗余和重复内容。
   * 反之，较低的length_penalty值则允许生成更长的文本片段，适用于需要更详细描述的场景。
   * 通过调整length_penalty，可以控制生成文本的长度和信息密度，提升整体的文本质量和可读性。 该参数在实际应用中需要根据具体需求进行调优，以达到最佳的文本生成效果。
   */
  private float lengthPenalty = -1.0f;

  /**
   * temperature_inc（温度递增）是用于动态调整解码温度的参数，其核心作用是在多次解码尝试中逐步提高温度，平衡识别结果的 “确定性” 和 “多样性”，最终找到更优的语音识别结果。
   */
  private float temperatureInc = 0.2f;

  /**
   * entropyThold 是通过 “熵值” 衡量模型预测不确定性的阈值参数，核心作用是：
   * 筛选高可靠性的识别结果，控制解码过程的终止时机，在精度（减少错误）和效率（避免冗余计算）之间找到平衡。其具体取值需根据应用场景（如实时性要求、音频质量）调整
   */
  private float entropyThold = 2.4f;

  /**
   * logprobThold 是通过 “对数概率”
   * 筛选可靠预测结果的阈值参数，核心作用是：剔除模型低置信度的输出，提升识别结果的精确性。其取值需根据场景调整（如严格的字幕生成用较高阈值，容错性强的语音转写用较低阈值），是平衡识别质量与完整性的重要控制手段。
   */
  private float logprobThold = -1.0f;

  /**
   * no_speech_thold 是控制 “语音 / 非语音” 判定的阈值参数，核心作用是过滤无有效语音的音频片段，通过调整阈值可平衡识别的 “灵敏度”（不漏检语音）和
   * “抗噪音能力”（不误检噪音），是提升语音识别效率和质量的重要配置。
   */
  private float noSpeechThold = 0.6f;

  /**
   * 对于长音频或实时流（如麦克风输入）场景，模型会将音频切分为多个连续的
   * whisper_segment（片段）逐段处理。每当一个片段的转录完成，whisper_new_segment_callback *
   * 会立即被调用，开发者可通过该回调实时拿到当前片段的文本、时间戳等信息，实现 “边听边转边显示” 的效果（如字幕实时渲染、实时语音助手响应）。
   */
  private WhisperNewSegmentCallback whisperNewSegmentCallback;

  /**
   * 实时反馈处理进度<br>
   * 当使用 whisper_full 或 whisper_full_with_state 等函数处理音频时，模型会按固定间隔（通常是处理完一定比例的音频数据后）触发
   * whisper_progress_callback，并传递当前的进度信息。开发者可利用这些信息实现：<br>
   * 1、进度条 UI 展示（如命令行进度条、图形界面进度条）； <br>
   * 2、日志输出（如 “已完成 30%”）；<br>
   * 3、超时控制（若长时间未更新进度，可主动中断处理）。 <br>
   * 支持中断处理流程<br>
   * 1、回调函数的返回值为 int 类型，开发者可通过返回非零值（如 1）主动中断当前的转录过程（例如用户手动点击 “取消” 按钮时），返回 0 则表示继续处理。<br>
   */
  private WhisperProgressCallback whisperProgressCallback;

  /**
   * whisper_encoder_begin_callback
   * 是一个与模型编码阶段相关的回调函数，主要用于在编码器（Encoder）开始处理音频数据前触发特定逻辑，为开发者提供了在编码阶段启动时介入处理流程的机会。<br>
   * whisper.cpp 的语音转文字（ASR）过程主要分为两大阶段：<br>
   * 1、编码器（Encoder）阶段：对输入的音频数据进行特征提取和编码，将原始音频转换为模型可理解的特征表示。<br>
   * 2、解码器（Decoder）阶段：基于编码器输出的特征，生成对应的文本转录结果。<br>
   * whisper_encoder_begin_callback 专门在编码器开始工作前被调用，其核心价值在于：<br>
   * 1、允许开发者在编码启动前执行初始化操作（如日志记录、资源预热、参数校验等）。<br>
   * 2、支持在编码开始前决定是否中断整个处理流程（通过返回值控制）。<br>
   */
  private WhisperEncoderBeginCallback whisperEncoderBeginCallback;

  /**
   * ggml_abort_callback 是 ggml 库（whisper.cpp 的底层张量计算引擎） 提供的中断回调函数，核心作用是在 ggml
   * 执行耗时计算（如模型推理中的张量运算）时，允许外部通过 “主动检查中断信号” 的方式终止计算，避免无响应的阻塞，是实现 “可取消计算” 的关键接口。
   */
  private GgmlAbortCallback ggmlAbortCallback;

  /**
   * whisper_logits_filter_callback 是一个用于自定义调整模型输出 “logits”（未归一化的概率分布） 的回调函数，作用是在解码器生成文本的关键阶段介入，通过修改
   * logits 来影响后续 token（词元）的选择，从而实现对转录结果的精细化控制（如过滤敏感词、强制输出特定格式、优化领域特定术语等）。
   *
   * <p>whisper 模型的文本生成（解码）过程是逐 token 进行的：<br>
   * 1、解码器每一步会输出当前位置的 logits—— 一个向量，每个元素对应一个可能 token 的 “未归一化得分”（得分越高，模型认为该 token 越可能出现）。<br>
   * 2、模型通过 softmax * 函数将 logits 转换为概率分布，再基于此选择下一个 token（如贪婪搜索选概率最高的 token）。<br>
   *
   * <p>whisper_logits_filter_callback 正是在 logits 生成后、概率转换前 被调用，允许开发者直接修改 logits 数值，从而改变后续 token
   * 的选择概率。
   */
  private WhisperLogitsFilterCallback whisperLogitsFilterCallback;

  /**
   * grammar_rules 是语音识别中 “规则约束层” 的核心，其核心价值是将模型的 “自由生成” 限制在业务需求范围内，通过限定内容、格式、词汇，解决
   * “识别结果虽正确但不符合实际使用需求” 的问题，尤其适合语音命令、结构化数据输入、专业领域识别等场景，是平衡 “识别精度” 与 “业务适配性” 的关键配置。
   */
  @Valid private WhisperGrammarElement[][] grammarRules;

  /**
   * n_grammar_rules 是指在语音识别解码过程中所使用的语法规则数量，核心作用是通过预定义的语法结构引导模型生成符合语言规范的文本，从而提升识别结果的准确性和一致性。 通过设置
   * n_grammar_rules，可以控制解码过程中应用的语法规则
   */
  private long nGrammarRules;

  /**
   * i_start_rule 是指定解码时应用的初始语法规则索引，核心作用是引导模型在特定的语法框架下进行文本生成，从而提升识别结果的语法正确性和一致性。 通过设置
   * i_start_rule，可以确保生成的文本符合预定义的语法结构，适用于需要严格语法控制的应用场景，如法律文本、技术文档等。
   * 该参数在实际应用中需要根据具体需求进行调优，以达到最佳的文本生成效果。
   */
  private long iStartRule;

  /**
   * grammar_penalty 是在语音识别解码过程中应用的惩罚系数，核心作用是通过对不符合预定义语法规则的输出进行惩罚，提升识别结果的语法正确性和一致性，从而生成更符合语言规范的文本。
   * 该参数在实际应用中需要根据具体需求进行调优，以达到最佳的文本生成效果。
   */
  private float grammarPenalty = 100.0f;

  /** VAD配置 */
  @Valid private VadConfig vad = new VadConfig();

  /** 模型文件路径，支持相对路径和绝对路径，必须是ggml模型文件，如ggml-small.bin等 */
  @NotBlank private String modelPath;

  /** whisper上下文配置 */
  @Valid private WhisperContextConfig context = new WhisperContextConfig();

  /**
   * 构建whisper_context_params结构体
   *
   * @return whisper_context_params结构体
   */
  public MemorySegment buildWhisperContextParams() {
    var arena = Arena.global();
    var whisperContextParams = whisper_context_default_params(arena);
    whisper_context_params.use_gpu(whisperContextParams, context.isUseGpu());
    whisper_context_params.flash_attn(whisperContextParams, context.isFlashAttn());
    whisper_context_params.gpu_device(whisperContextParams, context.getGpuDevice());
    whisper_context_params.dtw_token_timestamps(
        whisperContextParams, context.isDtwTokenTimestamps());
    whisper_context_params.dtw_aheads_preset(
        whisperContextParams, context.getDtwAHeadsPreset().ordinal());
    whisper_context_params.dtw_n_top(whisperContextParams, context.getDtwNTop());

    WhisperAHeads whisperAheads = context.getDtwAHeads();
    if (whisperAheads != null) {
      MemorySegment headers = whisper_aheads.allocate(arena);
      whisper_aheads.n_heads(headers, whisperAheads.nHeads());
      WhisperAHead[] heads = whisperAheads.heads();
      var array = whisper_ahead.allocateArray(heads.length, arena);
      for (int i = 0; i < heads.length; i++) {
        MemorySegment head = whisper_ahead.asSlice(array, i);
        whisper_ahead.n_text_layer(head, heads[i].nTextLayer());
        whisper_ahead.n_head(head, heads[i].nHead());
      }
      whisper_aheads.heads(headers, array);
      whisper_context_params.dtw_aheads(headers);
    }
    whisper_context_params.dtw_mem_size(whisperContextParams, context.getDtwMemSize());
    return whisperContextParams;
  }

  /**
   * 根据配置获取whisper_full_params结构体
   *
   * @return whisper_full_params结构体
   */
  public MemorySegment buildWhisperFullParams() {
    var arena = Arena.global();
    var whisperFullParams =
        whisper_full_default_params(arena, getSamplingStrategy().ordinal()); // 创建默认参数结构体

    whisper_full_params.n_threads(whisperFullParams, getNThreads());
    whisper_full_params.n_max_text_ctx(whisperFullParams, getNMaxTextCtx());
    whisper_full_params.offset_ms(whisperFullParams, getOffsetMs());
    whisper_full_params.duration_ms(whisperFullParams, getDurationMs());

    whisper_full_params.translate(whisperFullParams, isTranslate());
    whisper_full_params.no_context(whisperFullParams, isNoContext());
    whisper_full_params.no_timestamps(whisperFullParams, isNoTimestamps());
    whisper_full_params.single_segment(whisperFullParams, isSingleSegment());
    whisper_full_params.print_special(whisperFullParams, isPrintSpecial());
    whisper_full_params.print_progress(whisperFullParams, isPrintProgress());
    whisper_full_params.print_realtime(whisperFullParams, isPrintRealtime());
    whisper_full_params.print_timestamps(whisperFullParams, isPrintTimestamps());

    // [EXPERIMENTAL] token-level timestamps
    whisper_full_params.token_timestamps(whisperFullParams, isTokenTimestamps());
    whisper_full_params.thold_pt(whisperFullParams, getTholdPt());
    whisper_full_params.thold_ptsum(whisperFullParams, getTholdPtsum());
    whisper_full_params.max_len(whisperFullParams, getMaxLen());
    whisper_full_params.split_on_word(whisperFullParams, isSplitOnWord());
    whisper_full_params.max_tokens(whisperFullParams, getMaxTokens());

    // [EXPERIMENTAL] speed-up techniques
    // note: these can significantly reduce the quality of the output
    whisper_full_params.debug_mode(whisperFullParams, isDebugMode());
    whisper_full_params.audio_ctx(whisperFullParams, getAudioCtx());

    // [EXPERIMENTAL] [TDRZ] tinydiarize
    whisper_full_params.tdrz_enable(whisperFullParams, isTdrzEnable());
    whisper_full_params.suppress_regex(whisperFullParams, getString(arena, getSuppressRegex()));
    whisper_full_params.initial_prompt(whisperFullParams, getString(arena, getInitialPrompt()));
    int[] pts = getPromptTokens();
    int ptsLength = pts == null ? 0 : pts.length;
    whisper_full_params.prompt_tokens(
        whisperFullParams, ptsLength == 0 ? NULL : arena.allocateFrom(int32_t, pts));
    whisper_full_params.prompt_n_tokens(whisperFullParams, ptsLength);

    // 语言设置
    whisper_full_params.language(
        whisperFullParams, validateSupportedLanguage(arena, getLanguage()));
    whisper_full_params.detect_language(whisperFullParams, isDetectLanguage());

    // common decoding parameters:
    whisper_full_params.suppress_blank(whisperFullParams, isSuppressBlank());
    whisper_full_params.suppress_nst(whisperFullParams, isSuppressNst());

    whisper_full_params.temperature(whisperFullParams, getTemperature());
    whisper_full_params.max_initial_ts(whisperFullParams, getMaxInitialTs());
    whisper_full_params.length_penalty(whisperFullParams, getLengthPenalty());

    whisper_full_params.temperature_inc(whisperFullParams, getTemperatureInc());
    whisper_full_params.entropy_thold(whisperFullParams, getEntropyThold());
    whisper_full_params.logprob_thold(whisperFullParams, getLogprobThold());
    whisper_full_params.no_speech_thold(whisperFullParams, getNoSpeechThold());

    // 配置new_segment_callback回调函数
    if (whisperNewSegmentCallback != null) {
      whisper_full_params.new_segment_callback(
          whisperFullParams, whisperNewSegmentCallback.newSegmentCallback());
      whisper_full_params.new_segment_callback_user_data(
          whisperFullParams, whisperNewSegmentCallback.newSegmentUserData());
    }

    // 配置progress_callback回调函数
    if (whisperProgressCallback != null) {
      whisper_full_params.progress_callback(
          whisperFullParams, whisperProgressCallback.progressCallback());
      whisper_full_params.progress_callback_user_data(
          whisperFullParams, whisperProgressCallback.progressCallbackUserData());
    }

    // 配置encoder_begin_callback回调函数
    if (whisperEncoderBeginCallback != null) {
      whisper_full_params.encoder_begin_callback(
          whisperFullParams, whisperEncoderBeginCallback.encoderBeginCallback());
      whisper_full_params.encoder_begin_callback_user_data(
          whisperFullParams, whisperEncoderBeginCallback.encoderBeginCallbackUserData());
    }

    // 配置ggml_abort_callback回调函数
    if (ggmlAbortCallback != null) {
      whisper_full_params.abort_callback(whisperFullParams, ggmlAbortCallback.abortCallback());
      whisper_full_params.abort_callback_user_data(
          whisperFullParams, ggmlAbortCallback.abortCallbackUserData());
    }

    // 配置whisper_logits_filter_callback回调函数
    if (whisperLogitsFilterCallback != null) {
      whisper_full_params.logits_filter_callback(
          whisperFullParams, whisperLogitsFilterCallback.logitsFilterCallback());
      whisper_full_params.logits_filter_callback_user_data(
          whisperFullParams, whisperLogitsFilterCallback.logitsFilterCallbackUserData());
    }

    // 配置语法规则
    var grammarRules = getGrammarRules();
    if (grammarRules != null) {
      MemorySegment pp = arena.allocate(C_POINTER, grammarRules.length);
      int j = 0;
      for (WhisperGrammarElement[] row : grammarRules) {
        MemorySegment array = whisper_grammar_element.allocateArray(row.length, arena);
        for (int i = 0; i < row.length; i++) {
          MemorySegment wge = whisper_grammar_element.asSlice(array, i);
          whisper_grammar_element.type(wge, row[i].type().ordinal());
          whisper_grammar_element.value(wge, row[i].value());
        }
        pp.setAtIndex(C_POINTER, j++, array);
      }
    }
    whisper_full_params.i_start_rule(whisperFullParams, getIStartRule());
    whisper_full_params.n_grammar_rules(whisperFullParams, getNGrammarRules());
    whisper_full_params.grammar_penalty(whisperFullParams, getGrammarPenalty());

    // VAD
    VadConfig vadConfig = getVad();
    var vadParams = whisper_full_params.vad_params(whisperFullParams);
    whisper_vad_params.max_speech_duration_s(vadParams, vadConfig.getMaxSpeechDurationS());
    whisper_vad_params.min_silence_duration_ms(vadParams, vadConfig.getMinSilenceDurationMs());
    whisper_vad_params.min_speech_duration_ms(vadParams, vadConfig.getMinSpeechDurationMs());
    whisper_vad_params.samples_overlap(vadParams, vadConfig.getSamplesOverlap());
    whisper_vad_params.speech_pad_ms(vadParams, vadConfig.getSpeechPadMs());
    whisper_vad_params.threshold(vadParams, vadConfig.getThreshold());
    whisper_full_params.vad_model_path(
        whisperFullParams,
        vadConfig.getModelPath() == null || StringUtils.isBlank(vadConfig.getModelPath())
            ? NULL
            : arena.allocateFrom(validateModelPath(vadConfig.getModelPath()), UTF_8));
    whisper_full_params.vad(whisperFullParams, vadConfig.isEnable());
    return whisperFullParams;
  }

  private MemorySegment getString(Arena arena, String val) {
    return val == null || StringUtils.isBlank(val) ? NULL : arena.allocateFrom(val, UTF_8);
  }
}
