/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jgroups.View;

import java.io.Closeable;
import java.io.Serial;
import java.io.Serializable;

/**
 * 视图变化事件
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-29 22:38
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
public class ViewChangedEvent implements Closeable, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 当前视图 */
  private View newview;

  /** 上一个视图 */
  private View oldView;

  @Override
  public void close() {
    oldView = newview = null;
  }
}
