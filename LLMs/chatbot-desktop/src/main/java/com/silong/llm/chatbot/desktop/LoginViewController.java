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

import static com.silong.llm.chatbot.desktop.ChatbotDesktopApplication.CONFIGURATION;
import static com.silong.llm.chatbot.desktop.ChatbotDesktopApplication.primaryStage;
import static javafx.scene.Cursor.CLOSED_HAND;
import static javafx.scene.Cursor.DEFAULT;
import static javafx.scene.input.KeyCode.TAB;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
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

    // 初始焦点
    primaryStage.setOnShown(e -> hostTextField.requestFocus());

    // 配置焦点转移顺序
    configureFocusTraversalOrder();

    // 图标大小 16px
    minimizedBtn.setGraphic(FontIcon.of(BootstrapIcons.DASH_CIRCLE, 16));

    // 图标大小 16px
    closeBtn.setGraphic(FontIcon.of(BootstrapIcons.X_CIRCLE, 16));

    hostLabel.setGraphic(FontIcon.of(BootstrapIcons.CLOUD_ARROW_UP, 64));

    portLabel.setGraphic(FontIcon.of(FontAwesomeSolid.ETHERNET, 64));

    credentialsLabel.setGraphic(FontIcon.of(FontAwesomeSolid.KEY, 64));

    int numberOfSquares = 20;
    while (numberOfSquares > 0) {
      generateAnimation();
      numberOfSquares--;
    }
  }

  /** 配置焦点转移顺序 */
  private void configureFocusTraversalOrder() {
    Node[] nodes = {
      hostTextField, portTextField, credentialsTextField, loginBtn, minimizedBtn, closeBtn
    };

    for (int i = 0; i < nodes.length; i++) {
      int index = i;
      nodes[index].setOnKeyPressed(
          e -> {
            if (e.getCode() == TAB && !e.isShiftDown()) {
              e.consume();
              nodes[(index + 1) % nodes.length].requestFocus();
            }
          });
    }
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

  private enum Direction {
    LEFT2RIGHT,
    TOP2BOTTOM,
    LEFT2RIGHT_TOP2BOTTOM,
    BOTTOM2TOP,
    RIGHT2LEFT,
    RIGHT2LEFT_BOTTOM2TOP
  }

  /* This method is used to generate the animation on the login window, It will generate random ints to determine
   * the size, speed, starting points and direction of each square.
   */
  public void generateAnimation() {
    int heightBound = CONFIGURATION.loginWindowSize().height();
    int widthBound = CONFIGURATION.loginWindowSize().width();
    SecureRandom rand = new SecureRandom();
    int sizeOfSqaure = rand.nextInt(50) + 1;
    int speedOfSqaure = rand.nextInt(10) + 5;
    int startXPoint = rand.nextInt(heightBound);
    int startYPoint = rand.nextInt(widthBound);
    Direction direction = Direction.values()[rand.nextInt(Direction.values().length)];

    KeyValue moveXAxis = null;
    KeyValue moveYAxis = null;
    Rectangle r1 = null;

    switch (direction) {
      case LEFT2RIGHT:
        // MOVE LEFT TO RIGHT
        r1 = new Rectangle(0, startYPoint, sizeOfSqaure, sizeOfSqaure);
        moveXAxis = new KeyValue(r1.xProperty(), widthBound - sizeOfSqaure);
        break;
      case TOP2BOTTOM:
        // MOVE TOP TO BOTTOM
        r1 = new Rectangle(startXPoint, 0, sizeOfSqaure, sizeOfSqaure);
        moveYAxis = new KeyValue(r1.yProperty(), heightBound - sizeOfSqaure);
        break;
      case LEFT2RIGHT_TOP2BOTTOM:
        // MOVE LEFT TO RIGHT, TOP TO BOTTOM
        r1 = new Rectangle(startXPoint, 0, sizeOfSqaure, sizeOfSqaure);
        moveXAxis = new KeyValue(r1.xProperty(), widthBound - sizeOfSqaure);
        moveYAxis = new KeyValue(r1.yProperty(), heightBound - sizeOfSqaure);
        break;
      case BOTTOM2TOP:
        // MOVE BOTTOM TO TOP
        r1 = new Rectangle(startXPoint, heightBound - sizeOfSqaure, sizeOfSqaure, sizeOfSqaure);
        moveYAxis = new KeyValue(r1.xProperty(), 0);
        break;
      case RIGHT2LEFT:
        // MOVE RIGHT TO LEFT
        r1 = new Rectangle(heightBound - sizeOfSqaure, startYPoint, sizeOfSqaure, sizeOfSqaure);
        moveXAxis = new KeyValue(r1.xProperty(), 0);
        break;
      case RIGHT2LEFT_BOTTOM2TOP:
        // MOVE RIGHT TO LEFT, BOTTOM TO TOP
        r1 = new Rectangle(startXPoint, 0, sizeOfSqaure, sizeOfSqaure);
        moveXAxis = new KeyValue(r1.xProperty(), widthBound - sizeOfSqaure);
        moveYAxis = new KeyValue(r1.yProperty(), heightBound - sizeOfSqaure);
        break;
    }

    r1.setFill(Color.CHARTREUSE);
    r1.setOpacity(0.1);

    KeyFrame keyFrame = new KeyFrame(Duration.millis(speedOfSqaure * 1000), moveXAxis, moveYAxis);
    Timeline timeline = new Timeline();
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.setAutoReverse(true);
    timeline.getKeyFrames().add(keyFrame);
    timeline.play();
    mainLayout.getChildren().add(mainLayout.getChildren().size() - 1, r1);
  }
}
