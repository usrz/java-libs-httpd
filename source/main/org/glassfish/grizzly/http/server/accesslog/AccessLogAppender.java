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

import java.io.Closeable;
import java.io.IOException;

/**
 * An interface defining an <em>appender</em> for Grizzly access logs entries.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public interface AccessLogAppender extends Closeable {

    /**
     * Append the specified access log entry.
     *
     * @param accessLogEntry The {@link String} value of the data to be append
     *                       in the access log.
     * @throws IOException If an I/O error occurred appending to the log.
     */
    public void append(String accessLogEntry)
    throws IOException;

    /**
     * Close any underlying resource owned by this appender.
     */
    @Override
    public void close()
    throws IOException;
}
