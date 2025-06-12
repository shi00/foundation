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
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.HiddenSidesPane;
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

    // 图标大小 16px
    minimizedBtn.setGraphic(FontIcon.of(BootstrapIcons.DASH_CIRCLE, 16));

    // 图标大小 16px
    closeBtn.setGraphic(FontIcon.of(BootstrapIcons.X_CIRCLE, 16));

    // 图标大小 16px
    newConversation.setGraphic(FontIcon.of(BootstrapIcons.PERSON_PLUS_FILL, 16));

    // 1. 创建主内容区域
    Label centerLabel = new Label("主内容区域");
    VBox centerContent = new VBox(centerLabel);
    centerContent.setStyle("-fx-background-color: #E0E0E0; -fx-padding: 20;");

    // 2. 创建边缘滑出内容（顶部、底部、左侧、右侧）
    Button topButton = new Button("顶部工具栏");
    VBox topContent = new VBox(topButton);
    topContent.setStyle("-fx-background-color: #FFCC80; -fx-padding: 10;");

    Button leftButton = new Button("左侧菜单");
    VBox leftContent = new VBox(leftButton);
    leftContent.setStyle("-fx-background-color: #81D4FA; -fx-padding: 10;");

    // 3. 创建HiddenSidesPane并添加内容
    hiddenSidesPane.setContent(centerContent); // 设置主内容
    hiddenSidesPane.setTop(topContent); // 设置顶部内容
    hiddenSidesPane.setLeft(leftContent); // 设置左侧内容

    // 4. 配置触发行为（默认鼠标悬停触发）
    hiddenSidesPane.setTriggerDistance(30); // 边缘触发距离（像素）
    hiddenSidesPane.setAnimationDelay(Duration.millis(200)); // 动画延迟（毫秒）

    // 创建Tooltip并设置属性
    //    Tooltip tooltip = new Tooltip();
    //    tooltip.setTextAlignment(TextAlignment.CENTER);
    //    tooltip.setText(resources.getString("foldbtn.tooltip"));
    //    tooltip.setShowDelay(Duration.millis(100)); // 鼠标悬停200ms后显示
    //    tooltip.setHideDelay(Duration.millis(100)); // 鼠标移开1000ms后隐藏
    //    tooltip.setShowDuration(Duration.INDEFINITE); // 永久显示，直到鼠标移开
    //    foldBtn.setTooltip(tooltip);
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
