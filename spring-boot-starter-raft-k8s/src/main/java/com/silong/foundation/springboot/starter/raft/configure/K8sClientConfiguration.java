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

package com.silong.foundation.springboot.starter.raft.configure;

import static java.util.concurrent.TimeUnit.SECONDS;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import java.io.IOException;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;

/**
 * k8s客户端配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-16 11:11
 */
//@Configuration
public class K8sClientConfiguration {

  @Bean
  ApiClient apiClient() throws IOException {
    // loading the in-cluster config, including:
    //  1. service-account CA
    //  2. service-account bearer-token
    //  3. service-account namespace
    //  4. master endpoint(ip, port) from pre-set environment variables
    ApiClient apiClient = ClientBuilder.cluster().build();

    // infinite timeout for watch
    OkHttpClient httpClient =
        apiClient.getHttpClient().newBuilder().readTimeout(0, SECONDS).build();
    apiClient.setHttpClient(httpClient);
    io.kubernetes.client.openapi.Configuration.setDefaultApiClient(apiClient);
    return apiClient;
  }

  @Bean
  CoreV1Api coreV1Api(ApiClient apiClient) {
    return new CoreV1Api(apiClient);
  }
}
