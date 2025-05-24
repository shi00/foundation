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

package com.silong.foundation.springboot.starter.raft.configure;

import static com.silong.foundation.springboot.starter.raft.MemberRole.*;
import static com.silong.foundation.springboot.starter.raft.MemberRoleChangedEvent.*;

import com.silong.foundation.springboot.starter.raft.MemberRole;
import com.silong.foundation.springboot.starter.raft.MemberRoleChangedEvent;
import com.silong.foundation.springboot.starter.raft.fsm.RaftStateMachineListener;
import java.util.EnumSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * raft状态机配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-16 16:42
 */
@Configuration
@EnableStateMachine
public class RaftStateMachineConfiguration
    extends EnumStateMachineConfigurerAdapter<MemberRole, MemberRoleChangedEvent> {

  @Bean
  RaftStateMachineListener stateMachineListener() {
    return new RaftStateMachineListener();
  }

  @Override
  public void configure(
      StateMachineConfigurationConfigurer<MemberRole, MemberRoleChangedEvent> config)
      throws Exception {
    config
        .withConfiguration()
        .listener(stateMachineListener())
        .machineId("Raft-FSM")
        .autoStartup(true);
  }

  @Override
  public void configure(
      StateMachineTransitionConfigurer<MemberRole, MemberRoleChangedEvent> transitions)
      throws Exception {
    transitions
        .withExternal()
        .source(FOLLOWER)
        .target(CANDIDATE)
        .event(HEARTBEAT_TIMEOUT)
        .and()
        .withExternal()
        .source(CANDIDATE)
        .target(LEADER)
        .event(ELECTION_VICTORY)
        .and()
        .withExternal()
        .source(CANDIDATE)
        .target(CANDIDATE)
        .event(ELECTION_TIMEOUT)
        .and()
        .withExternal()
        .source(CANDIDATE)
        .target(FOLLOWER)
        .event(HIGH_TERM)
        .and()
        .withExternal()
        .source(LEADER)
        .target(FOLLOWER)
        .event(HIGH_TERM);
  }

  @Override
  public void configure(StateMachineStateConfigurer<MemberRole, MemberRoleChangedEvent> states)
      throws Exception {
    states.withStates().initial(FOLLOWER).states(EnumSet.allOf(MemberRole.class));
  }
}
