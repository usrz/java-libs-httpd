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
package org.usrz.libs.httpd.rest;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import javax.inject.Inject;

import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.configurations.Configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;

public class ObjectMapperProvider implements Provider<ObjectMapper> {

    private static final Log log = new Log();
    private Configurations configurations;

    public ObjectMapperProvider() {
        /* Nothing to do */
    }

    @Inject
    public void init(Configurations configurations) {
        this.configurations = configurations.extract("json");
    }

    @Override
    public ObjectMapper get() {
        log.debug("Constructing new ObjectMapper instance");
        return new ObjectMapper()
                     .configure(INDENT_OUTPUT,                  configurations.get("json.indent", false))
                     .configure(WRITE_DATES_AS_TIMESTAMPS,      configurations.get("json.use_timestamps", true))
                     .configure(ORDER_MAP_ENTRIES_BY_KEYS,      configurations.get("json.order_keys", true))
                     .configure(SORT_PROPERTIES_ALPHABETICALLY, configurations.get("json.order_keys", true))
                     .setPropertyNamingStrategy(CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}
