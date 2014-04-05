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
package org.usrz.libs.httpd.inject;

import static org.usrz.libs.configurations.Configurations.EMPTY_CONFIGURATIONS;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.inject.Optional;


@Singleton
public class HttpServerProvider implements Provider<HttpServer >{

    private HttpServer server;

    private Configurations configurations = EMPTY_CONFIGURATIONS;
    private ErrorPageGenerator defaultErrorPageGenerator;

    @Inject
    protected HttpServerProvider() {
        /* Nothing to do, really */
    }

    @Inject
    protected final void setConfigurations(Configurations configurations) {
        this.configurations = configurations;
    }

    @Inject @Optional
    protected final void setDefaultErrorPageGenerator(ErrorPageGenerator defaultErrorPageGenerator) {
        this.defaultErrorPageGenerator = defaultErrorPageGenerator;
    }

    @Override
    public HttpServer get() {
        if (server != null) return server;

        final HttpServer server = new HttpServer();

        /* Get our configurations */
        final ServerConfiguration configuration = server.getServerConfiguration();

        /* If we have a default error page generator ... */
        if (defaultErrorPageGenerator != null)
            configuration.setDefaultErrorPageGenerator(defaultErrorPageGenerator);

        /* Sensible defaults */
        configuration.setHttpServerName   (configurations.get("name",    "Grizzly"));
        configuration.setHttpServerVersion(configurations.get("version",  Grizzly.getDotedVersion()));
        configuration.setSendFileEnabled  (configurations.get("sendFile", true));

        /* Even more sensible defaults (non-configurable) */
        configuration.setPassTraceRequest(false);
        configuration.setTraceEnabled(false);

        return this.server = server;
    }
}
