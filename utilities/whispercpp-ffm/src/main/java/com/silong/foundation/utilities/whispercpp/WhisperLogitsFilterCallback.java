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

import com.silong.foundation.utilities.whispercpp.generated.whisper_logits_filter_callback;
import com.silong.foundation.utilities.whispercpp.generated.whisper_logits_filter_callback.Function;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import lombok.SneakyThrows;

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
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public record WhisperLogitsFilterCallback(
    String logitsFilterCallbackClass, String logitsFilterCallbackUserDataClass) {

  /**
   * 创建新的回调函数指针
   *
   * @return 回调函数指针
   */
  @SneakyThrows({
    ClassNotFoundException.class,
    NoSuchMethodException.class,
    InstantiationException.class,
    IllegalAccessException.class,
    IllegalArgumentException.class,
    InvocationTargetException.class
  })
  public MemorySegment logitsFilterCallback() {
    if (logitsFilterCallbackClass == null || logitsFilterCallbackClass.isBlank()) {
      throw new IllegalArgumentException("logitsFilterCallbackClass must not be null or blank.");
    }
    Class<?> clazz = Class.forName(logitsFilterCallbackClass);
    if (!clazz.isAssignableFrom(Function.class)) {
      throw new IllegalArgumentException(
          "logitsFilterCallbackClass must be a class that implements com.silong.foundation.utilities.whispercpp.generated.whisper_logits_filter_callback.Function.");
    }
    return whisper_logits_filter_callback.allocate(
        (Function) clazz.getDeclaredConstructor().newInstance(), Arena.global());
  }

  /**
   * 创建新的回调函数用户数据指针
   *
   * @return 回调函数用户数据指针
   */
  @SneakyThrows({
    ClassNotFoundException.class,
    NoSuchMethodException.class,
    InstantiationException.class,
    IllegalAccessException.class,
    IllegalArgumentException.class,
    InvocationTargetException.class
  })
  @SuppressWarnings("unchecked")
  public MemorySegment logitsFilterCallbackUserData() {
    if (logitsFilterCallbackUserDataClass == null || logitsFilterCallbackUserDataClass.isBlank()) {
      return MemorySegment.NULL;
    }
    Class<?> clazz = Class.forName(logitsFilterCallbackUserDataClass);
    if (!clazz.isAssignableFrom(Supplier.class)) {
      throw new IllegalArgumentException(
          "logitsFilterCallbackUserDataClass must be a class that implements java.util.function.Supplier<MemorySegment>.");
    }
    return ((Supplier<MemorySegment>) clazz.getDeclaredConstructor().newInstance()).get();
  }
}
