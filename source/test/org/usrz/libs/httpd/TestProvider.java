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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.testng.Assert;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.httpd.inject.HttpServerConfigurations;

@Provider
@Singleton
@HttpServerConfigurations
public class TestProvider implements MessageBodyWriter<Method> {

    @Inject
    private TestProvider(@HttpServerConfigurations Configurations configurations1,
                         @Named("foobar") Map<String, Integer> map,
                         Configurations configurations2) {
        /* This will be constructed up at initialization of the app, before any request is done */
        Assert.assertFalse(configurations1.extract("listeners").isEmpty()); /* HTTP configs have listeners */
        Assert.assertTrue(configurations2.containsKey("conf")); /* All applications have a "conf" key */
        Assert.assertSame(map, ServerBuilderTest.DEPENDENCY); /* Check we got the *SAME* map */
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return false;
    }

    @Override
    public long getSize(Method t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Method t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        throw new UnsupportedOperationException("Foo!");
    }
}
