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
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.usrz.libs.logging.Log;

/**
 * A {@link Configurations} implementation parsing mappings from a
 * {@link File}, either in <em>Java {@linkplain Properties properties}</em>
 * or <em><a href="http://json.org/">JSON</a></em> format.
 *
 * <p>This class does not make any attempt to <em>discover</em> the format
 * of the contents to be parsed, it simply relies on the <em>extension</em>
 * which <b>must be</b> "<code>.json</code>" for JSON files, or either
 * "<code>.properties</code>" or "<code>.xml</code>" for properties files.</p>
 */
public class FileConfigurations extends Configurations {

    private static final Log log = new Log();

    /**
     * Create a new {@link Configurations} instance parsing the specified
     * {@link File}, either in <em>Java {@linkplain Properties properties}</em>
     * or <em><a href="http://json.org/">JSON</a></em> format.
     */
    public FileConfigurations(File file) {
        super(load(file), false);
    }

    /* ====================================================================== */

    private static final Map<?, ?> load(File file) {
        if (file == null) throw new NullPointerException("Null URL");

        log.debug("Parsing configurations from file %s", file);

        final String name = file.getAbsolutePath();
        try {
            if (!file.isFile())
                throw new IllegalArgumentException("File " + name + " not found (or not a file)");

            final FileInputStream input = new FileInputStream(file);
            if (name.endsWith(".json"))
                return JsonConfigurations.parse(input);
            else if (name.endsWith(".properties") || name.endsWith(".xml")) {
                return PropertiesConfigurations.parse(input);
            } else {
                input.close();
                throw new IllegalArgumentException("File \"" + name + "\" must end with \".json\", \".properties\", or \".xml\"");
            }
        } catch (ConfigurationsException exception) {
            throw exception.initLocation(name).unchecked();
        } catch (IOException exception) {
            throw new IllegalStateException("I/O error parsing " + name);
        }
    }

}
