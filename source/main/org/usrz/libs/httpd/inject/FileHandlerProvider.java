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

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

@Singleton
public class FileHandlerProvider extends HttpHandlerProvider {

    public FileHandlerProvider(File documentRoot, HttpHandlerPath path) {
        super(create(documentRoot), path);
    }

    private static HttpHandler create(File documentRoot) {
        notNull(documentRoot, "Document root not specified");
        try {
            final File directory = documentRoot.getCanonicalFile();
            if (directory.isDirectory()) return new StaticHttpHandler(directory.getAbsolutePath());
            throw new IllegalArgumentException("Document root \"" + directory + "\" is not a directory");
        } catch (IOException exception) {
            throw new IllegalArgumentException("I/O error resolving document root \"" + documentRoot + "\"", exception);
        }
    }
}