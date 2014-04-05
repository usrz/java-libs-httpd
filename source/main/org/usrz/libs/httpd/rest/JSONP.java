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
package org.usrz.libs.httpd.rest;

import java.util.Objects;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class JSONP {

    public static final String JSONP = "application/javascript";
    public static final MediaType JSONP_TYPE = MediaType.valueOf(JSONP);

    private final String callback;
    private final Object entity;
    private final int status;

    public JSONP(String callback, Object entity) {
        this(callback, Response.ok(entity).build());
    }

    public JSONP(String callback, int status, Object entity) {
        this(callback, Response.status(status).entity(entity).build());
    }

    public JSONP(String callback, Response response) {
        this.callback = Objects.requireNonNull(callback, "Null callback");
        Objects.requireNonNull(response, "Null response");
        entity = response.getEntity();
        status = response.getStatus();
    }

    @JsonIgnore
    public String getCallback() {
        return callback;
    }

    @JsonProperty("status")
    public int getStatus() {
        return status;
    }

    @JsonProperty("entity")
    public Object getEntity() {
        return entity;
    }

}
