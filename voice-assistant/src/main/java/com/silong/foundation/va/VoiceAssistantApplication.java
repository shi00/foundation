/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 *  under the License.
 *
 */

package com.silong.foundation.va;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Duuid生成器服务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 00:25
 */
@SpringBootApplication
@OpenAPIDefinition(
    info =
        @Info(
            title = "${spring.application.name}",
            version = "v1.0.0",
            contact =
                @Contact(email = "louis2sin@gmail.com", name = "Silong Technologies Co., Ltd."),
            license =
                @License(
                    name = "Apache License 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0"),
            description = "A voice assistant named Little Pineapple."))
@SecuritySchemes({
  @SecurityScheme(type = APIKEY, name = "token", in = HEADER, description = "Access Token")
})
public class VoiceAssistantApplication {

  /**
   * 服务启动入口
   *
   * @param args 参数服务
   */
  public static void main(String[] args) {
    SpringApplication.run(VoiceAssistantApplication.class, args);
  }
}
