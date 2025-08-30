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

import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.C_POINTER;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.int32_t;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.utilities.whispercpp.generated.*;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.SystemUtils;

/**
 * 工具方法
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
class Utils {

  private static final Linker LINKER = Linker.nativeLinker();

  private static final MethodHandle FREE =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

  /**
   * 操作系统检测到的分类器
   *
   * @return 分类器字符串，如 linux-x86_64、windows-x86_64、osx-arm64 等
   */
  static String getOSDetectedClassifier() {
    // 操作系统类型
    String osType;
    if (SystemUtils.IS_OS_LINUX) {
      osType = "linux";
    } else if (SystemUtils.IS_OS_WINDOWS) {
      osType = "windows";
    } else if (SystemUtils.IS_OS_MAC) {
      osType = "osx";
    } else {
      throw new IllegalStateException("Unsupported OS: " + SystemUtils.OS_NAME);
    }

    // 系统架构（需手动映射标准化）
    String arch =
        switch (SystemUtils.OS_ARCH.toLowerCase()) {
          case String s when (s.contains("amd64") || s.contains("x86_64")) -> "x86_64";
          case String s when (s.contains("arm64") || s.contains("aarch64")) -> "arm64";
          default ->
              throw new IllegalStateException(
                  "Unsupported OS-ARCH: " + SystemUtils.OS_ARCH.toLowerCase());
        };

    return osType + "-" + arch;
  }

  static String whisperContextParams2String(MemorySegment params) {
    return String.format(
        "whisper_context_params: {use_gpu:%b, flash_attn:%b, gpu_device:%d, dtw_token_timestamps:%b, dtw_aheads_preset:%s, dtw_n_top:%d, dtw_aheads:%s, dtw_mem_size:%d}",
        whisper_context_params.use_gpu(params),
        whisper_context_params.flash_attn(params),
        whisper_context_params.gpu_device(params),
        whisper_context_params.dtw_token_timestamps(params),
        WhisperAlignmentHeadsPreset.fromOriginal(whisper_context_params.dtw_aheads_preset(params)),
        whisper_context_params.dtw_n_top(params),
        whisperAheads2String(whisper_context_params.dtw_aheads(params)),
        whisper_context_params.dtw_mem_size(params));
  }

  static String whisperAheads2String(MemorySegment params) {
    long nHeads = whisper_aheads.n_heads(params);
    MemorySegment heads = whisper_aheads.heads(params);
    return LongStream.range(0, nHeads)
        .mapToObj(
            i -> {
              MemorySegment head = heads.asSlice(i, whisper_ahead.layout());
              return String.format(
                  "{nTextLayer:%d, nHead:%d}",
                  whisper_ahead.n_text_layer(head), whisper_ahead.n_head(head));
            })
        .collect(Collectors.joining(", ", "[", "]"));
  }

  static String whisperFullParams2String(MemorySegment params) {
    try (var arena = Arena.ofConfined()) {
      MemorySegment temp;
      return String.format(
          "whisper_full_params: {n_threads:%d, n_max_text_ctx:%d, offset_ms:%dms, duration_ms:%dms, "
              + System.lineSeparator()
              + "translate:%b, no_context:%b, no_timestamps:%b, single_segment:%b, print_special:%b, print_progress:%b, print_realtime:%b, print_timestamps:%b, "
              + System.lineSeparator()
              + "token_timestamps:%b, thold_pt:%f, thold_ptsum:%f, max_len:%d, split_on_word:%b, max_tokens:%d, "
              + System.lineSeparator()
              + "debug_mode:%b, audio_ctx:%d, "
              + System.lineSeparator()
              + "tdrz_enable:%b, suppress_regex:%s, initial_prompt:%s, prompt_tokens:%s, prompt_n_tokens:%d, "
              + System.lineSeparator()
              + "language:%s, detect_language:%b, "
              + System.lineSeparator()
              + "suppress_blank:%b, suppress_nst:%b, temperature:%f, max_initial_ts:%f, length_penalty:%f, "
              + System.lineSeparator()
              + "temperature_inc:%f, entropy_thold:%f, logprob_thold:%f, no_speech_thold:%f, strategy:%s, "
              + System.lineSeparator()
              + "grammar_rules:%s, n_grammar_rules:%d, i_start_rule:%d, grammar_penalty:%f, "
              + System.lineSeparator()
              + "vad:%b, vad_model_path:%s, vad_params_threshold:%f, vad_params_min_speech_duration_ms:%d, vad_params_min_silence_duration_ms:%d, vad_params_max_speech_duration_s:%f, vad_params_speech_pad_ms:%d, vad_params_samples_overlap:%f}",
          whisper_full_params.n_threads(params),
          whisper_full_params.n_max_text_ctx(params),
          whisper_full_params.offset_ms(params),
          whisper_full_params.duration_ms(params),
          whisper_full_params.translate(params),
          whisper_full_params.no_context(params),
          whisper_full_params.no_timestamps(params),
          whisper_full_params.single_segment(params),
          whisper_full_params.print_special(params),
          whisper_full_params.print_progress(params),
          whisper_full_params.print_realtime(params),
          whisper_full_params.print_timestamps(params),
          whisper_full_params.token_timestamps(params),
          whisper_full_params.thold_pt(params),
          whisper_full_params.thold_ptsum(params),
          whisper_full_params.max_len(params),
          whisper_full_params.split_on_word(params),
          whisper_full_params.max_tokens(params),
          whisper_full_params.debug_mode(params),
          whisper_full_params.audio_ctx(params),
          whisper_full_params.tdrz_enable(params),
          getString(arena, whisper_full_params.suppress_regex(params)),
          getString(arena, whisper_full_params.initial_prompt(params)),
          intArray2String(
              arena,
              whisper_full_params.prompt_tokens(params),
              whisper_full_params.prompt_n_tokens(params)),
          whisper_full_params.prompt_n_tokens(params),
          getString(arena, whisper_full_params.language(params)),
          whisper_full_params.detect_language(params),
          whisper_full_params.suppress_blank(params),
          whisper_full_params.suppress_nst(params),
          whisper_full_params.temperature(params),
          whisper_full_params.max_initial_ts(params),
          whisper_full_params.length_penalty(params),
          whisper_full_params.temperature_inc(params),
          whisper_full_params.entropy_thold(params),
          whisper_full_params.logprob_thold(params),
          whisper_full_params.no_speech_thold(params),
          WhisperSamplingStrategy.fromOriginal(whisper_full_params.strategy(params)),
          whisperGrammarArray2String(
              arena,
              whisper_full_params.grammar_rules(params),
              whisper_full_params.n_grammar_rules(params)),
          whisper_full_params.n_grammar_rules(params),
          whisper_full_params.i_start_rule(params),
          whisper_full_params.grammar_penalty(params),
          whisper_full_params.vad(params),
          getString(arena, whisper_full_params.vad_model_path(params)),
          whisper_vad_params.threshold(temp = whisper_full_params.vad_params(params)),
          whisper_vad_params.min_speech_duration_ms(temp),
          whisper_vad_params.min_silence_duration_ms(temp),
          whisper_vad_params.max_speech_duration_s(temp),
          whisper_vad_params.speech_pad_ms(temp),
          whisper_vad_params.samples_overlap(temp));
    }
  }

  private static String intArray2String(Arena arena, MemorySegment segment, int length) {
    return (segment == null || MemorySegment.NULL.equals(segment)
        ? null
        : intArray2String(segment.reinterpret(arena, Utils::free), length));
  }

  private static String intArray2String(MemorySegment segment, int length) {
    return segment
        .elements(int32_t)
        .map(String::valueOf)
        .collect(Collectors.joining(", ", "[", "]"));
  }

  private static String getString(Arena arena, MemorySegment segment) {
    return (segment == null || MemorySegment.NULL.equals(segment)
        ? null
        : segment.reinterpret(arena, Utils::free).getString(0, UTF_8));
  }

  private static String whisperGrammarArray2String(
      Arena arena, MemorySegment segment, long length) {
    if (segment == null || MemorySegment.NULL.equals(segment)) {
      return null;
    }

    var arrayPtr =
        segment.reinterpret(
            arena,
            ms -> {
              LongStream.range(0, length).forEach(index -> free(ms.getAtIndex(C_POINTER, index)));
              free(segment);
            });

    arrayPtr.elements(C_POINTER).map(ptr -> String.format("[type:%s,value:%s]"));
    // TODO

    return null;
  }

  /**
   * 释放内存空间(malloc分配)
   *
   * @param ptr 指针
   */
  @SneakyThrows
  static void free(MemorySegment ptr) {
    FREE.invokeExact(ptr);
  }
}
