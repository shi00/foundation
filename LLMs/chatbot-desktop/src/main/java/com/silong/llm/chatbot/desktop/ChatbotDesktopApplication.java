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

import static javafx.scene.layout.BorderStrokeStyle.SOLID;
import static javafx.scene.paint.Color.GOLDENROD;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.llm.chatbot.desktop.config.DesktopConfig;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * 聊天助手应用入口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-27 19:15
 */
public class ChatbotDesktopApplication extends Application {

  private static final String CONFIG = "/config.json";

  private final DesktopConfig desktopConfig = loadDesktopConfig();
  private VBox chatContainer;
  private ScrollPane scrollPane;
  private TextArea messageInput;
  private Button sendButton;

  @SneakyThrows
  private DesktopConfig loadDesktopConfig() {
    return new ObjectMapper()
        .readValue(getClass().getResourceAsStream(CONFIG), DesktopConfig.class);
  }

  /**
   * 加载classpath中的图片资源
   *
   * @param path 图片资源路径
   * @return 图片
   */
  private Image loadImage(@NonNull String path) {
    return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
  }

  @Override
  public void start(Stage primaryStage) {

    // 创建主界面
    primaryStage.setTitle(desktopConfig.getTitle());
    primaryStage.getIcons().add(loadImage(desktopConfig.getIcon()));
    primaryStage.setWidth(desktopConfig.getWidth());
    primaryStage.setHeight(desktopConfig.getHeight());
    primaryStage.setResizable(true);

    var left = new VBox(10);
    left.setAlignment(Pos.TOP_CENTER);
    left.setPadding(new Insets(20));
    left.setBorder(
        new Border(new BorderStroke(GOLDENROD, SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    left.setMinWidth(desktopConfig.getWidth() * 0.3);

    // 创建聊天区域
    chatContainer = new VBox(15);
    chatContainer.setPadding(new Insets(20));
    chatContainer.setPrefWidth(680);

    // 创建滚动面板
    scrollPane = new ScrollPane(chatContainer);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefHeight(580);
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
    scrollPane.setStyle("-fx-background-color: transparent;");

    // 创建输入区域
    messageInput = new TextArea();
    messageInput.setPromptText("请输入问题...");
    messageInput.setPrefRowCount(3);
    messageInput.setWrapText(true);
    messageInput.setPrefWidth(550);
    messageInput.setFont(Font.font("Microsoft YaHei", 14));

    // 发送按钮
    sendButton = new Button("发送");
    sendButton.setPrefSize(100, 60);
    sendButton.setStyle(
        "-fx-background-color: #1677FF; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
    sendButton.setOnAction(e -> sendMessage());

    // 输入区域布局
    HBox inputBox = new HBox(10);
    inputBox.getChildren().addAll(messageInput, sendButton);
    inputBox.setPadding(new Insets(10));
    inputBox.setAlignment(Pos.CENTER);

    // 主布局
    VBox right = new VBox(10);
    right.getChildren().addAll(scrollPane, inputBox);
    right.setPadding(new Insets(20));
    right.setBorder(
        new Border(new BorderStroke(GOLDENROD, SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    right.setMinWidth(desktopConfig.getWidth() * 0.7);

    SplitPane mainLayout = new SplitPane(left, right);
    mainLayout.setPadding(new Insets(10));
    mainLayout.setDividerPositions(0.3);

    // 设置场景
    Scene scene = new Scene(mainLayout);
    scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
    primaryStage.setScene(scene);
    primaryStage.show();

    // 添加欢迎消息
    addBotMessage("你好！我是豆包对话助手，有什么可以帮助你的吗？");
  }

  private void sendMessage() {
    String userMessage = messageInput.getText().trim();
    if (!userMessage.isEmpty()) {
      // 添加用户消息到聊天区域
      addUserMessage(userMessage);

      // 清空输入框
      messageInput.clear();

      // 模拟豆包思考
      simulateTyping();

      // 延迟回复，模拟思考时间
      new Thread(
              () -> {
                try {
                  Thread.sleep(800 + new SecureRandom().nextInt(1200));
                  javafx.application.Platform.runLater(
                      () -> {
                        // 移除"正在输入"提示
                        if (chatContainer.getChildren().size() > 0
                            && chatContainer
                                    .getChildren()
                                    .get(chatContainer.getChildren().size() - 1)
                                instanceof HBox) {
                          HBox typingBox =
                              (HBox)
                                  chatContainer
                                      .getChildren()
                                      .get(chatContainer.getChildren().size() - 1);
                          if (typingBox.getUserData() != null
                              && typingBox.getUserData().equals("typing")) {
                            chatContainer.getChildren().remove(typingBox);
                          }
                        }

                        // 根据用户消息生成回复
                        addBotMessage("sdasdasdadasd");
                      });
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              })
          .start();
    }
  }

  private void addUserMessage(String message) {
    // 创建用户头像
    Image userAvatar = loadImage(desktopConfig.getAvatar());
    ImageView userAvatarView = new ImageView(userAvatar);
    userAvatarView.setFitHeight(40);
    userAvatarView.setFitWidth(40);

    // 创建消息文本
    Text userText = new Text(message);
    userText.setFont(Font.font("Microsoft YaHei", 14));
    TextFlow userTextFlow = new TextFlow(userText);
    userTextFlow.setPadding(new Insets(10));
    userTextFlow.setStyle("-fx-background-color: #E6F7FF; -fx-background-radius: 10px;");

    // 创建时间戳
    Text timestamp = new Text(getCurrentTime());
    timestamp.setFont(Font.font("Microsoft YaHei", 10));
    timestamp.setStyle("-fx-fill: #999999;");

    // 创建消息布局
    VBox messageLayout = new VBox(5);
    messageLayout.getChildren().addAll(userTextFlow, timestamp);

    // 创建用户消息HBox
    HBox userMessageHBox = new HBox(10);
    userMessageHBox.getChildren().addAll(messageLayout, userAvatarView);
    userMessageHBox.setAlignment(Pos.CENTER_RIGHT);

    // 添加到聊天容器
    chatContainer.getChildren().add(userMessageHBox);

    // 滚动到底部
    scrollToBottom();
  }

  private void addBotMessage(String message) {
    // 创建豆包头像
    Image botAvatar = new Image(getClass().getClassLoader().getResourceAsStream("icon.png"));
    ImageView botAvatarView = new ImageView(botAvatar);
    botAvatarView.setFitHeight(40);
    botAvatarView.setFitWidth(40);

    // 创建消息文本
    Text botText = new Text(message);
    botText.setFont(Font.font("Microsoft YaHei", 14));
    TextFlow botTextFlow = new TextFlow(botText);
    botTextFlow.setPadding(new Insets(10));
    botTextFlow.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 10px;");

    // 创建时间戳
    Text timestamp = new Text(getCurrentTime());
    timestamp.setFont(Font.font("Microsoft YaHei", 10));
    timestamp.setStyle("-fx-fill: #999999;");

    // 创建消息布局
    VBox messageLayout = new VBox(5);
    messageLayout.getChildren().addAll(botTextFlow, timestamp);

    // 创建豆包消息HBox
    HBox botMessageHBox = new HBox(10);
    botMessageHBox.getChildren().addAll(botAvatarView, messageLayout);
    botMessageHBox.setAlignment(Pos.CENTER_LEFT);

    // 添加到聊天容器
    chatContainer.getChildren().add(botMessageHBox);

    // 滚动到底部
    scrollToBottom();
  }

  private void simulateTyping() {
    // 创建豆包头像
    Image botAvatar = new Image(getClass().getResourceAsStream("/doubao_avatar.png"));
    ImageView botAvatarView = new ImageView(botAvatar);
    botAvatarView.setFitHeight(40);
    botAvatarView.setFitWidth(40);

    // 创建"正在输入"提示
    Text typingText = new Text("豆包正在输入...");
    typingText.setFont(Font.font("Microsoft YaHei", 14));
    TextFlow typingTextFlow = new TextFlow(typingText);
    typingTextFlow.setPadding(new Insets(10));
    typingTextFlow.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 10px;");

    // 创建"正在输入"布局
    HBox typingHBox = new HBox(10);
    typingHBox.getChildren().addAll(botAvatarView, typingTextFlow);
    typingHBox.setAlignment(Pos.CENTER_LEFT);
    typingHBox.setUserData("typing");

    // 添加到聊天容器
    chatContainer.getChildren().add(typingHBox);

    // 滚动到底部
    scrollToBottom();
  }

  private String getCurrentTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
  }

  private void scrollToBottom() {
    // 延迟滚动到底部，确保UI更新完成
    javafx.application.Platform.runLater(
        () -> {
          scrollPane.setVvalue(1.0);
        });
  }

  public static void main(String[] args) {
    launch(args);
  }
}
