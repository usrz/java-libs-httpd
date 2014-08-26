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
package org.usrz.libs.httpd.rest.handlers;

import static org.usrz.libs.utils.Check.notNull;

import javax.inject.Provider;

import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.utils.json.ObjectMapperProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RestObjectMapperProvider extends ObjectMapperProvider implements Provider<ObjectMapper> {

    public RestObjectMapperProvider(Configurations configurations) {
        super(notNull(configurations, "Null configurations"));
    }

}
