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

import static com.silong.llm.chatbot.desktop.ChatbotDesktopApplication.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.KeyCode.TAB;

import atlantafx.base.theme.Dracula;
import com.silong.llm.chatbot.desktop.client.AsyncRestClient;
import com.silong.llm.chatbot.desktop.utils.ResizeHelper;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
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
public class LoginViewController extends ViewController implements Initializable {

  private static final String HOST_CNF = "host.cnf";

  private Path hostCnfPath;

  private ObservableList<String> hostItems;

  @FXML private BorderPane mainLayout;

  @FXML private Button minimizedBtn;

  @FXML private Button closeBtn;

  @FXML private Button loginBtn;

  @FXML private Button addHostBtn;

  @FXML private Button deleteHostBtn;

  @FXML private Label userNameLabel;

  @FXML private Label passwordLabel;

  @FXML private Label hostLabel;

  @FXML private ComboBox<String> hostComboBox;

  @FXML private CustomTextField userNameTextField;

  @FXML private CustomPasswordField passwordTextField;

  private ResourceBundle resourceBundle;

  @FXML
  @SneakyThrows(IOException.class)
  void handleLoginAction(ActionEvent event) {
    String userName = userNameTextField.getText();
    if (userName == null || (userName = userName.trim()).isEmpty()) {
      showErrorDialog("input.username.error");
      userNameTextField.clear();
      return;
    }

    String password = passwordTextField.getText();
    if (password == null || (password = password.trim()).isEmpty()) {
      showErrorDialog("input.password.error");
      passwordTextField.clear();
      return;
    }

    var host = hostComboBox.getEditor().getSelectedText();
    if (host == null || (host = host.trim()).isEmpty()) {
      showErrorDialog("input.host.error");
      hostComboBox.setValue(null);
      return;
    }

    FXMLLoader loader =
        new FXMLLoader(getClass().getResource(CONFIGURATION.chatViewPath()), resourceBundle);
    Parent parent = loader.load();
    ChatViewController controller = loader.getController();
    String[] parts = host.split(":", 2);
    controller.setRestClient(
        AsyncRestClient.create(parts[0], Integer.parseInt(parts[1]), "credential"));
    var scene = new Scene(parent);
    scene.getStylesheets().add(new Dracula().getUserAgentStylesheet());
    var stage = (Stage) primaryStage.getScene().getWindow();
    stage.setResizable(true);
    stage.setWidth(CONFIGURATION.chatWindowSize().width());
    stage.setHeight(CONFIGURATION.chatWindowSize().height());
    stage.setMinWidth(CONFIGURATION.chatWindowSize().width() / 2.0);
    stage.setMinHeight(CONFIGURATION.chatWindowSize().height() / 2.0);
    stage.setOnCloseRequest(
        (WindowEvent e) -> {
          Platform.exit();
          System.exit(0);
        });
    stage.setScene(scene);
    ResizeHelper.addResizeListener(stage);
    stage.centerOnScreen();
  }

  private void showErrorDialog(String messageKey) {
    Platform.runLater(
        () -> {
          var alert = new Alert(Alert.AlertType.ERROR);
          alert.setTitle(resourceBundle.getString("dialog.error"));
          alert.setHeaderText(null);
          alert.setContentText(resourceBundle.getString(messageKey));
          alert.initOwner(primaryStage.getScene().getWindow());
          alert.showAndWait();
        });
  }

  @FXML
  void handleCloseAction(ActionEvent event) {
    exit();
  }

  @FXML
  void handleMinimizedAction(ActionEvent event) {
    minimize(primaryStage);
  }

  @SneakyThrows(IOException.class)
  private static List<String> loadHosts(Path configPath) {
    if (Files.exists(configPath)) {
      return Files.readAllLines(appHome.resolve(HOST_CNF));
    } else {
      List<String> lines = List.of("127.0.0.1:8080", "192.168.1.100:8080");
      Files.write(configPath, lines, UTF_8, CREATE, WRITE, TRUNCATE_EXISTING);
      return lines;
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.resourceBundle = resources;

    hostCnfPath = appHome.resolve(HOST_CNF);
    hostItems = observableArrayList(loadHosts(hostCnfPath));

    /* Drag and Drop */
    configureLoginWindowsDragAndDrop(mainLayout);

    // 创建一个ObservableList作为数据源
    hostComboBox.setItems(hostItems);
    // 添加自动完成支持
    TextFields.bindAutoCompletion(hostComboBox.getEditor(), hostComboBox.getItems());

    // 初始焦点
    primaryStage.setOnShown(e -> userNameTextField.requestFocus());

    // 配置焦点转移顺序
    configureFocusTraversalOrder();

    configureButton(
        minimizedBtn,
        BootstrapIcons.DASH_CIRCLE,
        16,
        null,
        resources.getString("minimizedBtn.tooltip"));

    configureButton(
        closeBtn, BootstrapIcons.X_CIRCLE, 16, null, resources.getString("closeBtn.tooltip"));

    hostLabel.setGraphic(FontIcon.of(FontAwesomeSolid.SERVER, 64));

    userNameLabel.setGraphic(FontIcon.of(BootstrapIcons.PERSON_BOUNDING_BOX, 64));

    passwordLabel.setGraphic(FontIcon.of(FontAwesomeSolid.KEY, 64));

    addHostBtn.setGraphic(FontIcon.of(FontAwesomeSolid.PLUS_CIRCLE, 64));

    deleteHostBtn.setGraphic(FontIcon.of(BootstrapIcons.DASH_CIRCLE, 64));

    int numberOfSquares = 20;
    while (numberOfSquares > 0) {
      generateAnimation();
      numberOfSquares--;
    }
  }

  @FXML
  @SneakyThrows(IOException.class)
  void handleAddHostBtnAction(ActionEvent event) {
    String text = hostComboBox.getEditor().getText();
    if (text != null && !text.isEmpty() && !hostItems.contains(text)) {
      hostItems.addFirst(text);
      hostComboBox.setValue(text);
      Files.write(hostCnfPath, hostItems, UTF_8, TRUNCATE_EXISTING, CREATE);
    }
  }

  @FXML
  @SneakyThrows(IOException.class)
  void handleDeleteHostBtnAction(ActionEvent event) {
    String text = hostComboBox.getEditor().getText();
    if (text != null && !text.isEmpty() && hostItems.contains(text)) {
      hostItems.remove(text);
      hostComboBox.setValue(hostItems.getFirst());
      Files.write(hostCnfPath, hostItems, UTF_8, CREATE, TRUNCATE_EXISTING);
    }
  }

  /** 配置焦点转移顺序 */
  private void configureFocusTraversalOrder() {
    Node[] nodes = {
      userNameTextField,
      passwordTextField,
      hostComboBox,
      addHostBtn,
      loginBtn,
      minimizedBtn,
      closeBtn
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
    int sizeOfShape = rand.nextInt(50) + 1;
    int speedOfShape = rand.nextInt(10) + 5;
    int startXPoint = rand.nextInt(heightBound);
    int startYPoint = rand.nextInt(widthBound);
    Direction direction = Direction.values()[rand.nextInt(Direction.values().length)];

    KeyValue moveXAxis = null;
    KeyValue moveYAxis = null;
    Shape shape = null;

    switch (direction) {
      case LEFT2RIGHT:
        // MOVE LEFT TO RIGHT
        if (rand.nextInt() % 2 == 0) {
          var rectangle = new Rectangle(0, startYPoint, sizeOfShape, sizeOfShape);
          moveXAxis = new KeyValue(rectangle.xProperty(), widthBound - sizeOfShape);
          shape = rectangle;
        } else {
          var circle = new Circle(startXPoint, startYPoint, sizeOfShape);
          moveXAxis = new KeyValue(circle.centerXProperty(), widthBound - sizeOfShape);
          shape = circle;
        }
        break;
      case TOP2BOTTOM:
        // MOVE TOP TO BOTTOM
        if (rand.nextInt() % 2 == 0) {
          var rectangle = new Rectangle(startXPoint, 0, sizeOfShape, sizeOfShape);
          moveYAxis = new KeyValue(rectangle.yProperty(), heightBound - sizeOfShape);
          shape = rectangle;
        } else {
          var circle = new Circle(startXPoint, startYPoint, sizeOfShape);
          moveYAxis = new KeyValue(circle.centerYProperty(), heightBound - sizeOfShape);
          shape = circle;
        }
        break;

      case BOTTOM2TOP:
        // MOVE BOTTOM TO TOP
        if (rand.nextInt() % 2 == 0) {
          var rectangle =
              new Rectangle(startXPoint, heightBound - sizeOfShape, sizeOfShape, sizeOfShape);
          moveYAxis = new KeyValue(rectangle.xProperty(), 0);
          shape = rectangle;
        } else {
          var circle = new Circle(startXPoint, startYPoint, sizeOfShape);
          moveYAxis = new KeyValue(circle.centerXProperty(), 0);
          shape = circle;
        }
        break;
      case RIGHT2LEFT:
        // MOVE RIGHT TO LEFT
        if (rand.nextInt() % 2 == 0) {
          var rectangle =
              new Rectangle(heightBound - sizeOfShape, startYPoint, sizeOfShape, sizeOfShape);
          moveXAxis = new KeyValue(rectangle.xProperty(), 0);
          shape = rectangle;
        } else {
          var circle = new Circle(startXPoint, startYPoint, sizeOfShape);
          moveXAxis = new KeyValue(circle.centerXProperty(), 0);
          shape = circle;
        }
        break;
      case LEFT2RIGHT_TOP2BOTTOM:
      case RIGHT2LEFT_BOTTOM2TOP:
        // MOVE RIGHT TO LEFT, BOTTOM TO TOP
        if (rand.nextInt() % 2 == 0) {
          var rectangle = new Rectangle(startXPoint, 0, sizeOfShape, sizeOfShape);
          moveXAxis = new KeyValue(rectangle.xProperty(), widthBound - sizeOfShape);
          moveYAxis = new KeyValue(rectangle.yProperty(), heightBound - sizeOfShape);
          shape = rectangle;
        } else {
          var circle = new Circle(startXPoint, startYPoint, sizeOfShape);
          moveXAxis = new KeyValue(circle.centerXProperty(), widthBound - sizeOfShape);
          moveYAxis = new KeyValue(circle.centerYProperty(), heightBound - sizeOfShape);
          shape = circle;
        }
        break;
    }

    shape.setFill(Color.CHARTREUSE);
    shape.setOpacity(0.1);
    shape.setFocusTraversable(false);
    shape.setMouseTransparent(true);

    KeyFrame keyFrame = new KeyFrame(Duration.millis(speedOfShape * 1000), moveXAxis, moveYAxis);
    Timeline timeline = new Timeline();
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.setAutoReverse(true);
    timeline.getKeyFrames().add(keyFrame);
    timeline.play();
    mainLayout.getChildren().add(mainLayout.getChildren().size() - 1, shape);
  }
}
