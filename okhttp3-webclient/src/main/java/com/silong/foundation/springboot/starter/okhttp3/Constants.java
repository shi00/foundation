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
package com.silong.foundation.springboot.starter.okhttp3;

/**
 * 常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-13 11:20
 */
public interface Constants {
  /** 代理鉴权信息 */
  String PROXY_AUTHORIZATION = "Proxy-Authorization";
  /** 网络拦截器Qualifier */
  String NETWORK_INTERCEPTORS = "network-interceptors";
  /** 普通拦截器Qualifier */
  String NORMAL_INTERCEPTORS = "normal-interceptors";
  /** TLSv1.2 */
  String TLS_12 = "TLSv1.2";
  /** TLSv1.3 */
  String TLS_13 = "TLSv1.3";
  /**
   * The HTTP {@code Content-Type} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
   */
  String CONTENT_TYPE = "Content-Type";

  /**
   * A String equivalent of {@link MediaType#APPLICATION_JSON}.
   * @see #APPLICATION_JSON_UTF8_VALUE
   */
  String APPLICATION_JSON_VALUE = "application/json";
}
