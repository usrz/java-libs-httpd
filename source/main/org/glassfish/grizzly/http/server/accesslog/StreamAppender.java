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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * An {@link AccessLogAppender appender} writing log entries to an
 * {@link OutputStream}.
 *
 * <p>Log entries will <b>always</b> encoded in <em>UTF-8</em>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class StreamAppender implements AccessLogAppender {

    /* Line separator for entries, respect Windoshhhh */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    /* The writer we'll actually use */
    private final Writer writer;

    /**
     * Create a new {@link StreamAppender} instance writing log entries to the
     * specified {@link OutputStream}.
     */
    public StreamAppender(OutputStream  output) {
        writer = new OutputStreamWriter(output, Charset.forName("UTF-8"));
    }

    @Override
    public void append(String accessLogEntry)
    throws IOException {
        synchronized(this) {
            writer.write(accessLogEntry);
            writer.write(LINE_SEPARATOR);
            writer.flush();
        }
    }

    @Override
    public void close()
    throws IOException {
        writer.close();
    }

}
