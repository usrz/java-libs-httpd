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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ResourceConfig;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.httpd.jersey.GrizzlyHttpContainer;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.inject.ConfigurableProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.BindingAnnotation;
import com.google.inject.Injector;
import com.google.inject.Key;


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
        //final Configurations configurations = getInstance(injector, Configurations.class, path, true);

//        final ServiceLocator locator = ServiceLocatorFactory.getInstance().create("Jersey[" + path.value() + "]");

        // TODO: clean this shit up!!!!
        // TODO :rankings are SUPER important!!!!
        // TODO: try compiling without -> <dependency org="org.glassfish" name="hk2-guice-bridge" rev="latest.release" conf="default"/>
        // TODO: try compiling without -> <dependency org="org.glassfish" name="jersey-grizzly" rev="latest.release" conf="default"/>
        // TODO: move the code below in org.usrz.libs.httpd.jersey, it's fucking hairy!

        final ServiceLocator locator = Injections.createLocator(new AbstractBinder() {
                @Override protected void configure() {
                    final Map<Key<?>, Key<?>> applicationKeys = new HashMap<>();
                    final Map<Key<?>, Key<?>> keys = new HashMap<>();

                    for (Key<?> key: injector.getAllBindings().keySet()) {
                        if (path.equals(key.getAnnotation())) {
                            System.err.println("APPLICATION KEY " + key);
                            applicationKeys.put(Key.get(key.getTypeLiteral()), key);
                        } else {
//                        System.err.println("REGULAR KEY " + key);
                        System.err.println("KEY " + key);
                        keys.put(key, key);
                        }
                    }
                    System.err.println("----------------------");

                    keys.putAll(applicationKeys);

                    keys.forEach((bindable, guice) -> {
//                        if ((bindable.getAnnotationType() != null) && (bindable.getAnnotation() == null)) {
//                            System.err.println("SKIPPING " + bindable);
//                            return;
//                        }


                        final ServiceBindingBuilder<?> builder = bindFactory(new Factory<Object>(){
                            @Override public Object provide() {
                                final Object object = injector.getInstance(guice);
                                System.err.println("DELEGATING " + bindable + " TO GUICE " + guice + " PRODUCED " + object);
                                return object;
                            }
                            @Override public void dispose(Object instance) { /* Never dispose */ }
                        }).to(bindable.getTypeLiteral().getType());

                        Annotation annotation = bindable.getAnnotation();
                        final Class<? extends Annotation> annotationType = bindable.getAnnotationType();
                        //if ((annotationType != null) && (annotation == null)) annotation = AnnotationsCreator.generateAnnotation(annotationType);

                        if (annotation != null) {
                            System.err.println("1 BINDING APP " + bindable + " TO GUICE " + guice);
                            System.err.println("1 BINDING APP " + annotation.annotationType().isAnnotationPresent(BindingAnnotation.class));
                            if (annotationType.equals(javax.inject.Named.class)) {
                                builder.named(((javax.inject.Named) annotation).value()).ranked(0);
                            } else if (annotationType.equals(com.google.inject.name.Named.class)) {
                                builder.named(((com.google.inject.name.Named) annotation).value()).ranked(0);
                            } else {
                                builder.qualifiedBy(annotation).ranked(0);
                            }
                            //builder.ranked(0);

                        } else if (annotationType != null) {
                            System.err.println("2 BINDING APP " + bindable + " TO GUICE " + guice);

                            Annotation newAnnotation = AnnotationsCreator.generateAnnotation(annotationType);
                            System.out.println("NEW ANNOTATION -> " + newAnnotation + " / " + newAnnotation.getClass().isAnnotationPresent(Qualifier.class));
                            builder.qualifiedBy(newAnnotation).ranked(Integer.MIN_VALUE);


                        } else {
                            System.err.println("3 BINDING APP " + bindable + " TO GUICE " + guice);
                            builder.ranked(Integer.MAX_VALUE);
                        }

                        //System.err.println("BUILDER -> " + builder.toString());

                        //System.err.println("TYPE IS " + bindable.getTypeLiteral().getType());


                    });


                    System.err.println("===============================");

                    //if (configurations != null) this.bind(configurations).to(Configurations.class);
                    //this.bind(mapper).to(ObjectMapper.class);
                }});


//        for (Key<?> key: injector.getAllBindings().keySet()) {
//            System.err.println("KEY > " + key.getTypeLiteral().getType());
//        }
//        if (injector != null) throw new IllegalStateException();

        /* Set up all other bindings: they definitely go to Guice */
//        GuiceBridge.getGuiceBridge().initializeGuiceBridge(locator);
//        locator.getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);

        /* Create a brand new Grizzly HTTP container from Jersey */
        System.err.println("JERSEY INITIALIZING");
        GrizzlyHttpContainer container = new GrizzlyHttpContainer(config, locator);
        System.err.println("JERSEY INITIALIZED");

//        GuiceBridge.getGuiceBridge().initializeGuiceBridge(locator);
//        locator.getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);

        /* Create our handler and add it to our server configuration  */
        final HttpServer server = injector.getInstance(HttpServer.class);
        server.getServerConfiguration().addHttpHandler(container, path.value());
        log.info("Serving \"%s\" using Jersey application \"%s\"", path.value(), config.getApplicationName());

        return container;
    }

}
