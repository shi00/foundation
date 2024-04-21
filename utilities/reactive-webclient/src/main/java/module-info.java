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

/**
 * 模块定义
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-21 11:10
 */
module reactive.webclient {
  requires com.fasterxml.jackson.databind;
  requires com.github.spotbugs.annotations;
  requires io.netty.handler;
  requires io.netty.transport;
  requires jakarta.validation;
  requires static lombok;
  requires org.slf4j;
  requires reactor.netty.core;
  requires reactor.netty.http;
  requires spring.core;
  requires spring.web;
  requires spring.webflux;

  exports com.silong.foundation.webclient.reactive.config;
  exports com.silong.foundation.webclient.reactive;
}
