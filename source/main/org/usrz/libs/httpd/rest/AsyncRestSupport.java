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

import java.io.IOException;
import java.util.function.Function;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ChunkedOutput;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.concurrent.Acceptor;
import org.usrz.libs.utils.concurrent.NotifyingFuture;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface AsyncRestSupport {

    public ObjectMapper getObjectMapper();

    default <T> void respond(AsyncResponse response, Function<Acceptor<T>, NotifyingFuture<?>> function) {
        final ObjectMapper mapper = getObjectMapper();

        function.apply(new Acceptor<T>() {

            ChunkedOutput<String> output = null;

            private final void error(Throwable throwable) {
                new Log(AsyncRestSupport.this.getClass()).error(throwable, "Error iterating results");
                if (response.isSuspended()) response.resume(throwable);
            }

            @Override
            public boolean accept(T t) {
                try {
                    final String value = mapper.writeValueAsString(t);
                    if (output == null) {
                        response.resume(output = new ChunkedOutput<>(String.class));
                        output.write("[");
                        output.write(value);
                    } else {
                        output.write(",");
                        output.write(value);
                    }
                    return true;
                } catch (Throwable throwable) {
                    error(throwable);
                    return false;
                }
            }

            @Override
            public void completed() {
                if (output == null) response.resume("[]");
                else try {
                    output.write("]");
                    output.close();
                } catch (Throwable throwable) {
                    error(throwable);
                }
            }

            @Override
            public void failed(Throwable throwable) {
                error(throwable);
            }

        });
    }

    default <T> NotifyingFuture<T> respond(AsyncResponse response, NotifyingFuture<T> future) {
        return respond(response, future, (entity) -> {
            if (entity == null) throw new NotFoundException();
            response.resume(Response.ok(entity).build());
        });
    }

    default <T> NotifyingFuture<T> respond(AsyncResponse response, NotifyingFuture<T> future, Responder<T> responder) {
        return future.withConsumer((f) -> {
            try {
                responder.respondWith(f.get());
            } catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
                response.resume(throwable);
            }
        });
    }

    /* ====================================================================== */

    @FunctionalInterface
    public interface Responder<T> {

        public void respondWith(T entity)
        throws WebApplicationException, IOException;

    }

}
