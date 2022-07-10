/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.config;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

import static com.silong.foundation.devastator.config.AuthTokenConfig.Algorithm.HMAC_SHA256;

/**
 * 集群节点加入集群鉴权配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-15 22:12
 */
@Data
@Accessors(fluent = true)
public class AuthTokenConfig implements Serializable {

  @Serial private static final long serialVersionUID = -2658224355125878427L;

  /** 鉴权算法 */
  public enum Algorithm {
    /** HMAC-ShA256 */
    HMAC_SHA256,
    /** HMAC-ShA512 */
    HMAC_SHA512,
    /** HMAC-ShA384 */
    HMAC_SHA384
  }

  /** 鉴权算法，默认： HMAC_SHA256 */
  @NotNull private Algorithm algorithm = HMAC_SHA256;

  /** 鉴权密钥 */
  @NotEmpty private String authKey;
}
