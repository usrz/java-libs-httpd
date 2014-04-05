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
package org.usrz.libs.httpd.rest;

import javax.inject.Inject;

import org.glassfish.hk2.api.ServiceLocator;
import org.usrz.libs.logging.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class ResourceSupport
implements InjectionSupport, RestSupport, AsyncRestSupport {

    protected static final Log log = new Log();

    private ServiceLocator locator;
    private ObjectMapper mapper;

    protected ResourceSupport() {
        /* Nothing to do */
    }

    /* ====================================================================== */

    @Inject
    private void setServiceLocator(ServiceLocator locator) {
        this.locator = locator;
    }

    @Inject
    private void setObjectMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /* ====================================================================== */

    @Override
    public final ServiceLocator getServiceLocator() {
        return locator;
    }

    @Override
    public final ObjectMapper getObjectMapper() {
        return mapper;
    }

}
