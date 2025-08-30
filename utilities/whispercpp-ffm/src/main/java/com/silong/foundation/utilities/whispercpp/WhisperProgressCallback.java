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

import com.silong.foundation.utilities.whispercpp.generated.whisper_progress_callback;
import com.silong.foundation.utilities.whispercpp.generated.whisper_progress_callback.Function;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import lombok.SneakyThrows;

/**
 * 实时反馈处理进度<br>
 * 当使用 whisper_full 或 whisper_full_with_state 等函数处理音频时，模型会按固定间隔（通常是处理完一定比例的音频数据后）触发
 * whisper_progress_callback，并传递当前的进度信息。开发者可利用这些信息实现：<br>
 * 1、进度条 UI 展示（如命令行进度条、图形界面进度条）； <br>
 * 2、日志输出（如 “已完成 30%”）；<br>
 * 3、超时控制（若长时间未更新进度，可主动中断处理）。 <br>
 * 支持中断处理流程<br>
 * 1、回调函数的返回值为 int 类型，开发者可通过返回非零值（如 1）主动中断当前的转录过程（例如用户手动点击 “取消” 按钮时），返回 0 则表示继续处理。<br>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public record WhisperProgressCallback(
    String progressCallbackClass, String progressCallbackUserDataClass) {

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
  public MemorySegment progressCallback() {
    if (progressCallbackClass == null || progressCallbackClass.isBlank()) {
      throw new IllegalArgumentException("progressCallbackClass must not be null or blank.");
    }
    Class<?> clazz = Class.forName(progressCallbackClass);
    if (!clazz.isAssignableFrom(Function.class)) {
      throw new IllegalArgumentException(
          "progressCallbackClass must be a class that implements com.silong.foundation.utilities.whispercpp.generated.whisper_progress_callback.Function.");
    }
    return whisper_progress_callback.allocate(
        (whisper_progress_callback.Function) clazz.getDeclaredConstructor().newInstance(),
        Arena.global());
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
  public MemorySegment progressCallbackUserData() {
    if (progressCallbackUserDataClass == null || progressCallbackUserDataClass.isBlank()) {
      return MemorySegment.NULL;
    }
    Class<?> clazz = Class.forName(progressCallbackUserDataClass);
    if (!clazz.isAssignableFrom(Supplier.class)) {
      throw new IllegalArgumentException(
          "progressCallbackUserDataClass must be a class that implements java.util.function.Supplier<MemorySegment>.");
    }
    return ((Supplier<MemorySegment>) clazz.getDeclaredConstructor().newInstance()).get();
  }
}
