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
package org.usrz.libs.httpd.configurations;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * A {@link Configurations} implementation reading <em>key-value</em> mappings
 * from <em><a href="http://json.org/">JSON</a>-like file</em>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class JsonConfigurations extends Configurations {

    /* Platform-dependant line separator to wrap our JSON */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    /* Our charset, UTF8, always */
    private static final Charset UTF8 = Charset.forName("UTF8");
    /* Our JSON factory, to create parsers from */
    private static final JsonFactory JSON_FACTORY;

    /* Initialize our JSON factory */
    static {
        JSON_FACTORY = new JsonFactory();
        JSON_FACTORY.enable(JsonParser.Feature.ALLOW_COMMENTS);
        JSON_FACTORY.enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
        JSON_FACTORY.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        JSON_FACTORY.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        JSON_FACTORY.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
    }

    /* ====================================================================== */
    /* CONSTRUCTION                                                           */
    /* ====================================================================== */

    /**
     * Create a new {@link JsonConfigurations} instance reading a
     * <em><a href="http://json.org/">JSON</a>-like file</em> from the
     * specified {@link Reader}.
     */
    public JsonConfigurations(Reader reader) {
        super(load(reader), false);
    }

    /**
     * Create a new {@link JsonConfigurations} instance reading a
     * <em><a href="http://json.org/">JSON</a>-like file</em> from the
     * specified {@link InputStream}.
     */
    public JsonConfigurations(InputStream input) {
        super(load(input), false);
    }

    /* ====================================================================== */

    private static final Map<String, Object> load(InputStream input) {
        if (input == null) throw new NullPointerException("Null input stream");
        return load(new InputStreamReader(input, UTF8));
    }

    private static final Map<String, Object> load(Reader reader) {
        try {
            return parse(reader);
        } catch (ConfigurationsException exception) {
            throw exception.unchecked();
        } catch (IOException exception) {
            throw new IllegalStateException("I/O error reading JSON", exception);
        }
    }

    /* ====================================================================== */

    static final Map<String, Object> parse(InputStream input)
    throws ConfigurationsException, IOException {
        if (input == null) throw new NullPointerException("Null input stream");
        return parse(new InputStreamReader(input, UTF8));
    }

    static final Map<String, Object> parse(Reader reader)
    throws ConfigurationsException, IOException {
        if (reader == null) throw new NullPointerException("Null reader");

        /* Read our JSON fully, wrapping it in a { json } structure */
        final StringBuilder builder = new StringBuilder("{").append(LINE_SEPARATOR);
        final char[] buffer = new char[4096];
        int read = -1;
        while ((read = reader.read(buffer)) >= 0) {
            if (read > 0) builder.append(buffer, 0, read);
        }

        /* Return our JSON as a string */
        final String json = builder.append(LINE_SEPARATOR).append('}').toString();

        /* Build our Map holding the various configurations items */
        final Map<String, Object> configurations = new HashMap<>();

        try {
            /* Initialize parsing */
            final JsonParser parser = JSON_FACTORY.createParser(json);
            JsonToken token;
            Name name = new Name();

            while ((token = parser.nextToken()) != null) {
                final JsonLocation location = parser.getTokenLocation();

                switch (token) {
                    /* At the start of an object, get a child name */
                    case START_OBJECT: name = name.child(); break;
                    /* Ad the end of an object, get the parent name */
                    case END_OBJECT: name = name.parent(); break;

                    /* When we find a field we update the name */
                    case FIELD_NAME: name.next(parser.getCurrentName(), location); break;

                    /* When we start an array we start auto-incrementing */
                    case START_ARRAY: name.array(true); break;
                    /* When we end an array we stop auto-incrementing */
                    case END_ARRAY: name.array(false); break;

                    /* Value mapping, one per type */
                    case VALUE_NULL:         break;
                    case VALUE_TRUE:         configurations.put(name.name(), true); break;
                    case VALUE_FALSE:        configurations.put(name.name(), false); break;
                    case VALUE_NUMBER_FLOAT: configurations.put(name.name(), parser.getValueAsDouble()); break;
                    case VALUE_NUMBER_INT:   configurations.put(name.name(), parser.getValueAsLong()); break;
                    case VALUE_STRING:       configurations.put(name.name(), parser.getValueAsString()); break;

                    /* All the rest is actually unsupported */
                    case NOT_AVAILABLE:
                    case VALUE_EMBEDDED_OBJECT:
                    default:
                        System.err.println("Unsupported token " + token);
                }
            }

            /* Close parser and stream */
            parser.close();

        } catch (JsonParseException exception) {
            /* Wrap a JsonParseException in a ConfigurationsException */
            final ConfigurationsException wrapper = new ConfigurationsException("Unable to parse JSON format", true);
            final JsonLocation location = exception.getLocation();
            if (location != null) wrapper.initLocation(location.getLineNr(), location.getColumnNr());
            throw wrapper.initCause(exception);
        }

        return configurations;
    }

    /* ====================================================================== */
    /* CONFIGURATIONS KEY NAMES "BUILDER"... OF SORTS                         */
    /* ====================================================================== */

    private static final class Name {

        private Name parent;
        private String name = "";
        private boolean array;
        private int index;

        public void next(String name, JsonLocation location)
        throws ConfigurationsException {
            this.name = parent == null ? name : parent.internalName() + "." + name;
            try {
                Configurations.validateKey(internalName().substring(1));
            } catch (ConfigurationsException exception) {
                throw exception.initLocation(location.getLineNr() - 1, location.getColumnNr());
            }
        }

        public void array(boolean array) {
            if (this.array == array) return;
            if (array) {
                index = 0;
                this.array = true;
            } else {
                this.array = false;
                index = 0;
            }
        }

        public String name() {
            if (array) index++;
            return internalName().substring(1);
        }

        private String internalName() {
            return array ? name + "." + index : name;
        }

        public Name child() {
            final Name child = new Name();
            child.parent = this;
            return child;
        }

        public Name parent() {
            if (parent.array) parent.index++;
            return parent;
        }

    }
}
