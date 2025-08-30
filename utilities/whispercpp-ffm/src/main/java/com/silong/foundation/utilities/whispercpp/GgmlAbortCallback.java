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

import com.silong.foundation.utilities.whispercpp.generated.ggml_abort_callback;
import com.silong.foundation.utilities.whispercpp.generated.ggml_abort_callback.Function;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import lombok.SneakyThrows;

/**
 * ggml_abort_callback 是 ggml 库（whisper.cpp 的底层张量计算引擎） 提供的中断回调函数，核心作用是在 ggml
 * 执行耗时计算（如模型推理中的张量运算）时，允许外部通过 “主动检查中断信号” 的方式终止计算，避免无响应的阻塞，是实现 “可取消计算” 的关键接口。
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public record GgmlAbortCallback(String abortCallbackClass, String abortCallbackUserDataClass) {

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
  public MemorySegment abortCallback() {
    if (abortCallbackClass == null || abortCallbackClass.isBlank()) {
      throw new IllegalArgumentException("abortCallbackClass must not be null or blank.");
    }
    Class<?> clazz = Class.forName(abortCallbackClass);
    if (!clazz.isAssignableFrom(Function.class)) {
      throw new IllegalArgumentException(
          "abortCallbackClass must be a class that implements com.silong.foundation.utilities.whispercpp.generated.ggml_abort_callback.Function.");
    }
    return ggml_abort_callback.allocate(
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
  public MemorySegment abortCallbackUserData() {
    if (abortCallbackUserDataClass == null || abortCallbackUserDataClass.isBlank()) {
      return MemorySegment.NULL;
    }
    Class<?> clazz = Class.forName(abortCallbackUserDataClass);
    if (!clazz.isAssignableFrom(Supplier.class)) {
      throw new IllegalArgumentException(
          "abortCallbackUserDataClass must be a class that implements java.util.function.Supplier<MemorySegment>.");
    }
    return ((Supplier<MemorySegment>) clazz.getDeclaredConstructor().newInstance()).get();
  }
}
