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

import com.silong.llm.chatbot.desktop.client.AsyncRestClient;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天界面控制器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Slf4j
public class ChatViewController implements Initializable {

  @FXML private Button foldButton;

  @FXML private VBox leftVBox;

  @FXML private VBox rightVBox;

  @FXML private SplitPane mainLayout;

  @Setter private AsyncRestClient restClient;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // 禁用所有分割线的拖动
    //    for (SplitPane.Divider divider : mainLayout.getDividers()) {
    //      divider
    //          .positionProperty()
    //          .addListener(
    //              (observable, oldValue, newValue) -> {
    //                // 阻止分割线位置改变
    //                divider.setPosition(oldValue.doubleValue());
    //              });
    //    }

    // 可选：阻止鼠标事件传播到分割线
    //    mainLayout.addEventFilter(MouseEvent.MOUSE_PRESSED, Event::consume);

    // 创建Tooltip并设置属性
    Tooltip tooltip = new Tooltip();
    tooltip.setTextAlignment(TextAlignment.CENTER);
    tooltip.setText(resources.getString("foldbtn.tooltip"));
    tooltip.setShowDelay(Duration.millis(100)); // 鼠标悬停200ms后显示
    tooltip.setHideDelay(Duration.millis(100)); // 鼠标移开1000ms后隐藏
    tooltip.setShowDuration(Duration.INDEFINITE); // 永久显示，直到鼠标移开
    foldButton.setTooltip(tooltip);
  }
}
