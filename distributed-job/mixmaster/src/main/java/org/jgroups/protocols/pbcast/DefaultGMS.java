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

import com.silong.foundation.dj.hook.clock.LogicalClock;
import java.util.Collection;
import java.util.List;
import lombok.Setter;
import org.jgroups.*;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.Digest;
import org.jgroups.util.Util;

/**
 * 集群管理协议
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-24 10:45
 */
public class DefaultGMS extends GMS {

  /** 协议ID */
  public static final short ID = (short) 1031;

  /** 混合逻辑时钟 */
  @Setter private LogicalClock logicalClock;

  /** 是否更新时钟 */
  static final ScopedValue<Boolean> IS_UPDATE_CLOCK = ScopedValue.newInstance();

  /** 注册自定义协议 */
  public static void register() {
    ClassConfigurator.addProtocol(ID, DefaultGMS.class);
  }

  @Override
  public void installView(View newView, Digest digest) {
    // 默认情况需要更新时钟
    if (IS_UPDATE_CLOCK.orElse(Boolean.TRUE)) {
      long id = newView.getViewId().getId();
      if (log.isDebugEnabled()) {
        log.debug("Update logic clock by %s", LogicalClock.from(id));
      }
      logicalClock.update(id);
    }
    super.installView(newView, digest);
  }

  @Override
  public View getNextView(
      Collection<Address> joiners, Collection<Address> leavers, Collection<Address> suspectedMbrs) {
    synchronized (members) {
      ViewId viewId = view != null ? view.getViewId() : null;
      if (viewId == null) {
        log.error(Util.getMessage("ViewidIsNull"));
        return null; // this should *never* happen !
      }

      List<Address> mbrs =
          computeNewMembership(tmp_members.getMembers(), joiners, leavers, suspectedMbrs);
      Address newCoord = !mbrs.isEmpty() ? mbrs.get(0) : local_addr;
      View view = new View(new ViewId(newCoord, logicalClock.tick()), mbrs);

      // Update membership (see DESIGN for explanation):
      tmp_members.set(mbrs);

      // Update joining list (see DESIGN for explanation)
      if (joiners != null) {
        joiners.stream().filter(tmp_mbr -> !joining.contains(tmp_mbr)).forEach(joining::add);
      }

      // Update leaving list (see DESIGN for explanations)
      if (leavers != null) {
        leavers.stream().filter(addr -> !leaving.contains(addr)).forEach(leaving::add);
      }

      if (suspectedMbrs != null) {
        suspectedMbrs.stream().filter(addr -> !leaving.contains(addr)).forEach(leaving::add);
      }
      return view;
    }
  }

  @Override
  public void becomeClient() {
    GmsImpl tmp =
        impls.computeIfAbsent(CLIENT, key -> new DefaultClientGmsImpl(this, logicalClock));
    try {
      tmp.init();
    } catch (Exception e) {
      log.error(Util.getMessage("ExceptionSwitchingToClientRole"), e);
    }
    setImpl(tmp);
  }
}
