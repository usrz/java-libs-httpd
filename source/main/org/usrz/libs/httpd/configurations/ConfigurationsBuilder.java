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
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationsBuilder {

    private final Map<String, Object> configurations;

    public ConfigurationsBuilder() {
        configurations = new HashMap<>();
    }

    public Configurations build() {
        return new Configurations(configurations);
    }

    public ConfigurationsBuilder put(String key, String value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, boolean value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, byte value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, double value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, float value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, int value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, long value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, short value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, File value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, URI value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, URL value) {
        configurations.put(key, value);
        return this;
    }

    public ConfigurationsBuilder put(String key, Object value) {
        configurations.put(key, value);
        return this;
    }

}
