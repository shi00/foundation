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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * 登录界面控制器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Slf4j
public class LoginViewController implements Initializable {

  @FXML private BorderPane mainLayout;

  @FXML private Button minimizedBtn;

  @FXML private Button closeBtn;

  @FXML private Label hostLabel;

  @FXML private Label portLabel;

  @FXML private Label credentialsLabel;

  @FXML private Button loginBtn;

  @FXML private CustomPasswordField credentialsTextField;

  @FXML private CustomTextField hostTextField;

  @FXML private CustomTextField portTextField;

  private double xOffset;
  private double yOffset;

  @FXML
  void handleLoginAction(ActionEvent event) {}

  @FXML
  void handleCloseAction(ActionEvent event) {
    Platform.exit();
    System.exit(0);
  }

  @FXML
  void handleMinimizedAction(ActionEvent event) {
    primaryStage.setIconified(true);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    /* Drag and Drop */
    configureLoginWindowsDragAndDrop();

    // 图标大小 16px
    minimizedBtn.setGraphic(FontIcon.of(BootstrapIcons.DASH_CIRCLE, 16));

    // 图标大小 16px
    closeBtn.setGraphic(FontIcon.of(BootstrapIcons.X_CIRCLE, 16));

    hostLabel.setGraphic(FontIcon.of(BootstrapIcons.CLOUD_ARROW_UP, 64));

    portLabel.setGraphic(FontIcon.of(FontAwesomeSolid.ETHERNET, 64));

    credentialsLabel.setGraphic(FontIcon.of(FontAwesomeSolid.KEY, 64));
  }

  /** 配置登录窗口支持拖拉拽 */
  private void configureLoginWindowsDragAndDrop() {
    mainLayout.setOnMousePressed(
        event -> {
          xOffset = primaryStage.getX() - event.getScreenX();
          yOffset = primaryStage.getY() - event.getScreenY();
          mainLayout.setCursor(CLOSED_HAND);
        });

    mainLayout.setOnMouseDragged(
        event -> {
          primaryStage.setX(event.getScreenX() + xOffset);
          primaryStage.setY(event.getScreenY() + yOffset);
        });

    mainLayout.setOnMouseReleased(event -> mainLayout.setCursor(DEFAULT));
  }
}
