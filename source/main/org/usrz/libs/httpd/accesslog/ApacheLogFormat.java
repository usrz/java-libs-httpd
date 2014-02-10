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

import static java.util.logging.Level.WARNING;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.MimeHeaders;

/**
 * An {@link AccessLogFormat} using a standard vaguely similar and heavily
 * influenced by <a href="http://httpd.apache.org/docs/2.2/mod/mod_log_config.html#formats">Apache's
 * own custom access log formats</a>.
 *
 * <p>As with Apache, the format string specified at
 * {@linkplain #ApacheLogFormat(String) construction} should be composed of
 * these tokens:</p>
 *
 * <table>
 * <tr><th><code>%%</code></th>
 *     <td>The literal percent sign (can also be escaped with back-slash, like "<code>\%</code>"</td></tr>
 * <tr><th><code>%a</code></th>
 *     <td>Remote IP-address</td></tr>
 * <tr><th><code>%A</code></th>
 *     <td>Local IP-address</td></tr>
 * <tr><th><code>%b</code></th>
 *     <td>Size of response in bytes, excluding HTTP headers, using "<code>-</code>" (a <em>dash</em> character) rather than a "<code>0</code>" (<em>zero</em>) when no bytes are sent</td></tr>
 * <tr><th><code>%B</code></th>
 *     <td>Size of response in bytes, excluding HTTP headers</td></tr>
 * <tr><th><code>%{Foobar}C</code></th>
 *     <td>The contents of cookie "<code>Foobar</code>" in the request sent to the server</td></tr>
 * <tr><th><code>%D</code></th>
 *     <td>The time taken to serve the request, in microseconds</td></tr>
 * <tr><th><code>%h</code></th>
 *     <td>Remote host name</td></tr>
 * <tr><th><code>%{local|remote}h</code></th>
 *     <td>Host name, either "<code>local</code>" or "<code>remote</code>"</td></tr>
 * <tr><th><code>%H</code></th>
 *     <td>The request protocol</td></tr>
 * <tr><th><code>%{Foobar}i</code></th>
 *     <td>The contents of the "<code>Foobar: ...</code>" header in the request</td></tr>
 * <tr><th><code>%m</code></th>
 *     <td>The request method</td></tr>
 * <tr><th><code>%{Foobar}o</code></th>
 *     <td>The contents of the "<code>Foobar: ...</code>" header in the response</td></tr>
 * <tr><th><code>%p</code></th>
 *     <td>Local port number</td></tr>
 * <tr><th><code>%{local|remote}p</code></th>
 *     <td>The port number, either "<code>local</code>" or "<code>remote</code>"</td></tr>
 * <tr><th><code>%q</code></th>
 *     <td>The query string, prepended with a "<code>?</code>" (<em>question mark</em>) if a query string exists, otherwise an empty string</td></tr>
 * <tr><th><code>%r</code></th>
 *     <td>First line of request, an alias to "<code>%m %U%q %H</code>"</td></tr>
 * <tr><th><code>%s</code></th>
 *     <td>Status code</td></tr>
 * <tr><th><code>%t</code></th>
 *     <td>The time the request was received, in standard <em>English</em> format (like "<code>[09/Feb/2014:12:00:34 +0900]</code>")</td></tr>
 * <tr><th><code>%{format}t</code></th>
 *     <td>The time the request was received, in the form given by "<code>format</code>", as specified by {@link SimpleDateFormat}</td></tr>
 * <tr><th><code>%T</code></th>
 *     <td>The time taken to serve the request, in seconds</td></tr>
 * <tr><th><code>%{...}T</code></th>
 *     <td>The time taken to serve the request. The parameter can be a time unit like:
 *         <ul><li>"<code>n</code>",  "<code>nano<em>[s]<em></code>",  "<code>nanosec<em>[s]<em></code>",  "<code>nanosecond<em>[s]<em></code>"</li>
 *             <li>                  "<code>micro<em>[s]<em></code>", "<code>microsec<em>[s]<em></code>", "<code>microsecond<em>[s]<em></code>"</li>
 *             <li>"<code>m</code>", "<code>milli<em>[s]<em></code>", "<code>millisec<em>[s]<em></code>", "<code>millisecond<em>[s]<em></code>"</li>
 *             <li>"<code>s</code>",                                       "<code>sec<em>[s]<em></code>",      "<code>second<em>[s]<em></code>"</li></ul></td></tr>
 * <tr><th><code>%u</code></th>
 *     <td>The remote user name</td></tr>
 * <tr><th><code>%U</code></th>
 *     <td>The URL path requested, not including any query string</td></tr>
 * <tr><th><code>%v</code></th>
 *     <td>The name of the server which served the request</td></tr>
 * </table>
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class ApacheLogFormat implements AccessLogFormat {

    /** A {@linkplain ApacheLogFormat format} compatible with Apache's <em>common</em> format. */
    public static final ApacheLogFormat COMMON = new ApacheLogFormat("%h - %u %t \"%r\" %s %b");
    /** A {@linkplain ApacheLogFormat format} compatible with Apache's <em>combined</em> format. */
    public static final ApacheLogFormat COMBINED = new ApacheLogFormat("%h - %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-agent}i\"");
    /** A {@linkplain ApacheLogFormat format} compatible with Apache's <em>common with virtual-hosts</em> format. */
    public static final ApacheLogFormat VHOST_COMMON = new ApacheLogFormat("%v %h - %u %t \"%r\" %s %b");
    /** A {@linkplain ApacheLogFormat format} compatible with Apache's <em>combined with virtual-hosts</em> format. */
    public static final ApacheLogFormat VHOST_COMBINED = new ApacheLogFormat("%v %h - %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-agent}i\"");
    /** A {@linkplain ApacheLogFormat format} compatible with Apache's <em>referer</em> format. */
    public static final ApacheLogFormat REFERER = new ApacheLogFormat("%{Referer}i -> %U");
    /** A {@linkplain ApacheLogFormat format} compatible with Apache's <em>user-agent</em> format. */
    public static final ApacheLogFormat AGENT = new ApacheLogFormat("%{User-agent}i");

    /* Log log log, never enough */
    private static final Logger LOGGER = Grizzly.logger(HttpServer.class);

    /* Our list of fields for formatting */
    private final List<Field> fields;

    /**
     * Create a new {@link ApacheLogFormat} instance by parsing the format
     * from the specified {@link String}.
     */
    public ApacheLogFormat(String format) {
        fields = parse(format);
    }

    @Override
    public String format(Response response, Date timeStamp, long responseNanos) {
        final StringBuilder builder = new StringBuilder();
        final Request request = response.getRequest();
        for (Field field: fields) try {
            field.format(builder, request, response, timeStamp, responseNanos);
        } catch (Exception exception) {
            LOGGER.log(WARNING, "Exception formatting access log entry", exception);
            builder.append('-');
        }
        return builder.toString();
    }

    /**
     * Return the <em>normalized</em> format associated with this instance.
     */
    public String getFormat() {
        final StringBuilder builder = new StringBuilder();
        for (Field field: fields) builder.append(field.toString());
        return builder.toString();
    }

    /* ====================================================================== */
    /* PARSING OF FORMAT STRINGS                                              */
    /* ====================================================================== */

    private static List<Field> parse(String format) {
        final List<Field> fields = new ArrayList<>();

        for (int x = 0; x < format.length(); x ++) {
            switch (format.charAt(x)) {
                case '\\': x = parseEscape(format, x, fields); break;
                case '%':  x = parseFormat(format, null, x, fields); break;
                default: addLiteral(fields, format.charAt(x));
            }
        }
        return fields;
    }

    private static int parseFormat(String format, String parameter, int position, List<Field> fields) {
        if (++position < format.length()) {
            final char field = format.charAt(position);

            /* Initial check to see if parameter is supposed to be there */
            if (parameter != null) switch (field) {
                case 'C': break; // parameter is cookie name
                case 'h': break; // parameter for host ("local" or "remote")
                case 'i': break; // parameter is request header name
                case 'o': break; // parameter is response header name
                case 'p': break; // parameter for port ("local" or "remote")
                case 't': break; // parameter is simple date format's format
                case 'T': break; // parameter is scale ("nano", "micro", "milli" or number)
                default: throw new IllegalArgumentException("Unsupported parameter \"" + parameter + "\" for field '" + field + "' in [" + format + "] at character " + position);
            }


            switch (field) {
                case '{': return parseParameter(format, position, fields);
                case '%': addLiteral(fields, '%'); break; // The percent sign
                case 'a': fields.add(new RemoteAddressField()); break; // Remote IP-address
                case 'A': fields.add(new LocalAddressField()); break; // Local IP-address
                case 'b': fields.add(new ResponseSizeField(false)); break; // Size of response in bytes in CLF format
                case 'B': fields.add(new ResponseSizeField(true)); break; // Size of response in bytes with zeroes
          /* */ case 'C': fields.add(new RequestCookieField(parameter)); break; // The contents of a cookie
                case 'D': fields.add(new ResponseTimeField("micro", format, position)); break; // The time taken to serve the request, in microseconds.
          /* */ case 'h':  // Remote (or local if parameterized) host
                    fields.add(parseLocal(parameter, false, field, format, position) ?
                                   new LocalHostField() :
                                   new RemoteHostField());
                    break;
                case 'H': fields.add(new RequestProtocolField()); break; // The request protocol
          /* */ case 'i': fields.add(new RequestHeaderField(parameter)); break; // A request header
                case 'm': fields.add(new RequestMethodField()); break; // The request method
          /* */ case 'o': fields.add(new ResponseHeaderField(parameter)); break; // A response header
          /* */ case 'p': // Local (or remote if parameterized) port
                    fields.add(parseLocal(parameter, true, field, format, position) ?
                                   new LocalPortField() :
                                   new RemotePortField());
                    break;
                case 'q': fields.add(new RequestQueryField()); break; // The query string (prepended with a ?)
                case 'r': // First line of request, alias to "%m %U%q %H"
                    fields.add(new RequestMethodField());
                    addLiteral(fields, ' ');
                    fields.add(new RequestURIField());
                    fields.add(new RequestQueryField());
                    addLiteral(fields, ' ');
                    fields.add(new RequestProtocolField());
                    break;

                case 's': fields.add(new ResponseStatusField()); break; // Status.
          /* */ case 't': fields.add(new RequestTimeField(parameter)); break; // The time, in the form given by format, which should be in strftime(3) format. (potentially localized)
          /* */ case 'T': fields.add(new ResponseTimeField(parameter, format, position)); break; // The time taken to serve the request, in the scale specified.
                case 'u': fields.add(new RequestUserField()); break; // The URL path requested, not including any query string.
                case 'U': fields.add(new RequestURIField()); break; // The URL path requested, not including any query string.
                case 'v': fields.add(new ServerNameField()); break; // The canonical ServerName of the server serving the request.
                default: throw new IllegalArgumentException("Unsupported field '" + field + "' in [" + format + "] at character " + position);
            }
            return position;
        }
        throw new IllegalArgumentException("Unterminated field declaration in [" + format + "] at character " + position);
    }

    private static boolean parseLocal(String parameter, boolean defaultValue, char field, String format, int position) {
        if (parameter == null) return defaultValue;
        switch (parameter.trim().toLowerCase()) {
            case "local": return true;
            case "remote": return false;
            default: throw new IllegalArgumentException("Unsupported parameter \"" + parameter + "\" for field '" + field + "' in [" + format + "] at character " + position);
        }
    }

    private static int parseParameter(String format, int position, List<Field> fields) {
        if (++position < format.length()) {
            final int end = format.indexOf('}', position);
            if (end == position) {
                return parseFormat(format, null, end, fields);
            } else if (end > position) {
                return parseFormat(format, format.substring(position, end), end, fields);
            }
        }
        throw new IllegalArgumentException("Unterminated format parameter in [" + format + "] at character " + position);
    }

    private static int parseEscape(String format, int position, List<Field> fields) {
        if (++position < format.length()) {
            final char escaped = format.charAt(position);
            switch (escaped) {
                case 't': addLiteral(fields, '\t'); break;
                case 'b': addLiteral(fields, '\b'); break;
                case 'n': addLiteral(fields, '\n'); break;
                case 'r': addLiteral(fields, '\r'); break;
                case 'f': addLiteral(fields, '\f'); break;
                default:  addLiteral(fields, escaped);
            }
            return position;
        }
        throw new IllegalArgumentException("Unterminated escape sequence in [" + format + "] at character " + position);
    }

    /* ====================================================================== */

    private static List<Field> addLiteral(List<Field> fields, char c) {
        /* See if we can add to the previuos literal field */
        if (!fields.isEmpty()) {
            final Field last = fields.get(fields.size() - 1);
            if (last instanceof LiteralField) {
                ((LiteralField) last).append(c);
                return fields;
            }
        }

        /* List empty or last field was not a literal, add a new one */
        fields.add(new LiteralField(c, true));
        return fields;
    }

    /* ====================================================================== */
    /* FIELD DECLARATIONS                                                     */
    /* ====================================================================== */

    private static abstract class Field {

        abstract StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos);

        @Override
        public abstract String toString();

    }

    private static abstract class AbstractField extends Field {
        private final char format;
        private final String parameter;

        protected AbstractField(char format) {
            this(format, null);
        }

        protected AbstractField(char format, String parameter) {
            this.format = format;
            this.parameter = parameter;
        }

        @Override
        public final String toString() {
            final StringBuilder builder = new StringBuilder().append('%');
            if (parameter != null) builder.append('{').append(parameter).append('}');
            return builder.append(format).toString();
        }
    }

    /* ====================================================================== */

    private abstract static class HeaderField extends AbstractField {

        final String name;

        HeaderField(char format, String name) {
            super(format, name.trim().toLowerCase());
            this.name = name.trim().toLowerCase();
        }

        StringBuilder format(StringBuilder builder, MimeHeaders headersx) {
            final Iterator<String> headers = headersx.values(name).iterator();
            if (headers.hasNext()) builder.append(headers.next());
            while (headers.hasNext()) builder.append("; ").append(headers.next());
            return builder;
        }
    }

    /* ====================================================================== */

    private static class LiteralField extends Field {

        final StringBuilder contents;

        LiteralField(char character, boolean crack) {
            contents = new StringBuilder().append(character);
        }

        void append(char character) {
            contents.append(character);
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(contents);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            for (int x = 0; x < contents.length(); x ++) {
                final char character = contents.charAt(x);
                /* Escape \t \b \n \r \f and %% */
                switch(character) {
                    case 't': case 'b': case 'n': case 'r': case 'f':
                        builder.append('\\'); break;
                    case '%':
                        builder.append('%'); break;
                }
                builder.append(character);
            }
            return builder.toString();
        }
    }

    /* ====================================================================== */

    private static class ServerNameField extends AbstractField {

        ServerNameField() {
            super('v');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getServerName());
        }
    }

    /* ====================================================================== */

    private static class LocalHostField extends AbstractField {

        LocalHostField() {
            super('h', "local");
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getLocalName());
        }
    }

    /* ====================================================================== */

    private static class LocalAddressField extends AbstractField {

        LocalAddressField() {
            super('A');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getLocalAddr());
        }
    }

    /* ====================================================================== */

    private static class LocalPortField extends AbstractField {

        LocalPortField() {
            super('p');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getLocalPort());
        }
    }

    /* ====================================================================== */

    private static class RemoteHostField extends AbstractField {

        RemoteHostField() {
            super('h');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getRemoteHost());
        }
    }

    /* ====================================================================== */

    private static class RemoteAddressField extends AbstractField {

        RemoteAddressField() {
            super('a');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getRemoteAddr());
        }
    }

    /* ====================================================================== */

    private static class RemotePortField extends AbstractField {

        RemotePortField() {
            super('p', "remote");
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getRemotePort());
        }
    }

    /* ====================================================================== */

    private static class RequestTimeField extends Field {

        private static final String DEFAULT_FORMAT = "[yyyy/MMM/dd:HH:mm:ss Z]";
        private final SimpleDateFormatThreadLocal simpleDateFormat;
        private final String format;

        RequestTimeField(String format) {
            this.format = format == null ? DEFAULT_FORMAT : format;
            simpleDateFormat = new SimpleDateFormatThreadLocal(this.format);
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(timeStamp == null ? "-" : simpleDateFormat.get().format(timeStamp));
        }

        @Override
        public String toString() {
            return DEFAULT_FORMAT.equals(format) ? "%t" : "%{" + format + "}";
        }
    }

    /* ====================================================================== */

    private static class RequestMethodField extends AbstractField {

        RequestMethodField() {
            super('m');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getMethod());
        }
    }

    /* ====================================================================== */

    private static class RequestUserField extends AbstractField {

        RequestUserField() {
            super('u');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            final String user = request.getRemoteUser();
            return builder.append(user == null ? "-" : user);
        }
    }

    /* ====================================================================== */

    private static class RequestURIField extends AbstractField {

        RequestURIField() {
            super('U');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return builder.append(request.getRequestURI());
        }
    }

    /* ====================================================================== */

    private static class RequestQueryField extends AbstractField {

        RequestQueryField() {
            super('q');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            final String query = request.getQueryString();
            if (query != null) builder.append('?').append(query);
            return builder;
        }
    }

    /* ====================================================================== */

    private static class RequestProtocolField extends AbstractField {

        RequestProtocolField() {
            super('H');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            final Protocol protocol = request.getProtocol();
            if (protocol == null) return builder.append("-");
            switch (protocol) {
                case HTTP_0_9: return builder.append("HTTP/0.9");
                case HTTP_1_0: return builder.append("HTTP/1.0");
                case HTTP_1_1: return builder.append("HTTP/1.1");
                default: return builder.append("-");
            }
        }
    }

    /* ====================================================================== */

    private static class RequestHeaderField extends HeaderField {

        RequestHeaderField(String name) {
            super('i', name);
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return this.format(builder, request.getRequest().getHeaders());
        }
    }

    /* ====================================================================== */

    private static class RequestCookieField extends AbstractField {

        final String name;

        RequestCookieField(String name) {
            super('C', name.trim().toLowerCase());
            this.name = name.trim().toLowerCase();
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            for (Cookie cookie: request.getCookies()) {
                if (name.equals(cookie.getName().toLowerCase())) {
                    return builder.append(cookie.getValue());
                }
            }
            return builder;
        }
    }

    /* ====================================================================== */

    private static class ResponseStatusField extends AbstractField {

        ResponseStatusField() {
            super('s');
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            final int status = response.getStatus();
            if (status < 10) builder.append('0');
            if (status < 100) builder.append('0');
            return builder.append(status);
        }
    }

    /* ====================================================================== */

    private static class ResponseSizeField extends AbstractField {

        final String zero;

        ResponseSizeField(boolean zero) {
            super(zero ? 'B' : 'b');
            this.zero = zero ? "0" : "-";
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            final long size = response.getContentLengthLong();
            return builder.append(size < 1 ? zero : Long.toString(size));
        }
    }

    /* ====================================================================== */

    private static class ResponseTimeField extends Field {

        private final long scale;

        ResponseTimeField(String unit, String format, int position) {
            /* Figure out the scale */
            if (unit == null) {
                scale = 1000000000;
                return;
            }

            switch (unit.trim().toLowerCase()) {
                case "n": case "nano":  case "nanos":  case "nanosec":  case "nanosecs":  case "nanosecond":  case "nanoseconds":  scale = 1; break;
                /* %D */  case "micro": case "micros": case "microsec": case "microsecs": case "microsecond": case "microseconds": scale = 1000; break;
                case "m": case "milli": case "millis": case "millisec": case "millisecs": case "millisecond": case "milliseconds": scale = 1000000; break;
                /* %T */                case      "s": case      "sec": case      "secs": case      "second": case      "seconds": scale = 1000000000; break;
                default:
                    throw new IllegalArgumentException("Unsupported time unit \"" + unit + "\" for field 'T' in [" + format + "] at character " + position);
            }
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            if (responseNanos < 0) return builder.append('-');
            return builder.append(responseNanos / scale);
        }

        @Override
        public String toString() {
            final StringBuilder string = new StringBuilder().append('%');
            if      (scale          == 1) string.append('T');
            else if (scale       == 1000) string.append("{m}T");
            else if (scale    == 1000000) string.append("D");
            else if (scale == 1000000000) string.append("{n}T");
            else string.append('{').append(scale).append("}T");
            return string.toString();
        }
    }

    /* ====================================================================== */

    private static class ResponseHeaderField extends HeaderField {

        ResponseHeaderField(String name) {
            super('o', name);
        }

        @Override
        StringBuilder format(StringBuilder builder, Request request, Response response, Date timeStamp, long responseNanos) {
            return this.format(builder, response.getResponse().getHeaders());
        }
    }
}
