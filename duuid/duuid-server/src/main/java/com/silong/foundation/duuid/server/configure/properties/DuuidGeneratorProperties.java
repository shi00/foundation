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
package com.silong.foundation.duuid.server.configure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import static com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator.Constants.*;

/**
 * 服务配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Validated
@ConfigurationProperties(prefix = "duuid.generator")
public class DuuidGeneratorProperties {
  /** workerId占用比特数量，默认：23 */
  @Positive private int workerIdBits = DEFAULT_WORK_ID_BITS;

  /** deltaDays占用比特数量，默认：15 */
  @Positive private int deltaDaysBits = DEFAULT_DELTA_DAYS_BITS;

  /** sequence占用比特数量，默认：25 */
  @Positive private int sequenceBits = DEFAULT_SEQUENCE_BITS;

  /** 序列号 */
  @PositiveOrZero private long sequence = 0;

  /** 队列长度，默认：8192 */
  @Positive private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

  /** id增量随机数上边界，默认：10 */
  @Positive private int maxRandomIncrement = DEFAULT_MAX_RANDOM_INCREMENT;

  /** 是否开启生成id随机增长，避免出现连续id */
  private boolean enableSequenceRandom;

  /** 启用的workerId分配器权限定名 */
  private String workerIdAllocatorFqdn;
}
