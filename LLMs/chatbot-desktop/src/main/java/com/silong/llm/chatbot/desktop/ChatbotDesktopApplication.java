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

package com.silong.llm.chatbot.desktop;

import static javafx.stage.StageStyle.UNDECORATED;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 聊天助手程序入口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
public class ChatbotDesktopApplication extends Application {

  private URL loadURL(String path) {
    return getClass().getResource(path);
  }

  private InputStream loadStream(String path) {
    return getClass().getResourceAsStream(path);
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(loadURL("/views/login-view.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 360, 420);
    primaryStage.initStyle(UNDECORATED);
    primaryStage.setResizable(false);
    primaryStage.setScene(scene);
    primaryStage.centerOnScreen();
    primaryStage.show();
  }

  /**
   * 启动入口
   *
   * @param args 参数
   */
  public static void main(String[] args) {
    launch(args);
  }
}
