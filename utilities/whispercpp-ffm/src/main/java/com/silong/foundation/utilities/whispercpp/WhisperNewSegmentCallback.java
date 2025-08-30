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

import com.silong.foundation.utilities.whispercpp.generated.whisper_new_segment_callback;
import com.silong.foundation.utilities.whispercpp.generated.whisper_new_segment_callback.Function;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import lombok.SneakyThrows;

/**
 * 对于长音频或实时流（如麦克风输入）场景，模型会将音频切分为多个连续的
 * whisper_segment（片段）逐段处理。每当一个片段的转录完成，whisper_new_segment_callback
 * 会立即被调用，开发者可通过该回调实时拿到当前片段的文本、时间戳等信息，实现 “边听边转边显示” 的效果（如字幕实时渲染、实时语音助手响应）。
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public
record WhisperNewSegmentCallback( // 回调函数实现类，必须实现com.silong.foundation.utilities.whispercpp.generated.whisper_new_segment_callback.Function，并且提供无参构造函数
    String newSegmentCallbackClass,
    // 回调函数用户数据实现类，此实现类必须实现 java.util.function.Supplier<MemorySegment>
    String newSegmentCallbackUserDataClass) {

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
  public MemorySegment newSegmentCallback() {
    if (newSegmentCallbackClass == null || newSegmentCallbackClass.isBlank()) {
      throw new IllegalArgumentException("newSegmentCallbackClass must not be null or blank.");
    }
    Class<?> clazz = Class.forName(newSegmentCallbackClass);
    if (!clazz.isAssignableFrom(Function.class)) {
      throw new IllegalArgumentException(
          "newSegmentCallbackClass must be a class that implements com.silong.foundation.utilities.whispercpp.generated.whisper_new_segment_callback.Function.");
    }
    return whisper_new_segment_callback.allocate(
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
  public MemorySegment newSegmentUserData() {
    if (newSegmentCallbackUserDataClass == null || newSegmentCallbackUserDataClass.isBlank()) {
      return MemorySegment.NULL;
    }
    Class<?> clazz = Class.forName(newSegmentCallbackUserDataClass);
    if (!clazz.isAssignableFrom(Supplier.class)) {
      throw new IllegalArgumentException(
          "newSegmentCallbackUserDataClass must be a class that implements java.util.function.Supplier<MemorySegment>.");
    }
    return ((Supplier<MemorySegment>) clazz.getDeclaredConstructor().newInstance()).get();
  }
}
