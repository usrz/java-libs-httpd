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
import java.util.Collections;
import java.util.List;

import org.usrz.libs.logging.Log;

public class DefaultConfigurations extends Configurations {

    public static final String DEFAULTS_PROPERTIES = "defaults.properties";
    private static final Log log = new Log();

    public DefaultConfigurations() {
        super(load());
    }

    public DefaultConfigurations(Class<?> clazz) {
        super(load(clazz));
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

        Configurations configurations = new Configurations(Collections.emptyMap());
        for (URL resource: resources) {
            configurations = configurations.merge(new PropertiesConfigurations(resource));
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
