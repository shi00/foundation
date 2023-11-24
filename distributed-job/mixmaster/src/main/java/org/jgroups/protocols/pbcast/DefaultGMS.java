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

import static java.util.Objects.requireNonNull;

import com.silong.foundation.dj.hook.SpringContext;
import com.silong.foundation.dj.hook.clock.LogicalClock;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import lombok.SneakyThrows;
import org.jgroups.*;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.MembershipChangePolicy;
import org.jgroups.util.Digest;
import org.jgroups.util.Util;
import org.springframework.util.ReflectionUtils;

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
  private final LogicalClock logicalClock;

  private final Field vi;

  /** 是否更新时钟 */
  static final ScopedValue<Boolean> IS_UPDATE_CLOCK = ScopedValue.newInstance();

  /** 默认构造方法 */
  public DefaultGMS() {
    logicalClock = SpringContext.getBean(LogicalClock.class);
    membership_change_policy = SpringContext.getBean(MembershipChangePolicy.class);
    vi = requireNonNull(ReflectionUtils.findField(View.class, "view_id", ViewId.class));
    vi.setAccessible(true);
  }

  /** 注册自定义协议 */
  public static void register() {
    ClassConfigurator.addProtocol(ID, DefaultGMS.class);
  }

  @SneakyThrows
  private void replaceDefaultViewId(View view) {
    ViewId viewId = view.getViewId();
    if (viewId.getClass() != DefaultViewId.class) {
      vi.set(view, new DefaultViewId(viewId));
    }
  }

  @Override
  public void installView(View newView, Digest digest) {
    // 默认情况需要更新时钟
    if (IS_UPDATE_CLOCK.orElse(Boolean.TRUE)) {
      logicalClock.update(newView.getViewId().getId());
    }
    replaceDefaultViewId(newView);
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
      View view = new View(new DefaultViewId(newCoord, logicalClock.tick()), mbrs);

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
