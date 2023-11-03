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

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherServerProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.bonecrusher.message.Messages.Request.Builder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.ViewId;
import org.jgroups.stack.IpAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 21:36
 */
@SpringBootTest(classes = BonecrusherApp4Test.class)
@TestPropertySource(locations = "classpath:application.properties")
@ExtendWith(SpringExtension.class)
public class BonecrusherTests {

  @Autowired private ApplicationEventPublisher publisher;

  @Autowired private DataSyncServer bonecrusher;

  @Autowired private BonecrusherServerProperties properties;

  @Test
  public void startServer() throws Exception {
    Address creator = new IpAddress("127.0.0.1:43434");
    ViewId oldViewId = new ViewId(creator, 1);
    ViewId newViewId = new ViewId(creator, 2);

    View oldView = new View(oldViewId, List.of(creator));

    View newView = new View(newViewId, List.of(new IpAddress("127.0.0.1:43436"), creator));

    publisher.publishEvent(new ClusterViewChangedEvent("cluster-test", creator, oldView, newView));
    bonecrusher.start(false);

    try (DataSyncClient client =
        bonecrusher.client().connect(properties.getAddress(), properties.getPort())) {
      ByteBuf byteBuf =
          client.<Builder, ByteBuf>sendSync(
              Messages.Request.newBuilder()
                  .setType(Messages.Type.LOADING_CLASS_REQ)
                  .setLoadingClass(
                      Messages.LoadingClassReq.newBuilder()
                          .setClassFqdn("com.silong.foundation.dj.bonecrusher.Bonecrusher")));

      try (InputStream inputStream =
          Objects.requireNonNull(
              getClass()
                  .getResourceAsStream(
                      "/com/silong/foundation/dj/bonecrusher/Bonecrusher.class"))) {
        int available = inputStream.available();
        byte[] bytes = new byte[available];
        inputStream.read(bytes);

        Assertions.assertTrue(ByteBufUtil.equals(byteBuf, Unpooled.wrappedBuffer(bytes)));
      }
    }
  }
}
