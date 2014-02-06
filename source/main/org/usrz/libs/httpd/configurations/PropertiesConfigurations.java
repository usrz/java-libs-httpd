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
package org.usrz.libs.httpd.configurations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import org.usrz.libs.logging.Log;

public class PropertiesConfigurations extends Configurations {

    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final Log log = new Log();

    /* ====================================================================== */
    /* CONSTRUCTION                                                           */
    /* ====================================================================== */

    public PropertiesConfigurations(Reader reader) {
        super(load(reader));
    }

    public PropertiesConfigurations(InputStream input) {
        super(load(input));
    }

    public PropertiesConfigurations(URI uri) {
        super(load(uri));
    }

    public PropertiesConfigurations(URL url) {
        super(load(url));
    }

    public PropertiesConfigurations(File file) {
        super(load(file));
    }

    /* ====================================================================== */

    private static final Map<?, ?> load(Reader reader) {
        if (reader == null) throw new NullPointerException("Null reader");
        final Properties properties = new Properties();
        try {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("I/O error parsing configurations", exception);
        }
        return properties;
    }

    private static final Map<?, ?> load(InputStream input) {
        if (input == null) throw new NullPointerException("Null input stream");
        return load(new InputStreamReader(input, UTF8));
    }

    private static final Map<?, ?> load(URI uri) {
        if (uri == null) throw new NullPointerException("Null URI");
        log.debug("Parsing properties from %s", uri);
        try {
            return load(uri.toURL());
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Malformed URL " + uri, exception);
        }
    }

    private static final Map<?, ?> load(URL url) {
        if (url == null) throw new NullPointerException("Null URL");
        log.debug("Parsing properties from %s", url);
        try {
            return load(url.openStream());
        } catch (IOException exception) {
            throw new IllegalStateException("I/O error opening stream for URL " + url, exception);
        }
    }

    private static final Map<?, ?> load(File file) {
        if (file == null) throw new NullPointerException("Null file");
        log.debug("Parsing properties from %s", file.getAbsolutePath());
        try {
            return load(new FileInputStream(file));
        } catch (FileNotFoundException exception) {
            throw new IllegalStateException("File " + file.getAbsolutePath() + " not found", exception);
        }
    }

}
