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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.usrz.libs.logging.Log;

public class Configurations implements Map<String, String>, Cloneable {

    private static final Pattern NAME_PATTERN = Pattern.compile("^([\\w-]+(\\.[\\w-]+)*)?$");
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final Log log = new Log();

    private final Map<String, String> configurations;

    /* ====================================================================== */
    /* CONSTRUCTION                                                           */
    /* ====================================================================== */

    private Configurations(Map<?, ?> map, boolean checkNames) {
        if (map == null) throw new NullPointerException("Null map");

        /* Do we *really* have to check names? */
        if ((checkNames) && (map instanceof Configurations)) checkNames = false;

        /* Prepare our map where key/values will be copied into */
        final Map<String, String> configurations = new HashMap<>();

        /* Iterate through the given map */
        for (Entry<?, ?> entry: map.entrySet()) {
            final String key = key(entry.getKey());
            final Object object = entry.getValue();

            if (!NAME_PATTERN.matcher(key).matches())
                log.warn("Non-standard configuration key \"%s\"", key);

            /* Null or empty values? */
            if (object == null) {
                log.debug("Null value in map for key \"%s\", ignoring...", key);
                continue;
            }
            final String value = object.toString().trim();
            if (value.length() == 0) {
                log.debug("Empty value in map for key \"%s\", ignoring...", key);
                continue;
            }

            /* Remember this mapping */
            configurations.put(key, value);
        }

        /* All done! */
        this.configurations = Collections.unmodifiableMap(configurations);
    }

    /* ====================================================================== */

    public Configurations(Map<?, ?> map) {
        this(map, true);
    }

    /* ====================================================================== */
    /* MERGING AND OVERRIDING                                                 */
    /* ====================================================================== */

    public final Configurations merge(Map<?, ?> map) {
        final Map<String, String> configurations = new HashMap<>();
        configurations.putAll(map instanceof Configurations ?
                                  (Configurations) map :
                                  new Configurations(map, true));
        configurations.putAll(this);
        return new Configurations(configurations, false);
    }

    public final Configurations override(Map<?, ?> map) {
        final Map<String, String> configurations = new HashMap<>();
        configurations.putAll(this);
        configurations.putAll(map instanceof Configurations ?
                                  (Configurations) map :
                                  new Configurations(map, true));
        return new Configurations(configurations, false);
    }

    /* ====================================================================== */
    /* EXTRACTING BY PREFIX AND REMAPPING BY ADDING/REMOVING PREFIXES         */
    /* ====================================================================== */

    public final Configurations prefix(String prefix) {

        /* Check and normalize the prefix */
        if (prefix == null) throw new NullPointerException("Null prefix");
        if (!prefix.endsWith(".")) prefix += ".";

        /* Remap adding the new prefix */
        final Map<String, String> configurations = new HashMap<>();
        for (Entry<String, String> entry: entrySet())
            configurations.put(prefix + entry.getKey(), entry.getValue());

        /* All done */
        return new Configurations(configurations, false);
    }

    public final Configurations strip(String prefix) {

        /* Check and normalize the prefix */
        if (prefix == null) throw new NullPointerException("Null prefix");
        while (prefix.endsWith(".")) prefix = prefix.substring(0, prefix.length() - 1);
        final String prefixDot = prefix + ".";
        final int prefixLen = prefixDot.length();

        /* Remap stripping the prefix */
        final Map<String, String> configurations = new HashMap<>();
        for (Entry<String, String> entry: entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (prefix.equals(key)) configurations.put("", value);
            if (key.startsWith(prefixDot))
                configurations.put(key.substring(prefixLen, key.length()), value);
        }

        /* All done */
        return new Configurations(configurations, false);
    }

    public final Configurations extract(String prefix) {

        /* Check and normalize the prefix */
        if (prefix == null) throw new NullPointerException("Null prefix");
        while (prefix.endsWith(".")) prefix = prefix.substring(0, prefix.length() - 1);
        final String prefixDot = prefix + ".";

        /* Remap stripping the prefix */
        final Map<String, String> configurations = new HashMap<>();
        for (Entry<String, String> entry: entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (prefix.equals(key) || key.startsWith(prefixDot))
                configurations.put(key, value);
        }

        /* All done */
        return new Configurations(configurations, false);
    }

    /* ====================================================================== */
    /* SUB-PROPERTIES EXTRACTION/GROUPING                                     */
    /* ====================================================================== */

    public final Map<String, Configurations> group(String prefix) {

        /* Check and normalize */
        if (prefix == null) throw new NullPointerException("Null prefix");
        if (!prefix.endsWith(".")) prefix += ".";

        /* Do we have anything to tokenize? */
        final Set<String> groups = new HashSet<>();
        for (String key: keySet()) {
            if (key.startsWith(prefix)) {
                int position = key.indexOf('.', prefix.length());
                if (position < 0) position = key.length();
                groups.add(key.substring(prefix.length(), position));
            }
        }

        /* Extract the various sub-configurations */
        final Map<String, Configurations> grouped = new HashMap<>();
        for (String group: groups) {
            grouped.put(group, strip(prefix + group));
        }

        /* All done, return our map, made unmodifiable */
        return Collections.unmodifiableMap(grouped);

    }

    public final Map<String, Configurations> explicitGrouping(String key) {

        /* Check and normalize the prefix */
        if (key == null) throw new NullPointerException("Null key");
        while (key.endsWith(".")) key = key.substring(0, key.length() - 1);

        final String value = this.get(key);
        if (value == null) return Collections.emptyMap();

        final StringTokenizer tokenizer = new StringTokenizer(value, " \t\r\n,");
        final Set<String> groups = new HashSet<>();
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken().trim();
            if (token.length() > 0) groups.add(token);
        }

        final Map<String, Configurations> grouped = group(key);
        final Map<String, Configurations> explicit = new HashMap<>();
        for (String group: groups) {
            if (grouped.containsKey(group)) explicit.put(group, grouped.get(group));
        }

        /* All done, return our map, made unmodifiable */
        return Collections.unmodifiableMap(explicit);
    }

    /* ====================================================================== */
    /* BASIC GET METHODS                                                      */
    /* ====================================================================== */

    @Override
    public final String get(Object key) {
        return getString(key, null);
    }

    public final String get(Object key, String defaultValue) {
        return getString(key, defaultValue);
    }

    public final String getString(Object key) {
        return getString(key, null);
    }

    public final String getString(Object key, String defaultValue) {
        final String value = configurations.get(key(key));
        return value == null ? defaultValue : value;
    }
    /* ====================================================================== */
    /* CONVERSION METHODS                                                     */
    /* ====================================================================== */

    public final File getFile(Object key) {
        return this.getFile(key, null);
    }

    public final URI getURI(Object key) {
        return this.getURI(key, null);
    }

    public final URL getURL(Object key) {
        return this.getURL(key, null);
    }

    /* ====================================================================== */

    public final File getFile(Object key, File defaultValue) {
        return this.get(key, defaultValue);
    }

    public final URI getURI(Object key, URI defaultValue) {
        return this.get(key, defaultValue);
    }

    public final URL getURL(Object key, URL defaultValue) {
        return this.get(key, defaultValue);
    }

    /* ====================================================================== */

    public final File get(Object key, File defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : new File(value);
    }

    public final URL get(Object key, URL defaultValue) {
        final String value = this.get(key);
        try {
            return value == null ? defaultValue : new URL(value);
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Unvalid URL " + value, exception);
        }
    }

    public final URI get(Object key, URI defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : URI.create(value);
    }

    /* ====================================================================== */
    /* PRIMITIVES METHODS                                                     */
    /* ====================================================================== */

    public final byte get(Object key, byte defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : Byte.parseByte(value);
    }

    public final short get(Object key, short defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : Short.parseShort(value);
    }

    public final int get(Object key, int defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public final long get(Object key, long defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    public final float get(Object key, float defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : Float.parseFloat(value);
    }

    public final double get(Object key, double defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : Double.parseDouble(value);
    }

    public final boolean get(Object key, boolean defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    /* ====================================================================== */
    /* AUTOBOXING METHODS                                                     */
    /* ====================================================================== */

    public final Byte getByte(Object key) {
        return this.getByte(key, null);
    }

    public final Short getShort(Object key) {
        return this.getShort(key, null);
    }

    public final Integer getInteger(Object key) {
        return this.getInteger(key, null);
    }

    public final Long getLong(Object key) {
        return this.getLong(key, null);
    }

    public final Float getFloat(Object key) {
        return this.getFloat(key, null);
    }

    public final Double getDouble(Object key) {
        return this.getDouble(key, null);
    }

    public final Boolean getBoolean(Object key) {
        return this.getBoolean(key, null);
    }

    /* ====================================================================== */

    public final Byte getByte(Object key, Byte defaultValue) {
        final String value = this.get(key);
        if (value == null) return defaultValue;
        return Byte.parseByte(value);
    }

    public final Short getShort(Object key, Short defaultValue) {
        final String value = this.get(key);
        if (value == null) return defaultValue;
        return Short.parseShort(value);
    }

    public final Integer getInteger(Object key, Integer defaultValue) {
        final String value = this.get(key);
        if (value == null) return defaultValue;
        return Integer.parseInt(value);
    }

    public final Long getLong(Object key, Long defaultValue) {
        final String value = this.get(key);
        if (value == null) return defaultValue;
        return Long.parseLong(value);
    }

    public final Float getFloat(Object key, Float defaultValue) {
        final String value = this.get(key);
        if (value == null) return defaultValue;
        return Float.parseFloat(value);
    }

    public final Double getDouble(Object key, Double defaultValue) {
        final String value = this.get(key);
        if (value == null) return defaultValue;
        return Double.parseDouble(value);
    }

    public final Boolean getBoolean(Object key, Boolean defaultValue) {
        final String value = this.get(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    /* ====================================================================== */
    /* UNMODIFIABLE COLLECTION METHODS                                        */
    /* ====================================================================== */

    @Override
    public final Set<String> keySet() {
        return Collections.unmodifiableSet(configurations.keySet());
    }

    @Override
    public final Collection<String> values() {
        return Collections.unmodifiableCollection(configurations.keySet());
    }

    @Override
    public final Set<Entry<String, String>> entrySet() {
        return Collections.unmodifiableSet(configurations.entrySet());
    }

    /* ====================================================================== */
    /* DELEGATED METHODS                                                      */
    /* ====================================================================== */

    @Override
    public final int size() {
        return configurations.size();
    }

    @Override
    public final boolean isEmpty() {
        return configurations.isEmpty();
    }

    @Override
    public final boolean containsKey(Object key) {
        return configurations.containsKey(key(key));
    }

    @Override
    public final boolean containsValue(Object value) {
        return configurations.containsValue(value);
    }

    /* ====================================================================== */
    /* DUMPING/SAVING                                                         */
    /* ====================================================================== */

    public Configurations save(OutputStream output)
    throws IOException {
        return this.save(new OutputStreamWriter(output, UTF8));
    }

    public Configurations save(Writer writer)
    throws IOException {
        final Set<String> sorted = new TreeSet<>(keySet());
        for (String key: sorted) {
            writer.write(key);
            writer.write(" = ");
            writer.write(this.get(key));
            writer.write(LINE_SEPARATOR);
        }
        writer.flush();
        writer.close();
        return this;
    }

    /* ====================================================================== */
    /* OBJECT METHODS                                                         */
    /* ====================================================================== */

    @Override
    public final int hashCode() {
        return configurations.hashCode() ^ Configurations.class.hashCode();
    }

    @Override
    public Configurations clone() {
        return new Configurations(configurations, false);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (object == null) return false;
        try {
            return configurations.equals(((Configurations) object).configurations);
        } catch (ClassCastException exception) {
            return false;
        }
    }

    /* ====================================================================== */
    /* UNSUPPORTED METHODS                                                    */
    /* ====================================================================== */

    @Override
    public final String put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final String remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    /* ====================================================================== */
    /* KEY MAPPING                                                            */
    /* ====================================================================== */

    private static String key(Object key) {
        if (key == null) return "";
        try {
            return ((String) key).trim();
        } catch (ClassCastException exception) {
            return key.toString().trim();
        }
    }

}
