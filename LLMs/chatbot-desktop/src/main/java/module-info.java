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

module chatbot.desktop {
  requires javafx.controls;
  requires javafx.fxml;
  requires com.fasterxml.jackson.databind;
  requires org.slf4j;
  requires org.apache.httpcomponents.client5.httpclient5;
  requires org.apache.httpcomponents.core5.httpcore5.h2;
  requires org.controlsfx.controls;
  requires org.apache.httpcomponents.core5.httpcore5;
  requires jakarta.annotation;
  requires atlantafx.base;
  requires org.kordamp.ikonli.core;
  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.ikonli.bootstrapicons;
  requires org.kordamp.ikonli.fontawesome6;
  requires static lombok;

  opens views to
      javafx.fxml;
  opens com.silong.llm.chatbot.desktop to
      com.fasterxml.jackson.databind,
      javafx.fxml,
      javafx.graphics;
  opens config to
      com.fasterxml.jackson.databind;
  opens com.silong.llm.chatbot.desktop.config to
      com.fasterxml.jackson.databind,
      javafx.fxml,
      javafx.graphics;
}
