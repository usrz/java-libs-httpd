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

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.logging.Log;

public class FileHandlerProvider implements Provider<HttpHandler> {

    private final Log log = new Log();
    private final StaticHttpHandler handler;
    private final String path;

    public FileHandlerProvider(File documentRoot, HttpHandlerPath path) {
        this.path = notNull(path, "Null path").value();

        try {
            final File directory = notNull(documentRoot, "Document root not specified").getCanonicalFile();
            if (directory.isDirectory()) {
                handler = new StaticHttpHandler(directory.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("Document root \"" + directory + "\" is not a directory");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("I/O error resolving document root \"" + documentRoot + "\"", exception);
        }
    }

    @Inject
    private void setup(HttpServer server) {
        server.getServerConfiguration().addHttpHandler(handler, path);
        log.info("Serving \"%s\" as static files from \"%s\"", path, handler.getDefaultDocRoot());
    }

    @Override
    public HttpHandler get() {
        return handler;
    }
}
