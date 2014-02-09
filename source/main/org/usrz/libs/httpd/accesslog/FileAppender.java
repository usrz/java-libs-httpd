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
package org.usrz.libs.httpd.accesslog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;

/**
 * An {@link AccessLogAppender appender} writing log entries to {@link File}s.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class FileAppender extends StreamAppender {

    private static final Logger LOGGER = Grizzly.logger(HttpServer.class);

    /**
     * Create a new {@link FileAppender} <em>appending to</em> (and not
     * overwriting) the specified {@link File}.
     *
     * @throws IOException If an I/O error occurred opening the file.
     */
    public FileAppender(File file)
    throws IOException {
        this(file, true);
    }

    /**
     * Create a new {@link FileAppender} writing to the specified {@link File}.
     *
     * @param append If <b>true</b> the file will be <em>appended to</em>,
     *               otherwise it will be completely <em>overwritten</em>.
     * @throws IOException If an I/O error occurred opening the file.
     */
    public FileAppender(File file, boolean append)
    throws IOException {
        super(new FileOutputStream(file, append));
        LOGGER.info("Access log file \"" + file.getAbsolutePath() + "\" opened");
    }
}
