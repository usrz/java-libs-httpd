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

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Singleton;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.accesslog.AccessLogBuilder;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.configurations.Configuration;
import org.usrz.libs.utils.configurations.Configurations;

import com.google.inject.Inject;
import com.google.inject.Provider;

@Singleton
@SuppressWarnings("restriction")
public class HttpServerProvider implements Provider<HttpServer >{

    private final static Log log = new Log();

    private final Collection<NetworkListener> networkListeners;
    private final Map<String, HttpHandler> httpHandlers;
    private final Configurations configurations;
    private HttpServer server;

    private ErrorPageGenerator defaultErrorPageGenerator;

    @Inject
    protected HttpServerProvider(Collection<NetworkListener> networkListeners,
                                 Map<String, HttpHandler> httpHandlers,
                                 @Configuration(HttpServerProvider.class) Configurations configurations) {
        this.networkListeners = networkListeners;
        this.httpHandlers = httpHandlers;
        this.configurations = configurations;
    }

    @Inject(optional=true)
    protected final void setDefaultErrorPageGenerator(ErrorPageGenerator defaultErrorPageGenerator) {
        this.defaultErrorPageGenerator = defaultErrorPageGenerator;
    }

    @Override
    public HttpServer get() {
        if (server != null) return server;

        final String name = configurations.get("name", "!default");
        final HttpServer server = new HttpServer();

        /* Get our configurations and set the server name */
        final ServerConfiguration configuration = server.getServerConfiguration();
        configuration.setName(name);

        /* Set up our access log (if needed) */
        final File accessLog = configurations.getFile("accesslog.file", null);
        if (accessLog != null) {
            /* Start building our access log */
            final AccessLogBuilder accessLogBuilder = new AccessLogBuilder(accessLog);

            /* Configure log rotation, if necessary */
            final String rotate = configurations.getString("accesslog.rotate", null);
            if (rotate != null) switch (rotate.toLowerCase()) {
                case "daily":  accessLogBuilder.rotatedDaily(); break;
                case "hourly": accessLogBuilder.rotatedHourly(); break;
                default: throw new IllegalStateException("Unsupported value \"" + rotate + "\" for parameter \"accesslog.rotate\"");
            } else {
                final String rotationPattern = configurations.getString("accesslog.rotationPattern");
                if (rotationPattern != null) accessLogBuilder.rotationPattern(rotationPattern);
            }

            /* Set the format, if we have to */
            final String format = configurations.getString("accesslog.format", null);
            if (format != null) accessLogBuilder.format(format);

            /* Finally configure time zone, if we need to... */
            final String timezone = configurations.getString("accesslog.timezone", null);
            if (timezone != null) accessLogBuilder.timeZone(timezone);

            /* Great! Instrument our server configurations */
            accessLogBuilder.instrument(configuration);
        }

        /* Check that we have some listeners, add them and dump out some informations about them */
        if (networkListeners.isEmpty()) throw new IllegalStateException("No listeners configured for server \"" + name +"\"");
        for (NetworkListener listener: networkListeners) {
            server.addListener(listener);
            log.info("Listener \"%s\" (host=%s, port=%s, secure=%b) configured for server \"%s\"",
                     listener.getName(), listener.getHost(), listener.getPort(), listener.isSecure(), name);
        }

        /* Instrument the server configuration with the various contexts */
        if (httpHandlers.isEmpty()) throw new IllegalStateException("No handlers configured for server \"" + name +"\"");
        for (Entry<String, HttpHandler> entry: httpHandlers.entrySet()) {
            final String path = entry.getKey();
            final HttpHandler handler = entry.getValue();
            configuration.addHttpHandler(handler, path);
            log.info("Handler \"%s[%s]\" configured at path \"%s\" for server \"%s\"",
                     handler.getClass().getName(), handler.getName(), path, name);
        }

        /* If we have a default error page generator ... */
        if (defaultErrorPageGenerator != null)
            configuration.setDefaultErrorPageGenerator(defaultErrorPageGenerator);

        /* Sensible defaults */
        configuration.setHttpServerName   (configurations.get("server.name",    "Grizzly"));
        configuration.setHttpServerVersion(configurations.get("server.version",  Grizzly.getDotedVersion()));
        configuration.setSendFileEnabled  (configurations.get("server.sendFile", true));

        /* Even more sensible defaults (non-configurable) */
        configuration.setPassTraceRequest(false);
        configuration.setTraceEnabled(false);

        return this.server = server;
    }
}
