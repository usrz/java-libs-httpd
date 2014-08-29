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
import org.glassfish.jersey.server.ResourceConfig;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.httpd.jersey.GrizzlyHttpContainerFactory;
import org.usrz.libs.httpd.jersey.ServiceLocatorFactory;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.inject.ConfigurableProvider;

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
public class RestHandlerProvider extends ConfigurableProvider<HttpHandler> {

    /* Our Log instance */
    private static final Log log = new Log();

    /** Jersey's {@link ResourceConfig} for non-trivial customization */
    protected final ResourceConfig config;

    private final HttpHandlerPath path;
    //private GrizzlyHttpContainer container;

    /**
     * Create a new {@link RestHandlerProvider} instance specifying the
     * underlying {@link Application}.
     */
    public RestHandlerProvider(Application application, HttpHandlerPath path) {
        super(path, true);

        this.path = notNull(path, "Null path");
        config = ResourceConfig.forApplication(notNull(application, "Null application"));
        config.setApplicationName(application.getClass().getName());
    }

    /**
     * Create a new {@link RestHandlerProvider} instance specifying a consumer
     * configuring the {@link Application}.
     */
    public RestHandlerProvider(Consumer<ResourceConfig> consumer, HttpHandlerPath path) {
        super(path, true);

        this.path = notNull(path, "Null path");
        config = new ResourceConfig();
        config.setApplicationName(consumer.toString());
        notNull(consumer).accept(config);
    }

    /* ====================================================================== */

    @Override
    protected HttpHandler get(Injector injector, Configurations configurations) {

        /* Setup our object mapper (could be qualified with path) */
        final ObjectMapper mapper = getInstance(injector, ObjectMapper.class, path);

        final Map<Class<?>, Integer> contractPriorities = new HashMap<>();
        contractPriorities.put(MessageBodyWriter.class, Integer.MIN_VALUE);
        contractPriorities.put(MessageBodyReader.class, Integer.MIN_VALUE);
        config.register(new JacksonJsonProvider(mapper,
                    new Annotations[] { Annotations.JACKSON, Annotations.JAXB }),
                    Collections.unmodifiableMap(contractPriorities));

        /* Create a ServiceLocator parent of all locators and inject the configurations */
        final ServiceLocator locator = ServiceLocatorFactory.create(injector, path);

        /* Set up the ObjectMapper that will be used by this application */
        ServiceLocatorUtilities.addOneConstant(locator, mapper, null, ObjectMapper.class);

        /* Create a brand new Grizzly HTTP container from Jersey */
        log.debug("Jersey application at \"%s\" initializing", path.value());
        GrizzlyHttpContainer container = GrizzlyHttpContainerFactory.create(config, locator);
        log.info("Jersey application at \"%s\" initialized successfully", path.value());

        /* Create our handler and add it to our server configuration  */
        final HttpServer server = injector.getInstance(HttpServer.class);
        server.getServerConfiguration().addHttpHandler(container, path.value());
        log.info("Serving \"%s\" using Jersey application \"%s\"", path.value(), config.getApplicationName());

        /* All done! */
        return container;
    }

}
