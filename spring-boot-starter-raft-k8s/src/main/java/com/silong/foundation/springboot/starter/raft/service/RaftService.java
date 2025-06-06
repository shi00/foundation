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

package com.silong.foundation.springboot.starter.raft.service;

import com.silong.foundation.springboot.starter.raft.service.generated.Request4AppendLogEntries;
import com.silong.foundation.springboot.starter.raft.service.generated.Request4Vote;
import com.silong.foundation.springboot.starter.raft.service.generated.Response4AppendEntries;
import com.silong.foundation.springboot.starter.raft.service.generated.Response4Vote;
import jakarta.validation.constraints.NotNull;

/**
 * raft服务接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-20 10:09
 */
public interface RaftService {

  /**
   * 选举接口，由candidate发送选举请求
   *
   * @param request 选举请求
   * @return 选举响应
   */
  @NotNull
  Response4Vote vote(@NotNull Request4Vote request);

  /**
   * 追加日志或握手接口
   *
   * @param request 请求
   * @return 响应
   */
  @NotNull
  Response4AppendEntries appendEntries(@NotNull Request4AppendLogEntries request);
}
