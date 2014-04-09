/* ========================================================================== *
 * Copyright 2014 USRZ.com and Pier Paolo Fumagalli                           *
 * -------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *  http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 * ========================================================================== */
package org.usrz.libs.httpd;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.utils.Check;

@Path("/")
public class TestResource {

    private final Configurations conf;
    private Map<String, Integer> map;

    @Inject
    private TestResource(Configurations configurations) {
        conf = Check.notNull(configurations, "Null configurations");
    }

    @Inject
    public void setDependency(@Named("foobar") Map<String, Integer> map) {
        this.map = Check.notNull(map, "Null map");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        return Response.ok(new Bean()).build();
    }

    public class Bean {
        public Date getEpochDate() { return new Date(0); }
        public Map<String, Integer> getDependedMap() { return map; }
        public String getConfig() { return conf.get("conf"); }
    }
}
