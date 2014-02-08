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

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.inject.Singleton;

@Singleton
public final class SSLPasswordCallback {

    private SSLContextInitializer sslContextInitializer = null;

    public SSLPasswordCallback() {
        super();
    }

    public void setPassword(char[] keyPassword, char[] keyStorePassword, char[] trustStorePassword)
    throws IOException, GeneralSecurityException {
        if (sslContextInitializer == null) throw new GeneralSecurityException("SSL context initalizer unavailable");
        sslContextInitializer.init(keyPassword, keyStorePassword, trustStorePassword);
    }

    public void setSSLContextInitializer(SSLContextInitializer sslContextInitializer)
    throws IllegalStateException {
        if (this.sslContextInitializer != null) throw new IllegalStateException("SSL context initalizer already set");
        this.sslContextInitializer = sslContextInitializer;
    }
}
