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

import static org.usrz.libs.utils.Check.notNull;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.usrz.libs.logging.Log;

import com.google.inject.Injector;
import com.google.inject.Key;

public class DefaultEPGProvider implements Provider<ErrorPageGenerator> {

    private final Log log = new Log();
    private final Key<? extends ErrorPageGenerator> key;
    private final ErrorPageGenerator generator;

    public DefaultEPGProvider(ErrorPageGenerator generator) {
        this.generator = notNull(generator, "Null error page generator");
        key = null;
    }

    public DefaultEPGProvider(Key<? extends ErrorPageGenerator> key) {
        this.key = notNull(key, "Null key");
        generator = null;
    }

    @Inject
    private void setup(Injector injector, HttpServer server) {
        final ServerConfiguration configuration = server.getServerConfiguration();
        configuration.setDefaultErrorPageGenerator(generator != null ? generator :
                                                       injector.getInstance(key));
        log.info("Configured default error page generator on server \"%s\"", configuration.getName());
    }

    @Override
    public ErrorPageGenerator get() {
        return generator;
    }
}
