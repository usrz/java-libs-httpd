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
package org.usrz.libs.httpd;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;

import org.glassfish.grizzly.http.server.HttpHandler;

import com.google.inject.BindingAnnotation;

class Handlers {

    @BindingAnnotation
    @Retention(RUNTIME)
    @interface At {

        /**
         * The context path where the associated {@link HttpHandler} will be
         * deployed under.
         */
        public String path();

    }

    static final At at(String path) {
        return new AtImpl(path);
    }

    /* ====================================================================== */

    @SuppressWarnings("all")
    private static class AtImpl implements At {

        private final String path;

        private AtImpl(String path) {
            if (path == null) throw new NullPointerException("Null path");
            path = ("/" + path).replaceAll("/+", "/");
            path += path.endsWith("/") ? "*" : "/*";
            this.path = path;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public int hashCode() {
            // This is specified in java.lang.Annotation.
            return (127 * "value".hashCode()) ^ path.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            try {
                return path.equals(((At)object).path());
            } catch (ClassCastException exception) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "@" + At.class.getName() + "(value=" + path + ")";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return At.class;
        }
    }


}
