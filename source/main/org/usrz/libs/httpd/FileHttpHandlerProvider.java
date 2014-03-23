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

import java.io.File;
import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import com.google.inject.Provider;
import com.google.inject.ProvisionException;

public class FileHttpHandlerProvider implements Provider<HttpHandler> {

    private String documentRoot = null;

    public FileHttpHandlerProvider(String documentRoot) {
        if (documentRoot == null) throw new NullPointerException("Null document root");
        this.documentRoot = documentRoot;
    }

    @Override
    public HttpHandler get() {
        if (documentRoot == null) throw new ProvisionException("Configuration \"documentRoot\" not specified");
        try {
            final File directory = new File(documentRoot).getCanonicalFile();
            if (directory.isDirectory()) return new StaticHttpHandler(directory.getAbsolutePath());
            throw new ProvisionException("Document root \"" + directory + "\" is not a directory");
        } catch (IOException exception) {
            throw new ProvisionException("I/O error resolving document root \"" + documentRoot + "\"", exception);
        }
    }
}
