/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.statiq.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.statiq.runtime.StatiqPage;
import io.quarkiverse.statiq.runtime.StatiqPages;

@Path("/statiq")
@ApplicationScoped
public class StatiqResource {
    // add some rest methods here

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@QueryParam("name") String name) {
        return "Hello " + name;
    }

    @Produces
    @Singleton
    @Transactional
    StatiqPages produce() {
        return new StatiqPages(List.of(
                StatiqPage.builder().html("/statiq?name=foo-html").build(),
                StatiqPage.builder().path("/statiq?name=foo").build(),
                StatiqPage.builder().path("/statiq?name=bar").build()));
    }

}
