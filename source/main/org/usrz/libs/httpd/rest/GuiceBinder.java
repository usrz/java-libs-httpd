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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.usrz.libs.logging.Log;

import com.google.inject.Injector;
import com.google.inject.Key;

public class GuiceBinder extends AbstractBinder {

    private final Log log = new Log();
    private final Injector injector;

    public GuiceBinder(Injector injector) {
        this.injector = injector;
    }

    @Override
    protected void configure() {
        for (Key<?> key: injector.getAllBindings().keySet()) {

            /* Create a factory for this value */
            @SuppressWarnings({ "rawtypes", "unchecked" })
            final Factory<?> factory = new GuiceFactory(key);

            /* Start binding the value */
            final Type type = key.getTypeLiteral().getType();
            final ServiceBindingBuilder<?> builder = this.bindFactory(factory).to(type);

            /* Process the annotation (if any) */
            final Annotation annotation = key.getAnnotation();
            if (annotation != null) {
                final Class<? extends Annotation> annotationType = annotation.annotationType();
                if (com.google.inject.name.Named.class.isAssignableFrom(annotationType)) {
                    builder.named(((com.google.inject.name.Named) annotation).value());
                } else if (javax.inject.Named.class.isAssignableFrom(annotationType)) {
                    builder.named(((javax.inject.Named) annotation).value());
                } else {
                    builder.qualifiedBy(annotation);
                }
            }

            /* Log what we've done */
            log.debug("Bound Guice's %s to HK2", key);
        }
    }

    private class GuiceFactory<T> implements Factory<T> {

        private final Key<T> key;

        private GuiceFactory(Key<T> key) {
            this.key = key;
        }

        @Override
        public T provide() {
            log.trace("Returining Guice instance of %s to HK2", key);
            return injector.getInstance(key);
        }

        @Override
        public void dispose(T instance) {
            /* Nothing to do */
        }
    }
}
