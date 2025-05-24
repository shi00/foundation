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

import com.silong.foundation.springboot.starter.raft.service.generated.*;
import com.silong.foundation.springboot.starter.raft.service.generated.ReactorRaftServiceGrpc.RaftServiceImplBase;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

/**
 * Raft协议服务接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-16 15:55
 */
@Slf4j
@GrpcService
public class RaftGrpcService extends RaftServiceImplBase {

  private RaftService raftService;

  @Override
  public Mono<Response4Vote> vote(Mono<Request4Vote> request) {
    return request.map(raftService::vote);
  }

  @Override
  public Mono<Response4AppendEntries> appendEntries(Mono<Request4AppendLogEntries> request) {
    return request.map(raftService::appendEntries);
  }

  @Autowired
  public void setRaftService(RaftService raftService) {
    this.raftService = raftService;
  }
}
