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

package com.silong.foundation.springboot.starter.jwt;

import static com.silong.foundation.springboot.starter.jwt.JwtAuthTests.FAKER;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 测试rest接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-27 20:19
 */
@Slf4j
@RestController
@RequestMapping("/guest")
public class TestService {

  public record User(long id, String name) {

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }
      User user = (User) object;
      return id == user.id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  @GetMapping(value = "/a/{id}", consumes = ALL_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(value = OK)
  public Mono<User> queryUser(@PathVariable("id") long id) {
    return Mono.just(new User(id, FAKER.animal().name()));
  }

  @PostMapping(value = "/c", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(value = ACCEPTED)
  public Mono<Long> createUser(@RequestBody User user) {
    return Mono.just(user.id);
  }

  @PutMapping(value = "/d", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(value = ACCEPTED)
  public Mono<Long> updateUser(@RequestBody User user) {
    return Mono.just(user.id);
  }

  @DeleteMapping(value = "/b/{id}")
  @ResponseStatus(value = OK)
  public Mono<Long> deleteUser(@PathVariable("id") long id) {
    return Mono.just(id);
  }
}
