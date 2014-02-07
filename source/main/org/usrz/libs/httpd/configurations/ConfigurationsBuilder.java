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

/**
 * A simple builder for {@link Configurations} instances.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class ConfigurationsBuilder {

    private final Map<String, Object> configurations;

    /**
     * Create a new {@link ConfigurationsBuilder} instance.
     */
    public ConfigurationsBuilder() {
        configurations = new HashMap<>();
    }

    /**
     * Build a {@link Configurations} instance from the mappings previously
     * <em>put</em> in this {@linkplain ConfigurationsBuilder builder}.
     */
    public Configurations build() {
        return new Configurations(configurations, false);
    }

    /**
     * Associate the given key with the specified {@link String} value.
     */
    public ConfigurationsBuilder put(String key, String value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified <b>boolean</b> value.
     */
    public ConfigurationsBuilder put(String key, boolean value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified <b>byte</b> value.
     */
    public ConfigurationsBuilder put(String key, byte value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified <b>double</b> value.
     */
    public ConfigurationsBuilder put(String key, double value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified <b>float</b> value.
     */
    public ConfigurationsBuilder put(String key, float value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified <b>int</b> value.
     */
    public ConfigurationsBuilder put(String key, int value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified <b>long</b> value.
     */
    public ConfigurationsBuilder put(String key, long value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified <b>short</b> value.
     */
    public ConfigurationsBuilder put(String key, short value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified {@link File} value.
     */
    public ConfigurationsBuilder put(String key, File value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified {@link URI} value.
     */
    public ConfigurationsBuilder put(String key, URI value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified {@link URL} value.
     */
    public ConfigurationsBuilder put(String key, URL value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

    /**
     * Associate the given key with the specified {@link Object} value.
     */
    public ConfigurationsBuilder put(String key, Object value) {
        try {
            configurations.put(Configurations.validateKey(key), value);
            return this;
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        }
    }

}
