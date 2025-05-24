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

import com.silong.foundation.springboot.starter.raft.MemberRole;
import com.silong.foundation.springboot.starter.raft.MemberRoleChangedEvent;
import com.silong.foundation.springboot.starter.raft.service.generated.Request4AppendLogEntries;
import com.silong.foundation.springboot.starter.raft.service.generated.Request4Vote;
import com.silong.foundation.springboot.starter.raft.service.generated.Response4AppendEntries;
import com.silong.foundation.springboot.starter.raft.service.generated.Response4Vote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

/**
 * raft协议实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-20 10:03
 */
@Slf4j
@Service
class RaftServiceImpl implements RaftService {

  private StateMachine<MemberRole, MemberRoleChangedEvent> raftStateMachine;

  @Override
  public Response4Vote vote(Request4Vote request) {

    return null;
  }

  @Override
  public Response4AppendEntries appendEntries(Request4AppendLogEntries request) {
    return null;
  }

  @Autowired
  public void setRaftStateMachine(
      StateMachine<MemberRole, MemberRoleChangedEvent> raftStateMachine) {
    this.raftStateMachine = raftStateMachine;
  }
}
