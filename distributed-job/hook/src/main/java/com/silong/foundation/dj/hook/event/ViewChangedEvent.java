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

package com.silong.foundation.dj.hook.event;

import java.io.Serial;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.jgroups.View;
import org.springframework.context.ApplicationEvent;

/**
 * 集群视图变化通知事件
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-28 15:33
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class ViewChangedEvent extends ApplicationEvent {

  @Serial private static final long serialVersionUID = -6_140_118_803_761_680_246L;

  /** 旧视图 */
  private final View oldView;

  /**
   * 构造方法
   *
   * @param oldView 旧视图
   * @param newView 当前新视图
   */
  public ViewChangedEvent(@Nullable View oldView, @NonNull View newView) {
    super(newView);
    this.oldView = oldView;
  }

  /**
   * 获取旧视图
   *
   * @return 旧视图
   */
  public View newView() {
    return (View) getSource();
  }

  @Override
  public String toString() {
    return String.format("ViewChangedEvent{newView:%s, oldView:%s}", newView(), oldView);
  }
}
