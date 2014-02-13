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
package org.usrz.libs.httpd.statistics;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.testng.annotations.Test;
import org.usrz.libs.httpd.statistics.RoundRobin.Type;
import org.usrz.libs.testing.AbstractTest;

public class RoundRobinTest extends AbstractTest {

    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
    public String f(long millis) { return sdf.format(new Date(millis)); }

    private final void holdOnASec() {
        long now = System.currentTimeMillis();
        long next = 1000 - (now % 1000);
        if (next < 900) try {
            System.err.println("Waiting " + next + "ms at " + now);
            Thread.sleep(next + 1);
        } catch (InterruptedException exception) {
            fail("Interrupted", exception);
        }
    }

    private final void assertRR(RoundRobin.Entry[] entries, Integer... expected) {
        final Long[] converted = new Long[expected.length];
        for (int x = 0; x < expected.length; x ++)
            converted[x] = expected[x] == null ? null : (long) expected[x];
        assertRR(entries, converted);
    }

    private final void assertRR(RoundRobin.Entry[] entries, Long... expected) {
        if (entries == null) throw new AssertionError("Expcected non-null entries");
        if (entries.length != expected.length) throw new AssertionError("Expected " + expected.length + " entries, but got " + entries);

        boolean faulty = false;
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x < expected.length; x ++) {
            final RoundRobin.Entry entry = entries[x];
            final Long value = entry.getValue();
            builder.append("\n  ")
                   .append(f(entry.getTimestamp()))
                   .append(" = ")
                   .append(value);
            if (expected[x] == null) {
                if (value == null) {
                    builder.append(" OK");
                } else {
                    builder.append(" != " + expected[x] + " : FAIL");
                    faulty = true;
                }
            } else {
                if (value == null) {
                    builder.append(" != " + expected[x] + " : FAIL");
                    faulty = true;
                } else {
                    if (expected[x].doubleValue() == value.doubleValue()) {
                        builder.append(" OK");
                    } else {
                        builder.append(" != " + expected[x] + " : FAIL");
                        faulty = true;
                    }
                }
            }
        }

        if (faulty) {
            builder.setCharAt(1, '[');
            builder.append(" ]");
            throw new AssertionError("Values mismatch:" + builder.toString());
        }

    }

    /* ====================================================================== */

    @Test
    public void testExtraData() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.LAST, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 20; x ++) {
            final long timestamp = now + (x * 1000) - 14000;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
    }

    /* ====================================================================== */

    @Test
    public void testLast1() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.LAST, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 20; x ++) {
            final long timestamp = now + (x * 500) - 9000;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 1, 3, 5, 7, 9, 11, 13, 15, 17, 19);
    }

    @Test
    public void testLast2() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.LAST, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8500;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8750;
            rr.record(timestamp, x + 10);
        }

        assertRR(rr.entries(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8250;
            rr.record(timestamp, x + 90);
        }

        assertRR(rr.entries(), 90, 91, 92, 93, 94, 95, 96, 97, 98, 99);
    }

    /* ====================================================================== */

    @Test
    public void testMin1() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.MIN, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 20; x ++) {
            final long timestamp = now + (x * 500) - 9000;
            rr.record(timestamp, x);
        }
        assertRR(rr.entries(), 0, 2, 4, 6, 8, 10, 12, 14, 16, 18);

    }

    @Test
    public void testMin2() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.MIN, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8500;
            rr.record(timestamp, x + 10);
        }

        assertRR(rr.entries(), 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8750;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8250;
            rr.record(timestamp, x + 5);
        }

        assertRR(rr.entries(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    /* ====================================================================== */

    @Test
    public void testMax1() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.MAX, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 20; x ++) {
            final long timestamp = now + (x * 500) - 9000;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 1, 3, 5, 7, 9, 11, 13, 15, 17, 19);
    }

    @Test
    public void testMax2() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.MAX, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8500;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8750;
            rr.record(timestamp, x + 10);
        }

        assertRR(rr.entries(), 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8250;
            rr.record(timestamp, x + 5);
        }

        assertRR(rr.entries(), 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
    }

    /* ====================================================================== */

    @Test
    public void testSum1() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.SUM, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 20; x ++) {
            final long timestamp = now + (x * 500) - 9000;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 0+1, 2+3, 4+5, 6+7, 8+9, 10+11, 12+13, 14+15, 16+17, 18+19);
    }

    @Test
    public void testSum2() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.SUM, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8500;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8750;
            rr.record(timestamp, x + 10);
        }

        assertRR(rr.entries(), 10+0, 11+1, 12+2, 13+3, 14+4, 15+5, 16+6, 17+7, 18+8, 19+9);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8250;
            rr.record(timestamp, x + 5);
        }

        assertRR(rr.entries(), 5+10+0, 6+11+1, 7+12+2, 8+13+3, 9+14+4, 10+15+5, 11+16+6, 12+17+7, 13+18+8, 14+19+9);
    }

    /* ====================================================================== */

    @Test
    public void testAverage1() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.AVERAGE, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 20; x ++) {
            final long timestamp = now + (x * 500) - 9000;
            rr.record(timestamp, x * 2);
        }

        assertRR(rr.entries(), 1, 5, 9, 13, 17, 21, 25, 29, 33, 37);
    }

    @Test
    public void testAverage2() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.AVERAGE, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8500;
            rr.record(timestamp, x * 2);
        }

        assertRR(rr.entries(), 0, 2, 4, 6, 8, 10, 12, 14, 16, 18);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8750;
            rr.record(timestamp, x * 4);
        }

        assertRR(rr.entries(), 0, 3, 6, 9, 12, 15, 18, 21, 24, 27);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 8250;
            rr.record(timestamp, 0);
        }

        assertRR(rr.entries(), 0, 2, 4, 6, 8, 10, 12, 14, 16, 18);
    }

    /* ====================================================================== */

    @Test
    public void testCounter1() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.COUNTER, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 9000;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 1, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    public void testCounter2() throws Throwable {
        holdOnASec();

        final RoundRobin rr = new RoundRobin(Type.COUNTER, SECONDS, 10, SECONDS, 1);
        long now = System.currentTimeMillis();

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 9000;
            rr.record(timestamp, x);
        }

        assertRR(rr.entries(), 1, 1, 1, 1, 1, 1, 1, 1, 1);

        for (int x = 0; x < 10; x ++) {
            final long timestamp = now + (x * 1000) - 9000;
            rr.record(timestamp, 2 * x);
        }

        assertRR(rr.entries(), 2, 2, 2, 2, 2, 2, 2, 2, 2);
    }

    /* ====================================================================== */

    @Test
    public void testCounterOverflows() throws Throwable {

        final long[] overflows = { 0x07FFFFFFFL, 0x07FFFFFFFFFFFFFFFL };
        for (long overflow: overflows) {
            for (int m = 0; m < 10; m ++) try {
                holdOnASec();

                final RoundRobin rr = new RoundRobin(Type.COUNTER, SECONDS, 10, SECONDS, 1);
                long now = System.currentTimeMillis();

                for (int x = 0; x < 10; x ++) {
                    final long timestamp = now + (x * 1000) - 9000;

                    long value = (((m * x) - (m * 4)) + overflow);
                    if (value > overflow) value = value | (overflow ^ -1L);

                    rr.record(timestamp, value);
                }

                assertRR(rr.entries(), m, m, m, m, m, m, m, m, m);

            } catch (AssertionError error) {
                fail("Error testing overflow " + Long.toHexString(overflow) + " with m=" + m, error);
            }
        }
    }

    /* ====================================================================== */

    @Test
    public void testCounterResets() throws Throwable {

        final long[] overflows = { 0x07FFFFFFFL, 0x0FFFFFFFFL, 0x07FFFFFFFFFFFFFFFL };
        for (long overflow: overflows) {
            for (int m = 0; m < 10; m ++) try {
                holdOnASec();

                final RoundRobin rr = new RoundRobin(Type.COUNTER, SECONDS, 10, SECONDS, 1);
                long now = System.currentTimeMillis();

                for (int x = 0; x < 10; x ++) {
                    final long timestamp = now + (x * 1000) - 9000;

                    long value = (((m * x) - (m * 4)) + overflow);
                    if (value > overflow) value = value & overflow;

                    rr.record(timestamp, value);
                }

                assertRR(rr.entries(), m, m, m, m, m, m, m, m, m);

            } catch (AssertionError error) {
                fail("Error testing reset " + Long.toHexString(overflow) + " with m=" + m, error);
            }
        }
    }

}