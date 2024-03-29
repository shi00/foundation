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

package com.silong.foundation.dj.mixmaster.core;

import com.silong.foundation.dj.mixmaster.MixmasterApp4Test;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
@SpringBootTest(classes = MixmasterApp4Test.class)
@TestPropertySource(
    locations = "classpath:application.properties",
    properties = {
      "mixmaster.long-haul.persist-data-path=./target/a",
      "mixmaster.instance-name=deer"
    })
@ExtendWith(SpringExtension.class)
public class Node1Tests {

  @Autowired private DefaultDistributedEngine engine;

  @Test
  public void test1() {
    engine.waitUntilConnected();

    while (engine.currentView().size() != 3) {
      Thread.onSpinWait();
    }

    Assertions.assertEquals(engine.currentView().size(), 3);
  }
}
