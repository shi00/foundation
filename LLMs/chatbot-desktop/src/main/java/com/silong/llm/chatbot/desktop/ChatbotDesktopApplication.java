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

import static com.silong.llm.chatbot.desktop.utils.ResourceLoader.loadStream;
import static com.silong.llm.chatbot.desktop.utils.ResourceLoader.loadURL;
import static javafx.stage.StageStyle.UNDECORATED;

import atlantafx.base.theme.Dracula;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.llm.chatbot.desktop.config.Configuration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.NonNull;
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

  /** 返回示例：Windows: "C:\Users\用户名", Linux: "/home/用户名" */
  private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

  /** 默认配置文件 */
  private static final String DEFAULT_CONFIG_FILE_PATH = "/config/configuration.json";

  /** 配置文件路径KEY */
  public static final String CONFIG_FILE = "config.file";

  /** 全局配置 */
  public static final Configuration CONFIGURATION;

  /** 应用数据存储目录 */
  private static Path appHome;

  /** 主窗口 */
  private static Stage primaryStage;

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

  @NonNull
  public static Path getAppHome() {
    return appHome;
  }

  @NonNull
  public static Stage getPrimaryStage() {
    return primaryStage;
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    ChatbotDesktopApplication.primaryStage = primaryStage;
    ResourceBundle resourceBundle =
        ResourceBundle.getBundle(CONFIGURATION.i18nPath(), Locale.getDefault());
    String appName = resourceBundle.getString("app.title");
    createAppDir(appName);
    FXMLLoader fxmlLoader = new FXMLLoader(loadURL(CONFIGURATION.loginViewPath()), resourceBundle);
    Parent root = fxmlLoader.load();
    Scene scene =
        new Scene(
            root,
            CONFIGURATION.loginWindowSize().width(),
            CONFIGURATION.loginWindowSize().height());
    String stylesheet = new Dracula().getUserAgentStylesheet();
    scene.getStylesheets().add(stylesheet);
    Application.setUserAgentStylesheet(stylesheet);

    primaryStage.setTitle(appName);
    primaryStage.initStyle(UNDECORATED);
    primaryStage.getIcons().add(new Image(loadStream(CONFIGURATION.iconPath())));
    primaryStage.setResizable(false);
    primaryStage.setScene(scene);
    primaryStage.setOnCloseRequest(windowEvent -> Platform.exit());
    primaryStage.centerOnScreen();
    primaryStage.show();
  }

  private void createAppDir(String dir) throws IOException {
    appHome = USER_HOME.resolve(dir);
    Files.createDirectories(appHome);
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
