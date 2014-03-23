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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;

/**
 * An {@link AccessLogAppender appender} enqueueing log entries into a
 * {@link LinkedBlockingQueue} and using a secondary, separate {@link Thread}
 * to forward them to a configured nested {@link AccessLogAppender appender}.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class QueueingAppender implements AccessLogAppender {

    private static final Logger LOGGER = Grizzly.logger(HttpServer.class);

    /* Our queue */
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    /* Where to forward stuff to */
    private final AccessLogAppender appender;
    /* The thread doing the despooling */
    private final Thread thread;

    /**
     * Create a new {@link QueueingAppender} instance enqueueing log entries
     * into a {@link LinkedBlockingQueue} and dequeueing them using a secondary
     * separate {@link Thread}.
     */
    public QueueingAppender(AccessLogAppender appender) {
        if (appender == null) throw new NullPointerException("Null appender");
        this.appender = appender;

        thread = new Thread(new Dequeuer());
        thread.setName(toString());
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void append(String accessLogEntry)
    throws IOException {
        if (thread.isAlive()) try {
            queue.put(accessLogEntry);
        } catch (InterruptedException exception) {
            LOGGER.log(FINE, "Interrupted adding log entry to the queue", exception);
        }
    }

    @Override
    public void close() throws IOException {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException exception) {
            LOGGER.log(FINE, "Interrupted stopping de-queuer", exception);
        } finally {
            appender.close();
        }
    }

    /* ====================================================================== */
    /* OUR DE-QUEUER                                                          */
    /* ====================================================================== */

    private final class Dequeuer implements Runnable {
        @Override
        public void run() {
            while (true) try {
                final String accessLogEntry = queue.take();
                if (accessLogEntry != null) appender.append(accessLogEntry);
            } catch (InterruptedException exception) {
                LOGGER.log(FINE, "Interrupted waiting for log entry to be queued, exiting!", exception);
                return;
            } catch (Throwable throwable) {
                LOGGER.log(WARNING, "Exception caught appending ququed log entry", throwable);
            }
        }
    }

}
