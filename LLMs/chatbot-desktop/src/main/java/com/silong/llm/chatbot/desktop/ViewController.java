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

import static com.silong.llm.chatbot.desktop.ChatbotDesktopApplication.primaryStage;
import static javafx.scene.Cursor.CLOSED_HAND;
import static javafx.scene.Cursor.DEFAULT;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.NonNull;

/**
 * 视图控制器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
abstract class ViewController {

  private double xOffset;
  private double yOffset;

  /**
   * 配置登录窗口支持拖拉拽
   *
   * @param pane 面板
   */
  protected void configureLoginWindowsDragAndDrop(@NonNull Pane pane) {
    pane.setOnMousePressed(
        event -> {
          xOffset = primaryStage.getX() - event.getScreenX();
          yOffset = primaryStage.getY() - event.getScreenY();
          pane.setCursor(CLOSED_HAND);
        });

    pane.setOnMouseDragged(
        event -> {
          primaryStage.setX(event.getScreenX() + xOffset);
          primaryStage.setY(event.getScreenY() + yOffset);
        });

    pane.setOnMouseReleased(event -> pane.setCursor(DEFAULT));
  }

  /**
   * 最小化窗口
   *
   * @param stage 窗口
   */
  protected void minimize(@NonNull Stage stage) {
    stage.setIconified(true);
  }

  /** 退出 */
  protected void exit() {
    Platform.exit();
    System.exit(0);
  }
}
