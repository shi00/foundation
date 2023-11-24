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

package org.jgroups.protocols.pbcast;

import static org.jgroups.protocols.pbcast.DefaultGMS.IS_UPDATE_CLOCK;

import com.silong.foundation.dj.hook.clock.LogicalClock;
import org.jgroups.*;
import org.jgroups.util.Digest;

/**
 * gms客户端默认实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-24 14:21
 */
class DefaultClientGmsImpl extends ClientGmsImpl {

  /** 混合逻辑时钟 */
  private final LogicalClock logicalClock;

  /**
   * 构造方法
   *
   * @param gms GMS
   * @param logicalClock 逻辑时钟
   */
  public DefaultClientGmsImpl(GMS gms, LogicalClock logicalClock) {
    super(gms);
    this.logicalClock = logicalClock;
  }

  /**
   * 启用混合逻辑时钟生成viewId
   *
   * @param mbr address
   */
  void becomeSingletonMember(Address mbr) {
    // create singleton view with mbr as only member
    View newView = new View(new ViewId(mbr, logicalClock.tick()), new Address[] {mbr});

    // set the initial digest (since I'm the first member)
    Digest initialDigest = new Digest(mbr, 0, 0);

    // impl will be coordinator
    ScopedValue.runWhere(
        IS_UPDATE_CLOCK, Boolean.FALSE, () -> gms.installView(newView, initialDigest));

    Event event = new Event(Event.BECOME_SERVER);
    gms.getUpProtocol().up(event);
    gms.getDownProtocol().down(event);
    if (log.isDebugEnabled()) {
      log.debug(
          "%s: created cluster (first member). My view is %s, impl is %s",
          gms.getAddress(), gms.getViewId(), gms.getImpl().getClass().getSimpleName());
    }
  }
}
