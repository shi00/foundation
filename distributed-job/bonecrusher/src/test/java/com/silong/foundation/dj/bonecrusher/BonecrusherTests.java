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

import static com.silong.foundation.dj.bonecrusher.handler.ResourcesTransferHandler.classFqdn2Path;
import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.LOADING_CLASS_REQ;

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherServerProperties;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.bonecrusher.message.Messages.LoadingClassReq;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 21:36
 */
@Slf4j
@SpringBootTest(classes = BonecrusherApp4Test.class)
@TestPropertySource(locations = "classpath:application.properties")
@ExtendWith(SpringExtension.class)
public class BonecrusherTests {

  @Autowired private BonecrusherServerProperties properties;

  @Autowired private DataSyncServer bonecrusher;

  @Test
  @DisplayName("sendSyncMessage")
  public void test1() throws Exception {
    String fqdn = Bonecrusher.class.getName();
    try (DataSyncClient client =
        bonecrusher.newClient().connect(properties.getAddress(), properties.getPort())) {
      ByteBuf byteBuf =
          client.sendSync(
              Messages.Request.newBuilder()
                  .setType(LOADING_CLASS_REQ)
                  .setLoadingClass(LoadingClassReq.newBuilder().setClassFqdn(fqdn)));

      try (InputStream inputStream =
          Objects.requireNonNull(getClass().getResourceAsStream(classFqdn2Path(fqdn)))) {
        int available = inputStream.available();
        byte[] bytes = new byte[available];
        inputStream.read(bytes);

        Assertions.assertTrue(ByteBufUtil.equals(byteBuf, Unpooled.wrappedBuffer(bytes)));
      }
    }
  }

  @Test
  @DisplayName("sendAsyncMessage")
  public void test2() throws Exception {
    try (DataSyncClient client =
        bonecrusher.newClient().connect(properties.getAddress(), properties.getPort())) {
      String fqdn = Bonecrusher.class.getName();
      Future<ByteBuf> bufFuture =
          client.sendAsync(
              Messages.Request.newBuilder()
                  .setType(LOADING_CLASS_REQ)
                  .setLoadingClass(LoadingClassReq.newBuilder().setClassFqdn(fqdn)));

      try (InputStream inputStream =
          Objects.requireNonNull(getClass().getResourceAsStream(classFqdn2Path(fqdn)))) {
        int available = inputStream.available();
        byte[] bytes = new byte[available];
        inputStream.read(bytes);

        Assertions.assertTrue(ByteBufUtil.equals(bufFuture.get(), Unpooled.wrappedBuffer(bytes)));
      }
    }
  }

  @Test
  @DisplayName("cancelRequest")
  public void test3() throws Exception {
    try (DataSyncClient client =
        bonecrusher.newClient().connect(properties.getAddress(), properties.getPort())) {
      String fqdn = Bonecrusher.class.getName();
      Future<ByteBuf> bufFuture =
          client.sendAsync(
              Messages.Request.newBuilder()
                  .setType(LOADING_CLASS_REQ)
                  .setLoadingClass(LoadingClassReq.newBuilder().setClassFqdn(fqdn)));

      Thread.sleep(50);

      bufFuture.cancel(true);

      Throwable cause = bufFuture.cause();
      Assertions.assertTrue(
          cause == null || CancellationException.class.isAssignableFrom(cause.getClass()));

      Thread.sleep(3000);
    }
  }
}
