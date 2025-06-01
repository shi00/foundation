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
import static org.kordamp.bootstrapfx.BootstrapFX.bootstrapFXStylesheet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.llm.chatbot.desktop.config.Configuration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天助手程序入口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Slf4j
public class ChatbotDesktopApplication extends Application {
  /** 默认配置文件 */
  private static final String DEFAULT_CONFIG_FILE_PATH = "/config/configuration.json";

  /** 配置文件路径KEY */
  public static final String CONFIG_FILE = "CONFIG_FILE";

  /** 全局配置 */
  public static final Configuration CONFIGURATION;

  static Stage primaryStage;

  static {
    String path = System.getProperty(CONFIG_FILE, DEFAULT_CONFIG_FILE_PATH);
    log.debug("config path: {}", path);
    var mapper = new ObjectMapper();
    try {
      if (DEFAULT_CONFIG_FILE_PATH.equals(path)) {
        CONFIGURATION = mapper.readValue(loadURL(path), Configuration.class);
      } else {
        CONFIGURATION = mapper.readValue(new File(path), Configuration.class);
      }
      log.debug("config: {}", CONFIGURATION);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static URL loadURL(String path) {
    return ChatbotDesktopApplication.class.getResource(path);
  }

  private static InputStream loadStream(String path) {
    return ChatbotDesktopApplication.class.getResourceAsStream(path);
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    ChatbotDesktopApplication.primaryStage = primaryStage;
    ResourceBundle resourceBundle = ResourceBundle.getBundle("/i18n/Messages", Locale.getDefault());
    FXMLLoader fxmlLoader = new FXMLLoader(loadURL(CONFIGURATION.loginView()), resourceBundle);
    Parent root = fxmlLoader.load();
    Scene scene =
        new Scene(
            root,
            CONFIGURATION.loginWindowSize().width(),
            CONFIGURATION.loginWindowSize().height());
    scene.getStylesheets().add(bootstrapFXStylesheet()); // 加载bootstrapfx.css
    primaryStage.initStyle(UNDECORATED);
    primaryStage.setTitle(resourceBundle.getString("app.title"));
    primaryStage.getIcons().add(new Image(loadStream(CONFIGURATION.icon())));
    primaryStage.setResizable(false);
    primaryStage.setScene(scene);
    primaryStage.setOnCloseRequest(windowEvent -> Platform.exit());
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
