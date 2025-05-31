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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录界面控制器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Slf4j
public class LoginViewController implements Initializable {

  @FXML private Button closeBtn;

  @FXML private TextField credentialTextField;

  @FXML private TextField hostTextField;

  @FXML private Button loginBtn;

  @FXML private Button minimizedBtn;

  @FXML private TextField portTextField;

  private ResourceBundle resourceBundle;

  @FXML
  void closeLoginWindow(ActionEvent event) {
    primaryStage.close();
  }

  @FXML
  void handleLogin(ActionEvent event) {
    var host = hostTextField.getText();
    if (host == null || (host = host.trim()).isEmpty()) {
      showErrorDialog("input.host.error");
      hostTextField.clear();
    }

    var port = Integer.parseInt(portTextField.getText());
    var credential = credentialTextField.getText();
  }

  @FXML
  void minimizeLoginWindow(ActionEvent event) {
    primaryStage.setIconified(true);
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    log.debug("Initializing url:{}, resourceBundle:{}", url, resourceBundle);
    this.resourceBundle = resourceBundle;
  }

  private void showErrorDialog(String messageKey) {
    Platform.runLater(
        () -> {
          Alert alert = new Alert(Alert.AlertType.ERROR);
          alert.setTitle(resourceBundle.getString("dialog.error"));
          alert.setHeaderText(resourceBundle.getString("input.error"));
          alert.setContentText(resourceBundle.getString(messageKey));
          alert.showAndWait();
        });
  }
}
