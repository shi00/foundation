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
package com.silong.foundation.devastator.model;

import com.silong.foundation.devastator.ClusterNode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;

/**
 * 节点权重二元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-30 09:35
 */
public record WeightNodeTuple(long weight, ClusterNode node) implements Comparable<WeightNodeTuple>, Serializable {
    @Serial
    private static final long serialVersionUID = 0L;

    /**
     * 节点权重比较器
     *
     * @author louis sin
     * @version 1.0.0
     * @since 2022-04-30 09:35
     */
    public static class WeightNodeTupleComparator
            implements Comparator<WeightNodeTuple>, Serializable {

        @Serial private static final long serialVersionUID = 0L;

        public static final WeightNodeTupleComparator COMPARATOR = new WeightNodeTupleComparator();

        /** forbidden */
        private WeightNodeTupleComparator() {}

        /** {@inheritDoc} */
        @Override
        public int compare(WeightNodeTuple o1, WeightNodeTuple o2) {
            return o1.weight < o2.weight
                    ? -1
                    : o1.weight > o2.weight ? 1 : o1.node.uuid().compareTo(o2.node.uuid());
        }
    }

    @Override
    public int compareTo(WeightNodeTuple o) {
        return WeightNodeTupleComparator.COMPARATOR.compare(this, o);
    }
}
