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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.testng.annotations.Test;
import org.usrz.libs.testing.AbstractTest;

public class ConfigurationsTest extends AbstractTest {

    @Test
    public void testBasics()
    throws Exception {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        assertEquals(configurations.get("value.missing"),                       null);
        assertEquals(configurations.get("value.string"),                        "a string");
        assertEquals(configurations.get("value.string",  "wrong string"),       "a string");
        assertEquals(configurations.get("value.boolean", false),                true);
        assertEquals(configurations.get("value.integer", 456),                  123);
        assertEquals(configurations.get("value.integer", (byte) 1),             123);
        assertEquals(configurations.get("value.integer", (long) 1),             123);
        assertEquals(configurations.get("value.integer", (short) 1),            123);
        assertEquals(configurations.get("value.real",    456.123F),             123.456F);
        assertEquals(configurations.get("value.real",    456.123D),             123.456D);
        assertEquals(configurations.get("value.uri",     new URI("foo:wrong")), new URI("opaque:uri"));
        assertEquals(configurations.get("value.url",     new URL("http://g")),  new URL("http://www.google.com/"));
        assertEquals(configurations.get("value.file",    new File("/bin")),     new File("/etc"));

        assertEquals(configurations.get("value.missing", "wrong string"),       "wrong string");
        assertEquals(configurations.get("value.missing", false),                false);
        assertEquals(configurations.get("value.missing", 456),                  456);
        assertEquals(configurations.get("value.missing", (byte) 1),             (byte) 1);
        assertEquals(configurations.get("value.missing", (long) 1),             1);
        assertEquals(configurations.get("value.missing", (short) 1),            (short) 1);
        assertEquals(configurations.get("value.missing", 456.123F),             456.123F);
        assertEquals(configurations.get("value.missing", 456.123D),             456.123D);
        assertEquals(configurations.get("value.missing", new URI("foo:wrong")), new URI("foo:wrong"));
        assertEquals(configurations.get("value.missing", new URL("http://g")),  new URL("http://g"));
        assertEquals(configurations.get("value.missing", new File("/bin")),     new File("/bin"));

        assertEquals(configurations.getString ("value.string",  "wrong string"),       "a string");
        assertEquals(configurations.getBoolean("value.boolean", false),                Boolean.TRUE);
        assertEquals(configurations.getInteger("value.integer", 456),                  new Integer(123));
        assertEquals(configurations.getByte   ("value.integer", (byte) 1),             new Byte((byte)123));
        assertEquals(configurations.getLong   ("value.integer", (long) 1),             new Long(123));
        assertEquals(configurations.getShort  ("value.integer", (short) 1),            new Short((short)123));
        assertEquals(configurations.getFloat  ("value.real",    456.123F),             new Float(123.456F));
        assertEquals(configurations.getDouble ("value.real",    456.123D),             new Double(123.456D));
        assertEquals(configurations.getURI    ("value.uri",     new URI("foo:wrong")), new URI("opaque:uri"));
        assertEquals(configurations.getURL    ("value.url",     new URL("http://g")),  new URL("http://www.google.com/"));
        assertEquals(configurations.getFile   ("value.file",    new File("/bin")),     new File("/etc"));

        assertEquals(configurations.getString ("value.string"),  "a string");
        assertEquals(configurations.getBoolean("value.boolean"), Boolean.TRUE);
        assertEquals(configurations.getInteger("value.integer"), new Integer(123));
        assertEquals(configurations.getByte   ("value.integer"), new Byte((byte)123));
        assertEquals(configurations.getLong   ("value.integer"), new Long(123));
        assertEquals(configurations.getShort  ("value.integer"), new Short((short)123));
        assertEquals(configurations.getFloat  ("value.real"),    new Float(123.456F));
        assertEquals(configurations.getDouble ("value.real"),    new Double(123.456D));
        assertEquals(configurations.getURI    ("value.uri"),     new URI("opaque:uri"));
        assertEquals(configurations.getURL    ("value.url"),     new URL("http://www.google.com/"));
        assertEquals(configurations.getFile   ("value.file"),    new File("/etc"));

        assertEquals(configurations.getString ("value.missing", "wrong string"),       "wrong string");
        assertEquals(configurations.getBoolean("value.missing", false),                Boolean.FALSE);
        assertEquals(configurations.getInteger("value.missing", 456),                  new Integer(456));
        assertEquals(configurations.getByte   ("value.missing", (byte) 1),             new Byte((byte) 1));
        assertEquals(configurations.getLong   ("value.missing", (long) 1),             new Long(1));
        assertEquals(configurations.getShort  ("value.missing", (short) 1),            new Short((short) 1));
        assertEquals(configurations.getFloat  ("value.missing", 456.123F),             new Float(456.123F));
        assertEquals(configurations.getDouble ("value.missing", 456.123D),             new Double(456.123D));
        assertEquals(configurations.getURI    ("value.missing", new URI("foo:wrong")), new URI("foo:wrong"));
        assertEquals(configurations.getURL    ("value.missing", new URL("http://g")),  new URL("http://g"));
        assertEquals(configurations.getFile   ("value.missing", new File("/bin")),     new File("/bin"));

        assertEquals(configurations.getString ("value.missing"), null);
        assertEquals(configurations.getBoolean("value.missing"), null);
        assertEquals(configurations.getInteger("value.missing"), null);
        assertEquals(configurations.getByte   ("value.missing"), null);
        assertEquals(configurations.getLong   ("value.missing"), null);
        assertEquals(configurations.getShort  ("value.missing"), null);
        assertEquals(configurations.getFloat  ("value.missing"), null);
        assertEquals(configurations.getDouble ("value.missing"), null);
        assertEquals(configurations.getURI    ("value.missing"), null);
        assertEquals(configurations.getURL    ("value.missing"), null);
        assertEquals(configurations.getFile   ("value.missing"), null);

    }

    @Test
    public void testMerge()
    throws Exception {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        assertEquals(configurations.get("value.string", "wrong string"), "a string");
        assertNull(configurations.get("value.merge"));

        final Configurations merge = new ConfigurationsBuilder().put("value.string", "new value")
                                                                .put("value.merge", "this is good")
                                                                .build();

        final Configurations merged = configurations.merge(merge);

        assertEquals(merged.get("value.string", "wrong string"), "a string");
        assertEquals(merged.get("value.merge",  "wrong string"), "this is good");
    }

    @Test
    public void testOverride()
    throws Exception {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        assertEquals(configurations.get("value.string", "wrong string"), "a string");
        assertNull(configurations.get("value.merge"));

        final Configurations override = new ConfigurationsBuilder().put("value.string", "new value")
                                                                   .put("value.merge", "this is good")
                                                                   .build();

        final Configurations overridden = configurations.override(override);

        assertEquals(overridden.get("value.string", "wrong string"), "new value");
        assertEquals(overridden.get("value.merge",  "wrong string"), "this is good");
    }

    @Test
    public void testPrefix()
    throws Exception {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        assertEquals(configurations.get("value.string", "wrong string"), "a string");
        assertNull(configurations.get("prefix.value.merge"));

        final Configurations prefixed = configurations.prefix("prefix");

        assertEquals(prefixed.get("prefix.value.string", "wrong string"), "a string");
        assertNull(prefixed.get("value.merge"));
    }

    @Test
    public void testStrip()
    throws Exception {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        assertEquals(configurations.get("value.string", "wrong string"), "a string");
        assertEquals(configurations.get("message",      "wrong string"), "hello");

        final Configurations stripped = configurations.strip("value");

        assertEquals(stripped.get("string", "wrong string"), "a string");
        assertNull(stripped.get("value.string"));
        assertNull(stripped.get("message"));
    }

    @Test
    public void testExtract()
    throws Exception {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        assertNotNull(configurations.get("value.string"));
        assertNotNull(configurations.get("message"));

        final Configurations extracted = configurations.extract("value");

        assertEquals(extracted.get("value.string",  "wrong string"),       "a string");
        assertEquals(extracted.get("value.boolean", false),                true);
        assertEquals(extracted.get("value.integer", 456),                  123);
        assertEquals(extracted.get("value.real",    456.123),              123.456);
        assertEquals(extracted.get("value.uri",     new URI("foo:wrong")), new URI("opaque:uri"));
        assertEquals(extracted.get("value.url",     new URL("http://g")),  new URL("http://www.google.com/"));
        assertEquals(extracted.get("value.file",    new File("/bin")),     new File("/etc"));
        assertNull(extracted.get("message"));
    }

    @Test
    public void testGroup() {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        final Map<String, Configurations> explicit = configurations.group("group", false);

        assertEquals(explicit.size(), 6);
        assertNotNull(explicit.get("foo"));
        assertNotNull(explicit.get("bar"));
        assertNotNull(explicit.get("baz"));
        assertNotNull(explicit.get("foo-x"));
        assertNotNull(explicit.get("bar-y"));
        assertNotNull(explicit.get("baz-z"));

        final Configurations foo = explicit.get("foo");
        final Configurations bar = explicit.get("bar");
        final Configurations baz = explicit.get("baz");
        final Configurations fooX = explicit.get("foo-x");
        final Configurations barY = explicit.get("bar-y");
        final Configurations bazZ = explicit.get("baz-z");

        assertEquals(foo.size(), 3);
        assertEquals(foo.get(null), "foo");
        assertEquals(foo.get("a"),  "foo.a");
        assertEquals(foo.get("b"),  "foo.b");

        assertEquals(bar.size(), 3);
        assertEquals(bar.get(null), "bar");
        assertEquals(bar.get("a"),  "bar.a");
        assertEquals(bar.get("b"),  "bar.b");

        assertEquals(baz.size(), 3);
        assertEquals(baz.get(null), "baz");
        assertEquals(baz.get("a"),  "baz.a");
        assertEquals(baz.get("b"),  "baz.b");

        assertEquals(fooX.size(), 1);
        assertEquals(fooX.get(null), "foo-x");

        assertEquals(barY.size(), 1);
        assertEquals(barY.get(null), "bar-y");

        assertEquals(bazZ.size(), 1);
        assertEquals(bazZ.get(null), "baz-z");
    }

    @Test
    public void testExplicitGroup() {
        final Configurations configurations = new ResourceConfigurations("test.properties");

        final Map<String, Configurations> explicit = configurations.group("explicit", true);

        assertEquals(explicit.size(), 2);
        assertNotNull(explicit.get("foo"));
        assertNotNull(explicit.get("bar"));
        assertNull   (explicit.get("baz"));

        final Configurations foo = explicit.get("foo");
        final Configurations bar = explicit.get("bar");

        assertEquals(foo.size(), 3);
        assertEquals(foo.get(null), "foo");
        assertEquals(foo.get("a"),  "foo.a");
        assertEquals(foo.get("b"),  "foo.b");

        assertEquals(bar.size(), 3);
        assertEquals(bar.get(null), "bar");
        assertEquals(bar.get("a"),  "bar.a");
        assertEquals(bar.get("b"),  "bar.b");

    }

    @Test
    public void testSaveLoad()
    throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Configurations original = new ResourceConfigurations("test.properties").list(output);

        final ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        final Configurations reloaded = new PropertiesConfigurations(input);

        assertNotSame(reloaded, original);

        assertEquals(reloaded, original);

        assertTrue(reloaded.equals(original));
        assertTrue(original.equals(reloaded));

        assertEquals(reloaded.hashCode(), original.hashCode());
        assertEquals(original.hashCode(), reloaded.hashCode());
    }

    @Test
    public void testClone()
    throws Exception {
        final Configurations original = new ResourceConfigurations("test.properties");
        final Configurations cloned = original.clone();

        assertNotSame(cloned, original);

        assertEquals(cloned, original);

        assertTrue(cloned.equals(original));
        assertTrue(original.equals(cloned));

        assertEquals(cloned.hashCode(), original.hashCode());
        assertEquals(original.hashCode(), cloned.hashCode());
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "^Invalid key name \\\"a~wrong~key\\\".*")
    public void testWrongKey()
    throws Exception {
        try {
            new ResourceConfigurations("wrongkey.properties");
            fail("Exception not thrown");
        } catch (IllegalArgumentException exception) {
            final ConfigurationsException wrapper = (ConfigurationsException) exception.getCause();
            assertEquals(exception.getMessage(), wrapper.getMessage());
            assertEquals(wrapper.getLocation(), this.getClass().getResource("wrongkey.properties").toString());
            assertEquals(wrapper.getLine(), -1);
            assertEquals(wrapper.getColumn(), -1);
            throw exception;
        }
    }
}
