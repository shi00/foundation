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

package com.silong.foundation.springboot.starter.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-21 14:56
 */
@RestController
public class TestController {

  @Value("${encrypt.id}")
  private String id;

  @Value("${encrypt.name}")
  private String name;

  @Value("${encrypt.pass}")
  private String pass;

  @PostMapping("/id")
  public String generateId() {
    return id;
  }

  @GetMapping("/pass")
  public String getPass() {
    return pass;
  }

  @GetMapping("/name")
  public String getName() {
    return name;
  }
}
