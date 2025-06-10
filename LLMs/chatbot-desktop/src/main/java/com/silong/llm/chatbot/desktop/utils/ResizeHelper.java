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

package com.silong.llm.chatbot.desktop.utils;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * 大小变化工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
public final class ResizeHelper {

  /** 禁止实例化 */
  private ResizeHelper() {}

  public static void addResizeListener(Stage stage) {
    ResizeListener resizeListener = new ResizeListener(stage);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_MOVED, resizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED, resizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_DRAGGED, resizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED, resizeListener);
    stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, resizeListener);
    ObservableList<Node> children = stage.getScene().getRoot().getChildrenUnmodifiable();
    for (Node child : children) {
      addListenerDeeply(child, resizeListener);
    }
  }

  public static void addListenerDeeply(Node node, EventHandler<MouseEvent> listener) {
    node.addEventHandler(MouseEvent.MOUSE_MOVED, listener);
    node.addEventHandler(MouseEvent.MOUSE_PRESSED, listener);
    node.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener);
    node.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
    node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener);
    if (node instanceof Parent parent) {
      ObservableList<Node> children = parent.getChildrenUnmodifiable();
      for (Node child : children) {
        addListenerDeeply(child, listener);
      }
    }
  }

  static class ResizeListener implements EventHandler<MouseEvent> {
    private final Stage stage;
    private Cursor cursorEvent = Cursor.DEFAULT;
    private int border = 4;
    private double startX = 0;
    private double startY = 0;

    public ResizeListener(Stage stage) {
      this.stage = stage;
    }

    @Override
    public void handle(MouseEvent mouseEvent) {
      EventType<? extends MouseEvent> mouseEventType = mouseEvent.getEventType();
      Scene scene = stage.getScene();

      double mouseEventX = mouseEvent.getSceneX(),
          mouseEventY = mouseEvent.getSceneY(),
          sceneWidth = scene.getWidth(),
          sceneHeight = scene.getHeight();

      if (MouseEvent.MOUSE_MOVED.equals(mouseEventType)) {
        if (mouseEventX < border && mouseEventY < border) {
          cursorEvent = Cursor.NW_RESIZE;
        } else if (mouseEventX < border && mouseEventY > sceneHeight - border) {
          cursorEvent = Cursor.SW_RESIZE;
        } else if (mouseEventX > sceneWidth - border && mouseEventY < border) {
          cursorEvent = Cursor.NE_RESIZE;
        } else if (mouseEventX > sceneWidth - border && mouseEventY > sceneHeight - border) {
          cursorEvent = Cursor.SE_RESIZE;
        } else if (mouseEventX < border) {
          cursorEvent = Cursor.W_RESIZE;
        } else if (mouseEventX > sceneWidth - border) {
          cursorEvent = Cursor.E_RESIZE;
        } else if (mouseEventY < border) {
          cursorEvent = Cursor.N_RESIZE;
        } else if (mouseEventY > sceneHeight - border) {
          cursorEvent = Cursor.S_RESIZE;
        } else {
          cursorEvent = Cursor.DEFAULT;
        }
        scene.setCursor(cursorEvent);
      } else if (MouseEvent.MOUSE_EXITED.equals(mouseEventType)
          || MouseEvent.MOUSE_EXITED_TARGET.equals(mouseEventType)) {
        scene.setCursor(Cursor.DEFAULT);
      } else if (MouseEvent.MOUSE_PRESSED.equals(mouseEventType)) {
        startX = stage.getWidth() - mouseEventX;
        startY = stage.getHeight() - mouseEventY;
      } else if (MouseEvent.MOUSE_DRAGGED.equals(mouseEventType)) {
        if (!Cursor.DEFAULT.equals(cursorEvent)) {
          if (!Cursor.W_RESIZE.equals(cursorEvent) && !Cursor.E_RESIZE.equals(cursorEvent)) {
            double minHeight =
                stage.getMinHeight() > (border * 2) ? stage.getMinHeight() : (border * 2);
            if (Cursor.NW_RESIZE.equals(cursorEvent)
                || Cursor.N_RESIZE.equals(cursorEvent)
                || Cursor.NE_RESIZE.equals(cursorEvent)) {
              if (stage.getHeight() > minHeight || mouseEventY < 0) {
                stage.setHeight(stage.getY() - mouseEvent.getScreenY() + stage.getHeight());
                stage.setY(mouseEvent.getScreenY());
              }
            } else {
              if (stage.getHeight() > minHeight || mouseEventY + startY - stage.getHeight() > 0) {
                stage.setHeight(mouseEventY + startY);
              }
            }
          }

          if (!Cursor.N_RESIZE.equals(cursorEvent) && !Cursor.S_RESIZE.equals(cursorEvent)) {
            double minWidth =
                stage.getMinWidth() > (border * 2) ? stage.getMinWidth() : (border * 2);
            if (Cursor.NW_RESIZE.equals(cursorEvent)
                || Cursor.W_RESIZE.equals(cursorEvent)
                || Cursor.SW_RESIZE.equals(cursorEvent)) {
              if (stage.getWidth() > minWidth || mouseEventX < 0) {
                stage.setWidth(stage.getX() - mouseEvent.getScreenX() + stage.getWidth());
                stage.setX(mouseEvent.getScreenX());
              }
            } else {
              if (stage.getWidth() > minWidth || mouseEventX + startX - stage.getWidth() > 0) {
                stage.setWidth(mouseEventX + startX);
              }
            }
          }
        }
      }
    }
  }
}
