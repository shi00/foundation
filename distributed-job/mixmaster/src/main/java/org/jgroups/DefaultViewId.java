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

package org.jgroups;

import com.silong.foundation.dj.hook.clock.LogicalClock;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 实现按照逻辑时钟排序的ViewId
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-24 19:37
 */
@NoArgsConstructor
public class DefaultViewId extends ViewId {

  /**
   * 构造方法
   *
   * @param viewId viewId
   */
  public DefaultViewId(@NonNull ViewId viewId) {
    this.creator = viewId.creator;
    this.id = viewId.id;
  }

  /**
   * Creates a ViewID with the coordinator address and a Lamport timestamp of 0.
   *
   * @param creator the address of the member that issued this view
   */
  public DefaultViewId(@NonNull Address creator) {
    this.creator = creator;
  }

  /**
   * Creates a ViewID with the coordinator address and the given Lamport timestamp.
   *
   * @param creator - the address of the member that issued this view
   * @param id - the Lamport timestamp of the view
   */
  public DefaultViewId(@NonNull Address creator, long id) {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be greater than 0.");
    }
    this.creator = creator;
    this.id = id;
  }

  @Override
  public int compareTo(ViewId other) {
    int ret;
    return (ret = compareToIDs(other)) == 0 ? creator.compareTo(other.creator) : ret;
  }

  @Override
  public int compareToIDs(ViewId other) {
    return LogicalClock.compare(id, other.id);
  }

  @Override
  public Supplier<? extends ViewId> create() {
    return DefaultViewId::new;
  }

  @Override
  public DefaultViewId copy() {
    return new DefaultViewId(creator, id);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof DefaultViewId v) {
      return id == v.id && Objects.equals(creator, v.creator);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, creator);
  }
}
