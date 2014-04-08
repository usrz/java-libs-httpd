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
package org.usrz.libs.httpd.inject;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.accesslog.AccessLogBuilder;
import org.glassfish.grizzly.http.server.accesslog.AccessLogProbe;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.logging.Log;

public class AccessLogProvider implements Provider<AccessLogProbe> {

    private final Log log = new Log();
    private final AccessLogProbe probe;
    private final File accessLog;

    public AccessLogProvider(Configurations configurations) {
        accessLog = configurations.requireFile("file");

        /* Start building our access log */
        final AccessLogBuilder accessLogBuilder = new AccessLogBuilder(accessLog);

        /* Configure log rotation, if necessary */
        final String rotate = configurations.getString("rotate", null);
        if (rotate != null) switch (rotate.toLowerCase()) {
            case "daily":  accessLogBuilder.rotatedDaily(); break;
            case "hourly": accessLogBuilder.rotatedHourly(); break;
            default: throw new IllegalStateException("Unsupported value \"" + rotate + "\" for parameter \"rotate\"");
        } else {
            final String rotationPattern = configurations.getString("rotationPattern");
            if (rotationPattern != null) accessLogBuilder.rotationPattern(rotationPattern);
        }

        /* Synchronous log? (not by default) */
        accessLogBuilder.synchronous(configurations.get("synchronous", false));

        /* Set the format, if we have to */
        final String format = configurations.getString("format", null);
        if (format != null) accessLogBuilder.format(format);

        /* Finally configure time zone, if we need to... */
        final String timezone = configurations.getString("timezone", null);
        if (timezone != null) accessLogBuilder.timeZone(timezone);

        /* Great! Instrument our server configurations */
        probe = accessLogBuilder.build();
    }

    public String getName() {
        return accessLog.getAbsolutePath();
    }

    @Inject
    private void setup(HttpServer server) {
        final ServerConfiguration configuration = server.getServerConfiguration();
        configuration.getMonitoringConfig().getWebServerConfig().addProbes(probe);

        log.info("Configured access log writing to \"%s\" on server \"%s\"", accessLog, configuration.getName());
    }

    @Override
    public AccessLogProbe get() {
        return probe;
    }

}
