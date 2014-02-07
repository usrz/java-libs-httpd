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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.usrz.libs.logging.Log;

/**
 * A {@link Configurations} implementation reading <em>key-value</em> mappings
 * from <em>Java {@linkplain Properties properties} files</em> stored as
 * alongside {@link Class#getResource(String) classes}, their super-classes
 * and implemented interfaces.
 *
 * <p>This class, when instantiated, will traverse the class hierarchy and
 * load (in order) a resource called <code>defaults.properties</code> stored
 * alongside the various classes it finds.</p>
 *
 * <p>The order in which classes will be traversed is as follows:</p>
 *
 * <ol>
 *   <li>The class {@linkplain DefaultConfigurations#DefaultConfigurations(Class)
 *       specified} at construction.</li>
 *   <li>All its <em>super-classes</em> (recursively).</li>
 *   <li>All The interfaces implemented by the specified class and all its
 *       super-classes</li>
 * </ol>
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class DefaultConfigurations extends Configurations {

    /**
     * The resource name (<code>defaults.properties</code>) to use when looking
     * for default properties files.
     */
    public static final String DEFAULTS_PROPERTIES = "defaults.properties";

    /**
     * The resource name (<code>defaults.properties</code>) to use when looking
     * for default JSON files.
     */
    public static final String DEFAULTS_JSON = "defaults.json";

    /* Our log */
    private static final Log log = new Log();

    /**
     * Create a new {@link ResourceConfigurations} instance parsing the
     * defaults files associated with the <em>caller</em> {@link Class}.
     */
    public DefaultConfigurations() {
        super(load(), false);
    }

    /**
     * Create a new {@link ResourceConfigurations} instance parsing the
     * defaults files associated with the <em>specified</em> {@link Class}.
     */
    public DefaultConfigurations(Class<?> clazz) {
        super(load(clazz), false);
    }

    /* ====================================================================== */

    private static final Configurations load() {
        final String className = new Throwable().getStackTrace()[2].getClassName();
        try {
            return load(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Class \"" + className + "\" not found", exception);
        }
    }

    private static final Configurations load(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException("Null class");

        final List<URL> resources = new ArrayList<>();
        discover(clazz, resources);

        Configurations configurations = Configurations.EMPTY_CONFIGURATIONS;
        for (URL resource: resources) {
            configurations = configurations.merge(new URLConfigurations(resource));
        }

        return configurations;
    }

    private static final void discover(Class<?> clazz, List<URL> discovered) {

        final URL resource = clazz.getResource(DEFAULTS_PROPERTIES);

        log.trace("Defaults for class \"%s\" %s", clazz, resource == null ? "do not exist" : "DO EXIST!");

        if ((resource != null) && (!discovered.contains(resource))) discovered.add(resource);

        if (clazz.getSuperclass() != null)
            discover(clazz.getSuperclass(), discovered);

        for (Class<?> interfaceClass: clazz.getInterfaces()) {
            discover(interfaceClass, discovered);
        }
    }
}
