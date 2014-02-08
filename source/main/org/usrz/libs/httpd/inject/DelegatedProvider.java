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

import java.util.logging.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

public abstract class DelegatedProvider<T> implements Provider<T> {

    private Module delegatingModule;

    protected DelegatedProvider() {
        this(null);
    }

    protected DelegatedProvider(Injector injector) {
        if (injector != null) this.setInjector(injector);
    }

    public abstract T get();

    protected final Injector delegatedInjector(Module... modules) {
        if (delegatingModule == null) throw new IllegalStateException("Not initialized");
        return Guice.createInjector(Modules.override(delegatingModule).with(modules));
    }

    @Inject
    private void setInjector(final Injector injector) {
        this.delegatingModule = new AbstractModule() {

            @Override @SuppressWarnings({ "rawtypes", "unchecked" })
            protected void configure() {
                this.configure(null);
                for (final Key<?> key: injector.getAllBindings().keySet()) {

                    /* Skip built-in bindings */
                    if (key.getTypeLiteral().equals(TypeLiteral.get(Injector.class))) continue;
                    if (key.getTypeLiteral().equals(TypeLiteral.get(Stage.class))) continue;
                    if (key.getTypeLiteral().equals(TypeLiteral.get(Logger.class))) continue;

                    /* Build our provider */
                    this.bind(key).toProvider(new Provider() {
                        public Object get() {
                            return injector.getInstance(key);
                        }
                    });
                };
            }
        };
    }
}
