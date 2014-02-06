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

import java.io.InputStream;

import org.usrz.libs.logging.Log;

public class ResourceConfigurations extends PropertiesConfigurations {

    private static final Log log = new Log();

    public ResourceConfigurations(String resource) {
        super(load(resource));
    }

    public ResourceConfigurations(Class<?> clazz, String resource) {
        super(load(clazz, resource));
    }

    private static InputStream load(String resource) {
        final String className = new Throwable().getStackTrace()[2].getClassName();
        try {
            return load(Class.forName(className), resource);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Class \"" + className + "\" not found", exception);
        }
    }

    private static InputStream load(Class<?> clazz, String resource) {
        if (clazz == null) throw new NullPointerException("Null class");
        log.debug("Loading configurations from resource \"%s\" of class %s", resource, clazz);
        final InputStream input = clazz.getResourceAsStream(resource);
        if (input == null) throw new IllegalStateException("Resource \"" + resource + "\" not found for class " + clazz.getName());
        return input;
    }
}
