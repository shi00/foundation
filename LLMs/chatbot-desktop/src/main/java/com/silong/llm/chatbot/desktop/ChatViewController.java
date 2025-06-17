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
import static javafx.geometry.Pos.*;

import com.silong.llm.chatbot.desktop.client.AsyncRestClient;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.tools.Borders;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
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

  private static final int ITEMS_PER_PAGE = 10;

  @FXML private Button closeBtn;

  @FXML private Button minimizedBtn;

  @FXML private Button newConversationBtn;

  @FXML private HiddenSidesPane hiddenSidesPane;

  @FXML private BorderPane mainLayout;

  @Setter private AsyncRestClient restClient;

  private String conversationId;

  private ResourceBundle resourceBundle;

  private TitledPane buildFunctionTitledPane() {
    var functionListVBox = new VBox();
    functionListVBox.setPadding(new Insets(5));
    functionListVBox.setSpacing(10);
    functionListVBox.setAlignment(TOP_LEFT);

    var aiSearchBtn = new Button("AI搜索", FontIcon.of(FontAwesomeSolid.SEARCH, 16));
    aiSearchBtn.setAlignment(CENTER);
    aiSearchBtn.setPrefSize(120, 35);

    var aiProgramBtn = new Button("AI编程", FontIcon.of(FontAwesomeSolid.FILE_CODE, 16));
    aiProgramBtn.setAlignment(CENTER);
    aiProgramBtn.setPrefSize(120, 35);

    functionListVBox.getChildren().addAll(aiSearchBtn, aiProgramBtn);
    VBox.setVgrow(aiSearchBtn, Priority.NEVER);
    VBox.setVgrow(aiProgramBtn, Priority.NEVER);

    return new TitledPane(resourceBundle.getString("function.list"), functionListVBox);
  }

  private TitledPane buildConversationHistoryTitlePane() {
    var historyConversationVBox = new VBox();
    historyConversationVBox.setPadding(new Insets(5));
    historyConversationVBox.setSpacing(10);
    historyConversationVBox.setAlignment(TOP_LEFT);
    TitledPane titledPane =
        new TitledPane(resourceBundle.getString("history.conversation"), historyConversationVBox);
    titledPane
        .expandedProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (newValue) {
                var pagination = new Pagination(3, 0);
                // 设置分页内容工厂
                pagination.setPageFactory(this::createPage);

                historyConversationVBox.getChildren().add(pagination);
              }
            });
    return titledPane;
  }

  private ListView<String> createPage(int pageIndex) {
    int fromIndex = pageIndex * 5;
    int toIndex = Math.min(fromIndex + 5, 20);

    var allData = new ArrayList<String>();

    ObservableList<String> pageData =
        FXCollections.observableArrayList(allData.subList(fromIndex, toIndex));

    ListView<String> listView = new ListView<>(pageData);
    listView.setPrefHeight(300);
    return listView;
  }

  /** 配置左侧侧边栏 */
  private void configureLeftSidebar() {

    var accordion = new Accordion(buildFunctionTitledPane(), buildConversationHistoryTitlePane());
    accordion.setPadding(new Insets(5));

    VBox leftContent = new VBox(accordion);
    leftContent.setAlignment(TOP_CENTER);
    leftContent.setPadding(new Insets(5));
    leftContent.setSpacing(10);

    hiddenSidesPane.setLeft(leftContent); // 设置左侧内容

    // 监听侧边栏显示状态
    accordion.prefHeightProperty().bind(hiddenSidesPane.heightProperty());
    accordion.prefWidthProperty().bind(hiddenSidesPane.widthProperty().multiply(0.25));
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.resourceBundle = resources;
    configureLoginWindowsDragAndDrop(mainLayout);

    configureButton(
        minimizedBtn,
        BootstrapIcons.DASH_CIRCLE,
        16,
        null,
        resources.getString("minimizedBtn.tooltip"));

    configureButton(
        closeBtn, BootstrapIcons.X_CIRCLE, 16, null, resources.getString("closeBtn.tooltip"));

    configureButton(
        newConversationBtn,
        BootstrapIcons.PERSON_PLUS_FILL,
        16,
        null,
        resources.getString("newConversationBtn.tooltip"));

    // 4. 配置触发行为（默认鼠标悬停触发）
    hiddenSidesPane.setTriggerDistance(30); // 边缘触发距离（像素）
    hiddenSidesPane.setAnimationDelay(Duration.millis(150)); // 动画延迟（毫秒）

    configureLeftSidebar(); // 配置左侧面板

    // 1. 创建主内容区域
    var chatDisplayArea = new TextFlow();
    var stackPane = new StackPane(chatDisplayArea);
    stackPane.setAlignment(CENTER);
    stackPane.setPadding(new Insets(10));

    var sendMessageHBox = new HBox();
    sendMessageHBox.setSpacing(20);
    sendMessageHBox.setAlignment(CENTER);
    var messageArea = new TextArea("聊天消息");
    messageArea.setWrapText(true);
    messageArea.setPromptText("hahahahhahahahahah");
    var sendButton = new Button("发送");
    sendButton.setPrefSize(50, 35);
    sendMessageHBox.getChildren().addAll(messageArea, sendButton);

    stackPane.getChildren().add(sendMessageHBox);
    sendMessageHBox.setTranslateY(100);

    var centerContent =
        Borders.wrap(stackPane)
            .etchedBorder()
            .outerPadding(2)
            .innerPadding(1)
            .radius(2)
            .highlight(Color.DARKSEAGREEN)
            .build()
            .build();

    //    leftContent.setStyle("-fx-background-color: #81D4FA; -fx-padding: 10;");

    // 3. 创建HiddenSidesPane并添加内容
    hiddenSidesPane.setContent(centerContent); // 设置主内容

    messageArea.prefHeightProperty().bind(hiddenSidesPane.heightProperty().multiply(0.25));
    messageArea.prefWidthProperty().bind(hiddenSidesPane.widthProperty().multiply(0.7));
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
