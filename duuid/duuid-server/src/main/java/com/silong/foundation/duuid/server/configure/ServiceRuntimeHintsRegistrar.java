/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.duuid.server.configure;

import io.netty.channel.epoll.EpollChannelOption;
import lombok.SneakyThrows;
import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.ConcurrentHistogram;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 本地镜像构建提示信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 21:23
 */
class ServiceRuntimeHintsRegistrar implements RuntimeHintsRegistrar {
  @Override
  @SneakyThrows
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    ReflectionHints reflection = hints.reflection();
    reflection.registerField(EpollChannelOption.class.getField("TCP_USER_TIMEOUT"));
    reflection.registerConstructor(
        ConcurrentHistogram.class.getConstructor(AbstractHistogram.class), ExecutableMode.INVOKE);
  }
}
