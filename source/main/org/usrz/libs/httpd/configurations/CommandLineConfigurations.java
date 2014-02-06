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

import static java.lang.Boolean.TRUE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CommandLineConfigurations extends Configurations {

    public CommandLineConfigurations(String... arguments) {
        super(parse(arguments));
    }

    private static Map<String, String> parse(String... arguments) {

        /* Prepare our map where key/values will be stored into */
        final Map<String, String> configurations = new HashMap<>();

        /* Iterate through arguments */
        for (final String argument: arguments) {

            /* Argument is like -Dxxx or -Dyyy=zzz */
            if (argument.startsWith("-D")) {
                final String entry = argument.substring(2);
                final int position = entry.indexOf('=');

                /* If we're looking at -Dxxx the value is always "true" */
                if (position < 0) configurations.put(entry, TRUE.toString());

                /* If we're looking at -Dyyy=zzz parse key and value */
                else {
                    final String key = entry.substring(0, position);
                    final String value = entry.substring(position + 1);
                    configurations.put(key, value);
                }

                /* No other options are supported */
            } else if (argument.startsWith("-")) {
                throw new IllegalArgumentException("Invalid argument " + argument);

            } else {
                /* Everything else is a reference to a properties file */
                configurations.putAll(new PropertiesConfigurations(new File(argument)));
            }
        }

        /* All done */
        return configurations;
    }

}
