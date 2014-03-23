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

import java.util.Date;

import org.glassfish.grizzly.http.server.Response;

/**
 * An interface defining a component capable of formatting {@link Response}s
 * into printable <em>access log entries</em>.
 *
 * <p>Implementations of this class <b>must</b> be thread-safe.</p>
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public interface AccessLogFormat {

    /**
     * Format the data contained in the specified {@link Response} and return
     * a {@link String} which can be appended to an access log file.
     *
     * @param response The {@link Response} holding the data to format.
     * @param timeStamp The {@link Date} at which the request was originated.
     * @param responseNanos The time, in nanoseconds, the {@link Response}
     *                      took to complete.
     */
    public String format(Response response, Date timeStamp, long responseNanos);

}
