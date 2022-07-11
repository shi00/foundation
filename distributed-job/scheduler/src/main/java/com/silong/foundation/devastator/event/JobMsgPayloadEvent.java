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
package com.silong.foundation.devastator.event;

import com.silong.foundation.devastator.model.Devastator.JobMsgPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务消息事件
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-29 22:38
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
public class JobMsgPayloadEvent implements AutoCloseable, Serializable {

  @Serial private static final long serialVersionUID = -8375488221362284221L;

  /** 任务消息 */
  private JobMsgPayload jobMsgPayload;

  /** 原始数据 */
  private byte[] rawData;

  @Override
  public void close() {
    jobMsgPayload = null;
    rawData = null;
  }
}