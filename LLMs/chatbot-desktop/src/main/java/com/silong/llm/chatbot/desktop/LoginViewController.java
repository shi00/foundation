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
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static javafx.scene.text.TextAlignment.CENTER;

import atlantafx.base.theme.Dracula;
import com.silong.llm.chatbot.desktop.client.AsyncRestClient;
import com.silong.llm.chatbot.desktop.utils.ResizeHelper;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.passay.*;

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

  @FXML private BorderPane mainLayout;

  @FXML private Button minimizedBtn;

  @FXML private Button closeBtn;

  @FXML private Button loginBtn;

  @FXML private Button addHostBtn;

  @FXML private Label userNameLabel;

  @FXML private Label passwordLabel;

  @FXML private Label hostLabel;

  @FXML private ComboBox<String> hostComboBox;

  @FXML private CustomTextField userNameTextField;

  @FXML private CustomPasswordField passwordTextField;

  private final ValidationSupport validation = new ValidationSupport();

  private ResourceBundle resourceBundle;

  private Path hostCnfPath;

  private ObservableList<String> hostItems;

  @FXML
  @SneakyThrows(IOException.class)
  void handleLoginAction(ActionEvent event) {
    var result = validation.getValidationResult();
    var errors = result.getErrors();
    errors.stream()
        .findFirst()
        .ifPresent(validationMessage -> showErrorDialog(validationMessage.getText()));
    if (!errors.isEmpty()) {
      return;
    }

    String userName = userNameTextField.getText();

    String password = passwordTextField.getText();

    var host = hostComboBox.getEditor().getText();

    FXMLLoader loader =
        new FXMLLoader(getClass().getResource(CONFIGURATION.chatViewPath()), resourceBundle);
    Parent parent = loader.load();
    ChatViewController controller = loader.getController();
    String[] parts = host.split(":", 2);
    controller.setRestClient(
        AsyncRestClient.create(parts[0].trim(), Integer.parseInt(parts[1].trim()), "credential"));
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

  private void showErrorDialog(String message) {
    Platform.runLater(
        () -> {
          var alert = new Alert(Alert.AlertType.ERROR);
          alert.setTitle(resourceBundle.getString("dialog.error"));
          alert.setHeaderText(null);
          alert.setContentText(message);
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
      List<String> lines = List.of("127.0.0.1:8080");
      Files.write(configPath, lines, UTF_8, CREATE, WRITE, TRUNCATE_EXISTING);
      return lines;
    }
  }

  private class DeleteButtonListCell extends ListCell<String> {
    private final HBox container;
    private final Label hostLabel;
    ;

    public DeleteButtonListCell(ListView<String> listView) {
      container = new HBox();
      container.setAlignment(CENTER_LEFT);
      container.setPadding(new Insets(2));
      container.setSpacing(2);

      var rightContainer = new HBox();
      rightContainer.setAlignment(CENTER_RIGHT);
      rightContainer.setPadding(new Insets(2));
      rightContainer.setSpacing(2);

      hostLabel = new Label();
      hostLabel.setTextAlignment(CENTER);

      container.getChildren().addAll(hostLabel, rightContainer);
      HBox.setHgrow(hostLabel, NEVER);
      HBox.setHgrow(rightContainer, ALWAYS);

      var deleteBtn = new Button();
      deleteBtn.getStyleClass().addAll("flat", "small");
      deleteBtn.setGraphic(FontIcon.of(BootstrapIcons.DASH_CIRCLE, 16));
      deleteBtn.setOnKeyPressed(
          event -> {
            if (event.getCode() == KeyCode.ENTER) {
              LoginViewController.this.handleDeleteHostBtnAction(hostLabel.getText());
            }
          });
      deleteBtn.setOnMousePressed(
          event -> LoginViewController.this.handleDeleteHostBtnAction(hostLabel.getText()));

      rightContainer.getChildren().add(deleteBtn);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
      super.updateItem(item, empty);

      if (empty || item == null) {
        setGraphic(null);
        setText(null);
      } else {
        // 设置单元格文本
        hostLabel.setText(item);

        // 仅在下拉列表中显示删除按钮
        if (getListView().getItems().contains(item)) {
          setGraphic(container);
        } else {
          setGraphic(null);
        }
      }
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.resourceBundle = resources;

    /* Drag and Drop */
    configureLoginWindowsDragAndDrop(mainLayout);

    // 创建验证支持
    configureValidation();

    configureHostComboBox();

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

    configureButton(
        addHostBtn,
        BootstrapIcons.PLUS_CIRCLE,
        64,
        null,
        resources.getString("addHostBtn.tooltip"));

    hostLabel.setGraphic(FontIcon.of(FontAwesomeSolid.SERVER, 64));

    userNameLabel.setGraphic(FontIcon.of(BootstrapIcons.PERSON_BOUNDING_BOX, 64));

    passwordLabel.setGraphic(FontIcon.of(FontAwesomeSolid.KEY, 64));

    int numberOfSquares = 20;
    while (numberOfSquares > 0) {
      generateAnimation();
      numberOfSquares--;
    }
  }

  private void configureHostComboBox() {
    hostCnfPath = appHome.resolve(HOST_CNF);
    hostItems = observableArrayList(loadHosts(hostCnfPath));

    // 创建一个ObservableList作为数据源
    hostComboBox.setItems(hostItems);

    // 添加自动完成支持
    //    AutoCompletionBinding<String> autoCompletionBinding =
    //        TextFields.bindAutoCompletion(hostComboBox.getEditor(), hostComboBox.getItems());
    //    autoCompletionBinding.setDelay((long) Duration.millis(100).toMillis());
    hostComboBox.setCellFactory(DeleteButtonListCell::new);
  }

  private PasswordValidator buildPasswordValidator() {
    List<Rule> rules = new ArrayList<>();
    rules.add(new LengthRule(8, 16)); // 长度8-16
    rules.add(
        new CharacterCharacteristicsRule(
            3,
            new CharacterRule(EnglishCharacterData.Alphabetical, 1), // 字母
            new CharacterRule(EnglishCharacterData.Digit, 1), // 数字
            new CharacterRule(EnglishCharacterData.Special, 1) // 符号
            ));
    rules.add(new WhitespaceRule()); // 禁止空格

    return new PasswordValidator(rules);
  }

  private void configureValidation() {
    validation.registerValidator(
        userNameTextField,
        true,
        Validator.createEmptyValidator(resourceBundle.getString("input.username.error")));

    PasswordValidator passwordValidator = buildPasswordValidator();
    var passwordErrorMsg = resourceBundle.getString("input.password.error");
    validation.registerValidator(
        passwordTextField,
        true,
        Validator.combine(
            Validator.createEmptyValidator(passwordErrorMsg),
            Validator.<String>createPredicateValidator(
                p -> passwordValidator.validate(new PasswordData(p)).isValid(), passwordErrorMsg)));

    String hostErrorMsg = resourceBundle.getString("input.host.error");
    validation.registerValidator(
        hostComboBox,
        true,
        Validator.combine(
            Validator.createEmptyValidator(hostErrorMsg),
            Validator.createPredicateValidator(LoginViewController::isValidHost, hostErrorMsg)));
  }

  private static boolean isValidHost(String s) {
    if (s == null || s.isEmpty()) {
      return true;
    }
    try {
      String[] split = s.split(":", 2);
      String host = split[0].trim();
      int port = Integer.parseInt(split[1].trim());
      if (port < 0 || port > 65535) {
        throw new IllegalArgumentException("Invalid port: " + port);
      }
      return InetAddressValidator.getInstance().isValid(host);
    } catch (Exception e) {
      log.error("Malformed URL: {}", s, e);
      return false;
    }
  }

  @SneakyThrows(IOException.class)
  void handleDeleteHostBtnAction(String text) {
    if (text != null && !text.isEmpty() && hostItems.contains(text)) {
      hostItems.remove(text);
      hostComboBox.hide();
      hostComboBox.setValue(null);
      Files.write(hostCnfPath, hostItems, UTF_8, CREATE, TRUNCATE_EXISTING);
      log.info("Deleted host: {}", text);
    }
  }

  @FXML
  @SneakyThrows(IOException.class)
  void handleAddHostBtnAction(ActionEvent event) {
    String text = hostComboBox.getEditor().getText();
    if (text != null && !text.isEmpty() && !hostItems.contains(text)) {

      if (!isValidHost(text)) {
        showErrorDialog(resourceBundle.getString("input.addHostBtn.error"));
        hostComboBox.setValue(null);
        return;
      }

      hostItems.addFirst(text);
      hostComboBox.setValue(text);
      Files.write(hostCnfPath, hostItems, UTF_8, TRUNCATE_EXISTING, CREATE);
      log.info("Added host: {}", text);
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
