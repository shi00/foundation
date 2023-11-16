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

package com.silong.foundation.dj.mixmaster.vo;

import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.ViewId;

/**
 * Empty View
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-11 9:09
 */
@EqualsAndHashCode(callSuper = true)
public class EmptyView extends View {
  /** 初始空视图 */
  public static final EmptyView EMPTY_VIEW = new EmptyView();

  private static final Address[] EMPTY_MEMBERS = new Address[0];

  /** 默认构造方法 */
  private EmptyView() {
    this.members = EMPTY_MEMBERS;
    this.view_id = new ViewId();
  }

  @Override
  public Supplier<? extends View> create() {
    return () -> EMPTY_VIEW;
  }
}
