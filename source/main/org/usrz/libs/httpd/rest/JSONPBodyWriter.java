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

import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class JSONPBodyWriter implements MessageBodyWriter<JSONP> {

    private final ObjectMapper mapper;

    @Inject
    public JSONPBodyWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override @Deprecated
    public long getSize(JSONP t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (JSONP.class.isAssignableFrom(type)) {
            if (JSONP.JSONP_TYPE.equals(mediaType)) return true;
            mediaType = new MediaType(mediaType.getType(), mediaType.getSubtype());
            if (JSONP.JSONP_TYPE.equals(mediaType)) return true;
        }
        return false;
    }

    @Override
    public void writeTo(JSONP jsonp, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream stream)
    throws IOException, WebApplicationException {
        final String charset = mediaType.getParameters().get(CHARSET_PARAMETER);
        final Writer writer = new ProtectingWriter(stream, charset);

        writer.write(jsonp.getCallback());
        writer.write('(');
        mapper.writer().writeValue(writer, jsonp);
        writer.write(");");
        writer.flush();
        stream.flush();
    }

    /* ====================================================================== */

    private final class ProtectingWriter extends OutputStreamWriter {

        public ProtectingWriter(OutputStream out, String charset)
        throws UnsupportedEncodingException {
            super(out, charset == null ? "UTF8" : charset);
        }

        @Override
        public void close() {
            /* Ignore me! */
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            super.write(cbuf);
        }

        @Override
        public void write(String str) throws IOException {
            super.write(str);
        }

        @Override
        public String getEncoding() {
            return super.getEncoding();
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            return super.append(csq);
        }

        @Override
        public void write(int c) throws IOException {
            super.write(c);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            super.write(cbuf, off, len);
        }

        @Override
        public Writer append(CharSequence csq, int start, int end)
                throws IOException {
            return super.append(csq, start, end);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            super.write(str, off, len);
        }

        @Override
        public void flush() throws IOException {
            super.flush();
        }

        @Override
        public Writer append(char c) throws IOException {
            return super.append(c);
        }
    }
}
