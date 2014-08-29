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
package org.usrz.libs.httpd.jersey;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.ws.rs.core.Application;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;

public class GrizzlyHttpContainerFactory {

    private GrizzlyHttpContainerFactory() {
        throw new IllegalStateException("Do not construct");
    }

    public static GrizzlyHttpContainer create(final Application application) {
        return create(new Class<?>[]{ Application.class },
                      new Object[]  { application });
    }

    public static GrizzlyHttpContainer create(final Application application, final ServiceLocator parentLocator) {
        return create(new Class<?>[]{ Application.class, ServiceLocator.class },
                      new Object[]  { application,       parentLocator });
    }

    private static GrizzlyHttpContainer create(Class<?>[] types, Object[] arguments) {
        try {
            final Constructor<GrizzlyHttpContainer> constructor =
                    GrizzlyHttpContainer.class.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException exception) {
            final Throwable cause = exception.getCause();
            if (cause != null) {
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new IllegalStateException("Unable to construct GrizzlyHttpContainer", cause);
            } else {
                throw new IllegalStateException("Unable to construct GrizzlyHttpContainer", cause);
            }
        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException exception) {
            throw new IllegalStateException("Exception invoking GrizzlyHttpContainer constructor", exception);
        }
    }
}
