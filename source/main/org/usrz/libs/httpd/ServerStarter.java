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

import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.util.function.Consumer;

import org.glassfish.grizzly.http.server.HttpServer;
import org.usrz.libs.configurations.CommandLineConfigurations;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.inject.Injector;
import org.usrz.libs.logging.Log;
import org.usrz.libs.logging.Logging;

public abstract class ServerStarter {

    static { Logging.init(); }

    private static final Log log = new Log();
    protected final Configurations configurations;

    protected ServerStarter(String args[]) {
        this(new CommandLineConfigurations(args));
    }

    private ServerStarter(Configurations configurations) {
        this.configurations = configurations;
    }

    public final void start(Consumer<ServerBuilder> consumer) {
        notNull(consumer, "Null ServerBuilder consumer");

        /* Create a new injector with this module */
        final Injector injector = Injector.create((binder) -> {
            if (consumer != null) consumer.accept(new ServerBuilder(binder));
        });

        /* Get a hold on our HttpServer instance */
        final HttpServer server = injector.getInstance(HttpServer.class);
        final String serverName = server.getServerConfiguration().getName();

        /* Attempt to start our server */
        try {
            log.info("Starting server %s", serverName);
            server.start();
        } catch (IOException exception) {
            log.error(exception, "Exception starting server %s", serverName);
            System.exit(1);
        }

        /* Add a shutdown hook terminating the server on exit */
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Shutting down server %s", serverName);
                server.shutdown();
            }
        });

        /* Wait forever until the server is shut down */
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException exception) {
            log.warn(exception, "Interrupted while waiting server shutdown");
        }
    }

}
