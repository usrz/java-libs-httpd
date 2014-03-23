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
package org.glassfish.grizzly.http.server.accesslog;

import java.text.SimpleDateFormat;

/**
 * Simple utility class to keep pre-configured {@link SimpleDateFormat}s around
 * on a per-{@link Thread} basis. The {@link SimpleDateFormat#clone() clone()}
 * method will be used to generate new instances.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
class SimpleDateFormatThreadLocal extends ThreadLocal<SimpleDateFormat> {

    private final SimpleDateFormat format;

    SimpleDateFormatThreadLocal(String format) {
        this.format = new SimpleDateFormat(format);
    }

    @Override
    protected SimpleDateFormat initialValue() {
        return (SimpleDateFormat) format.clone();
    }

}