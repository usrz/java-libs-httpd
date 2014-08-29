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
package org.usrz.libs.httpd.jersey;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.Annotations;
import org.usrz.libs.utils.Check;

import com.google.inject.Injector;
import com.google.inject.Key;

@SuppressWarnings("restriction")
public class ServiceLocatorFactory {

    private static final Log log = new Log(ServiceLocatorFactory.class);

    private ServiceLocatorFactory() {
        throw new IllegalStateException("Do not construct");
    }

    public static ServiceLocator create(Injector injector, HttpHandlerPath annotation) {
        Check.notNull(injector, "Null injector");
        Check.notNull(annotation, "Null HttpHandlerPath annotation");
        final String applicationPath = annotation.value();

        /* Create a ServiceLocator */
        final ServiceLocator locator = Injections.createLocator(new AbstractBinder() {
            @Override protected void configure() {

                /*
                 * The applicationKeys will contain @HttpHandlerPath-annotated
                 * keys, mapping a non-annotated key to the original Guice key,
                 * while globalKeys will contain everything (no mapping
                 * is performed on those)...
                 */
                final Map<Key<?>, Key<?>> applicationKeys = new HashMap<>();
                final Map<Key<?>, Key<?>> globalKeys = new HashMap<>();

                /* Process only through the *explicit* bindings */
                for (Key<?> key: injector.getBindings().keySet()) {

                    /* Map the key if it's annotated with our HttpHandlerPath */
                    if (annotation.equals(key.getAnnotation())) {
                        log.trace("ServiceLocator[%s]: found application-specific key %s", applicationPath, key);
                        applicationKeys.put(Key.get(key.getTypeLiteral()), key);
                    }

                    /* All keys go in the "globalKeys" map */
                    log.trace("ServiceLocator[%s]: adding generic key %s", applicationPath, key);
                    globalKeys.put(key, key);
                }

                /*
                 * Override the global keys with the application-specific ones:
                 * basically here if we got something like
                 *   Key<String.class,@HttpHandlerPath(myPath)> = "foo";
                 *   Key<String.class> = "bar";
                 * the  global "bar" injection will be overridden by the the
                 * application specific "foo" one...
                 */
                globalKeys.putAll(applicationKeys);

                /* Ok, now we actually *bind* the keys in the ServiceLocator */
                globalKeys.forEach((bindable, guice) -> {

                    log.trace("ServiceLocator[%s]: binding %s to original Guice key %s", applicationPath, bindable, guice);

                    /* Start binding a HK2 factory to the type literal */
                    final ServiceBindingBuilder<?> builder = bindFactory(new Factory<Object>(){
                        @Override public void dispose(Object instance) { /* Never dispose */ }
                        @Override public Object provide() {
                            //log.trace("ServiceLocator[%s]: delegating construction of %s to Guice key %s", applicationPath, bindable, guice);
                            return injector.getInstance(guice);
                        }
                    }).to(bindable.getTypeLiteral().getType());

                    /* Mangle annotations: create instances for Guice class-only annotations */
                    final Class<? extends Annotation> annotationType = bindable.getAnnotationType();
                    final Annotation specifiedAnnotation = bindable.getAnnotation();
                    final Annotation annotation = specifiedAnnotation != null ? specifiedAnnotation :
                                                  annotationType != null ? Annotations.newInstance(annotationType) :
                                                  null;

                    if (annotation != null) {
                        /* If we have an annotation, convert and bind */
                        if (annotationType.equals(javax.inject.Named.class)) {
                            final String name = ((javax.inject.Named) annotation).value();
                            log.trace("ServiceLocator[%s]: binding type %s with name %s (javax.inject.Named)", applicationPath, bindable.getTypeLiteral().getType(), name);
                            builder.named(name); //.ranked(0);
                        } else if (annotationType.equals(com.google.inject.name.Named.class)) {
                            final String name = ((com.google.inject.name.Named) annotation).value();
                            log.trace("ServiceLocator[%s]: binding type %s with name %s (com.google.inject.name.Named)", applicationPath, bindable.getTypeLiteral().getType(), name);
                            builder.named(name); //.ranked(0);
                        } else {
                            log.trace("ServiceLocator[%s]: binding type %s with annotation %s", applicationPath, bindable.getTypeLiteral().getType(), annotation);
                            builder.qualifiedBy(annotation); //.ranked(0);
                        }

                    } else {
                        log.trace("ServiceLocator[%s]: binding type %s without any qualifier annotation", applicationPath, bindable.getTypeLiteral().getType());
                        builder.ranked(Integer.MAX_VALUE);
                    }
                });
            }});

        return locator;
    }
}
