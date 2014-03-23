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

import static java.util.logging.Level.WARNING;

import java.util.Date;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.HttpServerProbe;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * A {@linkplain HttpServerProbe Grizzly probe} used to provide
 * access logs generation.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class AccessLogProbe extends HttpServerProbe.Adapter {

    /**
     * The minimum <em>response status</em> that will trigger an entry
     * in an access log configured by this instance (default, log everything).
     */
    public static final int DEFAULT_STATUS_THRESHOLD = Integer.MIN_VALUE;

    /* Our timestamp request attribute */
    private static final String ATTRIBUTE_TIME_STAMP = AccessLogProbe.class.getName() + ".timeStamp";
    /* Our logger, for eventualities */
    private static final Logger LOGGER = Grizzly.logger(HttpServer.class);

    /* The appender to send formatted data to */
    private final AccessLogAppender appender;
    /* The format to format data to log */
    private final AccessLogFormat format;
    /* The minimum status threshold */
    private final int statusThreshold;

    /**
     * Create a new {@link AccessLogProbe} formatting data with the specified
     * {@linkplain AccessLogFormat format} and appending it to the specified
     * {@linkplain AccessLogAppender appender}.
     */
    public AccessLogProbe(AccessLogAppender appender, AccessLogFormat format) {
        this(appender, format, DEFAULT_STATUS_THRESHOLD);
    }

    /**
     * Create a new {@link AccessLogProbe} formatting data with the specified
     * {@linkplain AccessLogFormat format} and appending it to the specified
     * {@linkplain AccessLogAppender appender}.
     *
     * <p>Only responses with <em>status</em> over the specified threshold will
     * be logged, for example a threshold of <code>500</code> will only
     * generate log entries for requests that terminated in error.</p>
     */
    public AccessLogProbe(AccessLogAppender appender, AccessLogFormat format, int statusThreshold) {
        if (appender == null) throw new NullPointerException("Null access log appender");
        if (format == null) throw new NullPointerException("Null format");
        this.appender = appender;
        this.format = format;
        this.statusThreshold = statusThreshold;
    }

    /**
     * Instrument the specified {@link Request} with an attribute marking its
     * <em>received</em> time (in {@linkplain System#nanoTime() nanoseconds}).
     */
    @Override @SuppressWarnings("rawtypes")
    public void onRequestReceiveEvent(HttpServerFilter filter, Connection connection, Request request) {
        request.setAttribute(ATTRIBUTE_TIME_STAMP, System.nanoTime());
        /*
         * It seems that in some edge cases Grizzly is not caching the
         * connection addresses in the request / response structure. Internally
         * the TCPNIOConnectionClass uses a Holder to store those (which
         * provides lazy initialization). We force the holders to get (and
         * cache) the values by alling the "get(Local|Peer)Address()" methods.
         */
        connection.getLocalAddress();
        connection.getPeerAddress();
    }

    /**
     * Receive notification of the completion of a {@link Response} an possibly
     * trigger an access log entry generation.
     */
    @Override @SuppressWarnings("rawtypes")
    public void onRequestCompleteEvent(HttpServerFilter filter, Connection connection, Response response) {

        /* Only call the format/appender if we have to */
        if (response.getStatus() < statusThreshold) return;

        /* Calculate request timing */
        final Long requestNanos = (Long) response.getRequest().getAttribute(ATTRIBUTE_TIME_STAMP);

        final long timeStamp = System.currentTimeMillis();
        final long nanoStamp = System.nanoTime();

        final long responseNanos = requestNanos == null ? -1 : nanoStamp - requestNanos.longValue();
        final Date requestMillis = new Date(timeStamp - (responseNanos / 1000000000L));

        /* Create a formatted log entry string and append it */
        try {
            appender.append(format.format(response, requestMillis, responseNanos));
        } catch (Throwable throwable) {
            LOGGER.log(WARNING, "Exception caught appending to access log", throwable);
        }
    }

}
