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

package com.silong.foundation.dj.bonecrusher;

import com.silong.foundation.dj.bonecrusher.event.JoinClusterEvent;
import com.silong.foundation.dj.bonecrusher.event.ViewChangedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.ViewId;
import org.jgroups.stack.IpAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 测试服务启动入口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-26 18:58
 */
@SpringBootApplication
@Slf4j
public class BonecrusherApp4Test {

  @Autowired private ApplicationEventPublisher publisher;

  @Autowired private DataSyncServer bonecrusher;

  private void fireViewChangedEvent() throws Exception {
    Address creator = new IpAddress("127.0.0.1:43434");
    ViewId oldViewId = new ViewId(creator, 1);
    ViewId newViewId = new ViewId(creator, 2);
    View oldView = new View(oldViewId, List.of(creator));
    View newView = new View(newViewId, List.of(new IpAddress("127.0.0.1:43436"), creator));
    publisher.publishEvent(new ViewChangedEvent(this, newView));
    publisher.publishEvent(new JoinClusterEvent(this, "test-cluster", creator));
  }

  @PostConstruct
  public void startServer() throws Exception {
    fireViewChangedEvent();
    bonecrusher.start(false);
    log.info("=========================== Start Server =============================");
  }

  @PreDestroy
  public void shutdownServer() throws Exception {
    bonecrusher.shutdown();
    log.info("=========================== Shutdown Server =============================");
  }

  /**
   * 服务启动入口
   *
   * @param args 参数服务
   */
  public static void main(String[] args) {
    SpringApplication.run(BonecrusherApp4Test.class, args);
  }
}
