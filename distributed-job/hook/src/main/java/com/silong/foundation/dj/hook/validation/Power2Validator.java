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

package com.silong.foundation.dj.hook.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 校验器实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-12 17:25
 */
public class Power2Validator implements ConstraintValidator<Power2, Integer> {

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext context) {
    return value != null && tableSizeFor(value) == value;
  }

  /** Returns a power of two size for the given target capacity. */
  private int tableSizeFor(int cap) {
    int maximumCapacity = Short.MAX_VALUE;
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= maximumCapacity) ? maximumCapacity : n + 1;
  }
}
