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

import com.silong.llm.chatbot.desktop.client.AsyncRestClient;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.tools.Borders;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * 聊天界面控制器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Slf4j
public class ChatViewController extends ViewController implements Initializable {

  @FXML private Button closeBtn;

  @FXML private Button minimizedBtn;

  @FXML private Button newConversation;

  @FXML private HiddenSidesPane hiddenSidesPane;

  @FXML private BorderPane mainLayout;

  @Setter private AsyncRestClient restClient;

  private String conversationId;

  private ResourceBundle resourceBundle;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.resourceBundle = resources;
    this.configureLoginWindowsDragAndDrop(mainLayout);

    configureButton(
        minimizedBtn,
        BootstrapIcons.DASH_CIRCLE,
        16,
        null,
        resources.getString("minimizedBtn.tooltip"));

    configureButton(
        closeBtn, BootstrapIcons.X_CIRCLE, 16, null, resources.getString("closeBtn.tooltip"));

    // 图标大小 16px
    newConversation.setGraphic(FontIcon.of(BootstrapIcons.PERSON_PLUS_FILL, 16));

    // 1. 创建主内容区域
    var chatArea = new TextArea();
    chatArea.setPromptText("Chat Area");
    var stackPane = new StackPane(chatArea);
    //    stackPane.setStyle("-fx-background-color: #E0E0E0; -fx-padding: 20;");
    var centerContent =
        Borders.wrap(stackPane)
            .etchedBorder()
            .outerPadding(2)
            .innerPadding(1)
            .radius(2)
            .highlight(Color.DARKSEAGREEN)
            .build()
            .build();

    Button leftButton = new Button("左侧菜单");
    VBox leftContent = new VBox(leftButton);
    leftContent.setStyle("-fx-background-color: #81D4FA; -fx-padding: 10;");

    // 3. 创建HiddenSidesPane并添加内容
    hiddenSidesPane.setContent(centerContent); // 设置主内容
    hiddenSidesPane.setLeft(leftContent); // 设置左侧内容

    // 4. 配置触发行为（默认鼠标悬停触发）
    hiddenSidesPane.setTriggerDistance(30); // 边缘触发距离（像素）
    hiddenSidesPane.setAnimationDelay(Duration.millis(200)); // 动画延迟（毫秒）
  }

  @FXML
  void handleCloseAction(ActionEvent event) {
    exit();
  }

  @FXML
  void handleMinimizedAction(ActionEvent event) {
    minimize(primaryStage);
  }

  @FXML
  void handleNewConversationAction(ActionEvent event) {
    this.conversationId = UUID.randomUUID().toString().replace("-", "");
  }
}
