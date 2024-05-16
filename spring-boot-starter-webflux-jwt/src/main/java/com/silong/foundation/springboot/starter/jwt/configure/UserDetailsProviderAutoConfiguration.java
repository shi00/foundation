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

package com.silong.foundation.springboot.starter.jwt.configure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

import com.silong.foundation.springboot.starter.jwt.configure.config.JWTAuthProperties;
import com.silong.foundation.springboot.starter.jwt.provider.DefaultUserDetailsProvider;
import com.silong.foundation.springboot.starter.jwt.provider.UserDetailsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置用户信息提供者
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-14 22:58
 */
@Configuration
@ConditionalOnProperty(prefix = "jwt-auth.hazelcast-config", value = "cluster-name")
@EnableConfigurationProperties(JWTAuthProperties.class)
@ConditionalOnWebApplication(type = REACTIVE)
public class UserDetailsProviderAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  UserDetailsProvider defaultUserDetailsProvider() {
    return new DefaultUserDetailsProvider();
  }
}
