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

package com.silong.foundation.springboot.starter.raft.fsm;

import com.silong.foundation.springboot.starter.raft.MemberRole;
import com.silong.foundation.springboot.starter.raft.MemberRoleChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

/**
 * Raft状态机监听器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-24 22:05
 */
@Slf4j
public class RaftStateMachineListener
    extends StateMachineListenerAdapter<MemberRole, MemberRoleChangedEvent> {
  @Override
  public void stateChanged(
      State<MemberRole, MemberRoleChangedEvent> from,
      State<MemberRole, MemberRoleChangedEvent> to) {
    log.info("Raft's state changed: from={}, to={}.", from, to);
  }
}
