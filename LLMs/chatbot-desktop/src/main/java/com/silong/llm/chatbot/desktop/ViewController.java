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
import static javafx.scene.text.TextAlignment.CENTER;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.NonNull;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

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

  /**
   * 配置按钮的图标和tooltip
   *
   * @param button 按钮
   * @param ikon icon
   * @param ikonSize icon大小
   * @param tooltipText tip
   */
  protected void configureButton(
      @NonNull Button button,
      Ikon ikon,
      int ikonSize,
      EventHandler<ActionEvent> eventEventHandler,
      String tooltipText) {
    if (ikon != null) {
      button.setGraphic(FontIcon.of(ikon, ikonSize));
    }

    if (tooltipText != null) {
      configureButtonToolkits(button, tooltipText);
    }

    if (eventEventHandler != null) {
      button.setOnAction(eventEventHandler);
    }
  }

  /**
   * 配置按钮的tooltip
   *
   * @param button 按钮
   * @param tooltipText 提示
   */
  private void configureButtonToolkits(Button button, String tooltipText) {
    Tooltip tooltip = new Tooltip();
    tooltip.setTextAlignment(CENTER);
    tooltip.setText(tooltipText);
    tooltip.setShowDelay(Duration.millis(100)); // 鼠标悬停200ms后显示
    tooltip.setHideDelay(Duration.millis(100)); // 鼠标移开1000ms后隐藏
    tooltip.setShowDuration(Duration.INDEFINITE); // 永久显示，直到鼠标移开
    button.setTooltip(tooltip);
  }
}
