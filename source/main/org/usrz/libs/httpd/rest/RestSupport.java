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

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.Objects;

import javax.ws.rs.core.Response;

public interface RestSupport {

    default Response created(URI requestUri, String location) {
        Objects.requireNonNull(requestUri, "Null request URI");
        Objects.requireNonNull(location, "Null location");
        final String path = requestUri.getPath();
        final URI uri = URI.create(path.endsWith("/") ? path + location : path + "/" + location);
        return Response.created(uri).build();
    }

    default Response stored(Object instance) {
        if (instance == null) return Response.status(NOT_FOUND).build();
        return Response.noContent().build();
    }

    default Response response(Object instance) {
        if (instance == null) return Response.status(NOT_FOUND).build();
        return Response.ok(instance).build();
    }

}
