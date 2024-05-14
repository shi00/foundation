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

package com.silong.foundation.springboot.starter.jwt.common;

/**
 * 鉴权请求头名称常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-18 17:04
 */
public interface Constants {

  /** 访问token */
  String ACCESS_TOKEN = "Access-Token";

  /** 用户唯一标识 */
  String IDENTITY = "Identity";

  /** token 缓存名 */
  String TOKEN_CACHE = "token-cache";
}
