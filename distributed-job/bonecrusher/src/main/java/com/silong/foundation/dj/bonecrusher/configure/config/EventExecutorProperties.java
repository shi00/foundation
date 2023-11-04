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

package com.silong.foundation.dj.bonecrusher.configure.config;

import static com.silong.foundation.dj.bonecrusher.enu.EventExecutorType.UNORDERED;

import com.silong.foundation.dj.bonecrusher.enu.EventExecutorType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 事件执行器配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-04 9:18
 */
@Data
public class EventExecutorProperties {
  /** 事件执行器类型，默认：unordered */
  @NotNull private EventExecutorType eventExecutorType = UNORDERED;

  /** 事件执行器线程池名字，默认：BEE */
  @NotEmpty private String eventExecutorPoolName = "BEE";

  /** 事件执行器线程数量，默认：2 */
  @Positive private int eventExecutorThreads = 2;
}
