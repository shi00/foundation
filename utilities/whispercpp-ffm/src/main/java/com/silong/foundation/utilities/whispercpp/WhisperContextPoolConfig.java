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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;

/**
 * whisper_context对象缓存配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
@Data
public class WhisperContextPoolConfig {

  /** 从对象池获取对象等待的最大时长，默认：3分钟 */
  @NotNull private Duration maxWait = Duration.ofMinutes(3);

  /** 池内对象的最大总数，默认：2 */
  @Positive private int maxTotal = 2;

  /** 池内最大空闲对象数，默认：2 */
  @Positive private int maxIdle = 2;

  /** 池内最小空闲对象数，默认：1 */
  @Positive private int minIdle = 1;

  /** 当池无可用对象时，是否阻塞等待（true：阻塞；false：立即抛异常）。默认：true */
  private boolean blockWhenExhausted = true;
}
