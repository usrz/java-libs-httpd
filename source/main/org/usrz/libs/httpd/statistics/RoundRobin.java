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

import static java.lang.System.currentTimeMillis;
import static org.usrz.libs.httpd.statistics.RoundRobin.Type.AVERAGE;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * A simple time-based circular buffer data structure capable of storing
 * time-based data, similar to <a href="http://oss.oetiker.ch/rrdtool/">RRD
 * Tool</a>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class RoundRobin {

    /**
     * The type of data stored in a {@link RoundRobin} data structure.
     *
     * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
     */
    public enum Type {
        /**
         * Only store the <em>minimum value</em> of all
         * {@linkplain #record(long, long) recordings} for each data point.
         */
        MIN,

        /**
         * Only store the <em>maximum value</em> of all
         * {@linkplain #record(long, long) recordings} for each data points.
         */
        MAX,

        /**
         * Only store the <em>last</em> (greatest time-stamp) of all
         * {@linkplain #record(long, long) recordings} for each data points.
         */
        LAST,

        /**
         * Aggregate and sum together the values of each individual
         * {@linkplain #record(long, long) recording} for each data point.
         *
         * <p>The values passed to the{@link #record(long, long)} for each
         * individual data points are summed together.</p>
         */
        SUM,

        /**
         * Aggregate and average the values of each individual
         * {@linkplain #record(long, long) recording} for each data point.
         *
         * <p>This is similar to {@link SUM} but the
         * {@link RoundRobin#entries()} method will divide the sum by the
         * number of recordings in order to return an average value for
         * each data point.</p>
         */
        AVERAGE,

        /**
         * Keep track of <em>always-increasing</em> counters (like the number
         * of bytes transmitted and received by a network interface).
         *
         * <p>When data is organized in this fashion, only the <em>maximum</em>
         * value of each individual {@linkplain #record(long, long) recordings}
         * will be kept, but the {@link RoundRobin#entries()} method will
         * calculate the delta between each data point and its previous one
         * and return this instead.</p>
         */
        COUNTER
    };

    /**
     * A simple holder class for time-based data.
     *
     * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
     */
    public static final class Entry implements Map.Entry<Date, Long> {
        private final long timestamp;
        private Long value;

        private Entry(long timestamp, Long value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        /**
         * Alias to {@link #getDate()}
         */
        @Override
        public Date getKey() {
            return getDate();
        }

        /**
         * Return a <b>non-null</b> {@link Date} associated with the current
         * {@linkplain #getValue() value}.
         */
        public Date getDate() {
            return new Date(timestamp);
        }

        /**
         * Return the <em>timestamp</em> (milliseconds from the Epoch)
         * associated with the current {@linkplain #getValue() value}.
         */
        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Return the <em>value</em> associated with the current
         * {@linkplain #getTimestamp() timestamp}
         */
        @Override
        public Long getValue() {
            return value;
        }

        /**
         * <em>Unsupported operation</em>
         *
         * @throws UnsupportedOperationException <b>Always</b>
         */
        @Override
        public Long setValue(Long value)
        throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Compare the specified object for equality.
         */
        @Override
        public boolean equals(Object object) {
            if (object == null) return false;
            if (object == this) return true;
            try {
                final Entry entry = (Entry) object;
                if (entry.timestamp != timestamp) return false;
                return entry.value == null ? value == null :
                           entry.value.equals(value);
            } catch (ClassCastException exception) {
                return false;
            }
        }

        /**
         * Return the {@link String} representation of this instance.
         */
        @Override
        public String toString() {
            return String.format("%s[%08x@%d]=%s", getClass().getName(),
                                                   hashCode(),
                                                   timestamp,
                                                   value == null ? "null" :
                                                       Double.toString(value));
        };
    }

    /* ====================================================================== */

    /* Overflows at 31, 32 and 63 bits */
    private static final long OVERFLOW_31 = 0x7FFFFFFFL;
    private static final long OVERFLOW_32 = 0xFFFFFFFFL;
    private static final long OVERFLOW_63 = 0x7FFFFFFFFFFFFFFFL;

    /* Lock reads and writes fairly */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final WriteLock writeLock = lock.writeLock();
    private final ReadLock readLock = lock.readLock();

    /* The type of data */
    private final Type type;
    /* The total duration of data to keep */
    private final long durationMs;
    /* The time of each individual datapoint */
    private final long aggregationMs;

    /* Our data points, where we keep stuff */
    private final Long[] datapoints;
    /* The timestamps associated with data */
    private final long[] timestamps;
    /* The number of recordings for each datapoint */
    private final int[] recordings;

    /**
     * Create a new {@link RoundRobin} instance.
     *
     * <p>For example, to keep <em>one hour</em> of data, organized in
     * 240 <em>fifteen seconds</em> data points:</p>
     *
     * <pre>new RoundRobin(<em>type</em>, TimeUnit.HOURS, 1 TimeUnit.SECONDS, 15);</pre>
     *
     * @param type The {@link Type} of the data managed by this instance.
     * @param durationUnit The {@link TimeUnit} for <code>duration</code>.
     * @param duration The actual value in <code>durationUnit</code> of data
     *                 to be kept by this instance.
     * @param aggregationUnit The {@link TimeUnit} for <code>aggregation</code>.
     * @param aggregation The actual value in <code>aggregationUnit</code> of
     *                    of how much time each data point represents.
     * @see #RoundRobin(Type, long, long)
     */
    public RoundRobin(Type type,
                      TimeUnit durationUnit, long duration,
                      TimeUnit aggregationUnit, long aggregation) {
        this(type, durationUnit.toMillis(duration), aggregationUnit.toMillis(aggregation));
    }

    /**
     * Create a new {@link RoundRobin} instance.
     *
     * <p>For example, to keep <em>ten minutes</em> of data, organized in
     * 600 <em>one second</em> data points:</p>
     *
     * <pre>new RoundRobin(<em>type</em>, 600000, 1000);</pre>
     *
     * @param type The {@link Type} of the data managed by this instance.
     * @param durationMs The actual value in <em>milliseconds</em> of data
     *                   to be kept by this instance.
     * @param aggregationMs The actual value in <em>milliseconds</em> of
     *                      of how much time each data point represents.
     */
    public RoundRobin(Type type, long durationMs, long aggregationMs) {
        if (type == null) throw new NullPointerException("Null type");
        if (durationMs < 1) throw new IllegalArgumentException("Negative or zero duration");
        if (aggregationMs < 1) throw new IllegalArgumentException("Negative or zero aggregation");

        this.durationMs = durationMs;
        this.aggregationMs = aggregationMs;
        this.type = type;

        final long moduloLong = durationMs / aggregationMs;
        if (moduloLong < 1L) throw new IllegalArgumentException("Duration/aggregation ratio too small");
        if (moduloLong > 65536L) throw new IllegalArgumentException("Too many datapoints");
        final int modulo = (int) moduloLong;

        datapoints = new Long[modulo];
        timestamps = new long[modulo];
        recordings = new int[modulo];

        Arrays.fill(timestamps, Long.MIN_VALUE);
    }

    /* ====================================================================== */

    /**
     * Record a value for the <em>current</em> data point.
     */
    public void record(long value) {
        record(System.currentTimeMillis(), value);
    }

    /**
     * Record a value for the data point identified by the given {@link Date}.
     *
     * <p>If the date is <b>null</b> it will be defaulted to <em>now</em>.</p>
     */
    public void record(Date date, long value) {
        record(date == null ? currentTimeMillis(): date.getTime(), value);
    }

    /**
     * Record a value for the data point identified by the given timestamp.
     */
    public void record(long timestamp, long value) {

        /* Wait here until we can write */
        writeLock.lock();
        try {

            /* Calculate our threshold *after* acquiring our object's monitor */
            final long threshold = threshold(System.currentTimeMillis());

            /* Attempted to record data way in the past or too in the future */
            if ((timestamp < threshold) || (timestamp >= threshold + durationMs)) return;

            /* Calculate the index in our arrays of where data must be stored */
            final int idx = (int) ((timestamp / aggregationMs) % datapoints.length);

            /* Store the data if this is the first entry */
            if ((datapoints[idx] == null) || (timestamps[idx] < threshold)) {
                timestamps[idx] = timestamp;
                recordings[idx] = 1;
                datapoints[idx] = value;
                return;
            }

            /* The data at "idx" is valid, we must record depending on type */
            switch (type) {
                case LAST: if (timestamp > timestamps[idx]) { timestamps[idx] = timestamp; datapoints[idx] = value; } break;
                case MIN:  if (value     < datapoints[idx]) { timestamps[idx] = timestamp; datapoints[idx] = value; } break;
                case COUNTER: // derivation rules applied in entries(), just keep the maximum
                case MAX:  if (value     > datapoints[idx]) { timestamps[idx] = timestamp; datapoints[idx] = value; } break;
                case AVERAGE: // average rules applied in entries(), just sum
                case SUM:  recordings[idx]++; timestamps[idx] = timestamp; datapoints[idx] += value; break;
            }

        } finally {
            /* Remember to let anyone else go free */
            writeLock.unlock();
        }
    }

    /* ====================================================================== */

    /**
     * Return the data currently held in this {@link RoundRobin} structure,
     * ordered by time (from the oldest to the most recent).
     */
    public Entry[] entries() {

        /* Where to put return values */
        final Entry[] entries = new Entry[datapoints.length];

        /* Wait here until we can read */
        readLock.lock();
        try {

            /* Calculate our timings *after* acquiring our object's monitor */
            final long now = System.currentTimeMillis();
            final long threshold = now - durationMs - (now % aggregationMs) + aggregationMs;
            final int offset = (int) ((now / aggregationMs) % datapoints.length);

            /* Calculate values and entries */
            long timestamp = threshold;
            for (int x = 0; x < entries.length; x ++, timestamp += aggregationMs) {
                final int idx = (x + offset + 1) % datapoints.length;
                final Long value = timestamps[idx] < threshold ? null :
                                   datapoints[idx] == null ? null :
                                   AVERAGE == type ?
                                       datapoints[idx] / recordings[idx] :
                                       datapoints[idx];
                entries[x] = new Entry(timestamp, value);
            }

            /* Return here if we we don't calculate deltas */
            if (type != Type.COUNTER) return entries;

        } finally {
            /* Mark as done, allow writes */
            readLock.unlock();
        }

        /* This is a counter: we need to figure out what's our overflow point */
        long overflow = 0L;
        for (Entry entry: entries) {
            if (entry.getValue() == null) continue;
            long value = entry.getValue();
            if (value < 0) value = ~value;
            if ((overflow <= OVERFLOW_31) && (value <= OVERFLOW_31)) { overflow = OVERFLOW_31; continue; }
            if ((overflow <= OVERFLOW_32) && (value <= OVERFLOW_32)) { overflow = OVERFLOW_32; continue; }
            overflow = OVERFLOW_63;
        }

        /* If "overflow" is still zero, all entry values are "null"s, bail out */
        if (overflow == 0) return entries;

        /* The first value is always null */
        Long previous = entries[0].value;
        entries[0].value = null;

        /* We need to iterate the array, */
        for (int x = 1; x < entries.length; x ++) {
            final Long current = entries[x].value;

            /* If we don't have a previous value, we hope for the next */
            if (previous == null) {
                entries[x].value = null;
                previous = current;
                continue;
            }

            /* If the current value is null or equal to current, easy */
            if (current == null) {
                entries[x].value = current;
                continue;
            }

            /*
             * If the current value is greater than (or equal to) previous,
             * simply compute the difference. This works both for normal (both
             * positive) and over-flown (both negative) values.
             */
            if (previous <= current) {
                entries[x].value = current - previous;
            }

            /*
             * Counters are ever-increasing, henceforth if currently the value
             * is less than zero, we must have over-flown at this point. The
             * previous value will definitely be greater than zero, as counters
             * are ever-increasing.
             */
            else if (current < 0) {
                entries[x].value = (previous - overflow) + (current - ~overflow) + 1;
            }

            /*
             * In this case, we have a simple counter reset at a bit boundary
             * (our overflow which we calculated before). We simply need to
             * keep that into account.
             */
            else {
                entries[x].value = (overflow - previous) + current + 1;
            }

            /* Keep the old value */
            previous = current;
        }

        /* Remove the always-null first entry and return */
        final Entry[] values = new Entry[entries.length - 1];
        System.arraycopy(entries, 1, values, 0, values.length);
        return values;
    }

    /* ====================================================================== */

    /* Calculate the threshold */
    private final long threshold(long now) {
        return now - durationMs - (now % aggregationMs) + aggregationMs;
    }

}
