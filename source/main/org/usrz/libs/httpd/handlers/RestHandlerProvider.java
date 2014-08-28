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
package org.usrz.libs.httpd.handlers;

import static org.usrz.libs.utils.Check.notNull;
import static org.usrz.libs.utils.inject.Injections.getInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.logging.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Injector;

/**
 * A {@link Provider} working in conjunction with
 * <a href="https://jersey.java.net/">Jersey</a> building <em>JAX-RS</em>
 * {@link Application}s and wrapping them in an appropriate
 * <a href="https://grizzly.java.net/">Grizzly</a> {@link HttpHandler}s.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
@Singleton
public class RestHandlerProvider implements Provider<HttpHandler> {

    /* Our Log instance */
    private static final Log log = new Log();

    /** Jersey's {@link ResourceConfig} for non-trivial customization */
    protected final ResourceConfig config;

    private final HttpHandlerPath path;
    private GrizzlyHttpContainer container;

    /**
     * Create a new {@link RestHandlerProvider} instance specifying the
     * underlying {@link Application}.
     */
    public RestHandlerProvider(Application application, HttpHandlerPath path) {
        this.path = notNull(path, "Null path");
        config = ResourceConfig.forApplication(notNull(application, "Null application"));
        config.setApplicationName(application.getClass().getName());
    }

    /**
     * Create a new {@link RestHandlerProvider} instance specifying a consumer
     * configuring the {@link Application}.
     */
    public RestHandlerProvider(Consumer<ResourceConfig> consumer, HttpHandlerPath path) {
        this.path = notNull(path, "Null path");
        config = new ResourceConfig();
        config.setApplicationName(consumer.toString());
        notNull(consumer).accept(config);
    }

    /* ====================================================================== */

    @Inject
    private void setup(Injector injector, HttpServer server) {

        /* Setup our object mapper (could be qualified with path) */
        final ObjectMapper mapper = getInstance(injector, ObjectMapper.class, path);

        final Map<Class<?>, Integer> contractPriorities = new HashMap<>();
        contractPriorities.put(MessageBodyWriter.class, Integer.MIN_VALUE);
        contractPriorities.put(MessageBodyReader.class, Integer.MIN_VALUE);
        config.register(new JacksonJsonProvider(mapper,
                    new Annotations[] { Annotations.JACKSON, Annotations.JAXB }),
                    Collections.unmodifiableMap(contractPriorities));

        /* Create a brand new Grizzly HTTP container from Jersey */
        container = new GrizzlyHttpContainerProvider().createContainer(GrizzlyHttpContainer.class, config);

        /* Extract the ServiceLocator from the Container */
        final ServiceLocator locator = container.getApplicationHandler().getServiceLocator();

        /* Inject our (annotated with @Path) configurations into the ServiceLocator */
        final Configurations configurations = getInstance(injector, Configurations.class, path, true);
        ServiceLocatorUtilities.addOneConstant(locator, configurations, null, Configurations.class);

        /* Set up all other bindings: they definitely go to Guice */
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(locator);
        locator.getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);

        /* Create our handler and add it to our server configuration  */
        server.getServerConfiguration().addHttpHandler(container, path.value());
        log.info("Serving \"%s\" using Jersey application \"%s\"", path.value(), config.getApplicationName());

    }

    /* ====================================================================== */

    @Override
    public final HttpHandler get() {
        return container;
    }

}
