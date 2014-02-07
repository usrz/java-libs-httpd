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

/**
 * An exception indicating that something went wrong parsing or creating a
 * a {@link Configurations} instance.
 *
 * <p>This exception is only used <em>internally</em> by the configurations
 * API, while normally it's {@linkplain #unchecked() unchecked} counter-part
 * is thrown back in normal methods.</p>
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class ConfigurationsException extends Exception {

    private String location = null;
    private int line = -1;
    private int column = -1;

    /**
     * Create a new {@link ConfigurationsException} for the specified key.
     */
    protected ConfigurationsException(String message, boolean x) {
        super(message);
    }

    /**
     * Initialize the location of this {@link ConfigurationsException}.
     */
    protected ConfigurationsException initLocation(String location) {
        return this.initLocation(location, -1, -1);
    }

    /**
     * Initialize the location of this {@link ConfigurationsException}.
     */
    protected ConfigurationsException initLocation(int line, int column) {
        return this.initLocation(null, line, column);
    }

    /**
     * Initialize the location of this {@link ConfigurationsException}.
     */
    protected ConfigurationsException initLocation(String location, int line, int column) {
        if (location != null) this.location = location;
        if (line > 0) this.line = line;
        if (column > 0) this.column = column;
        return this;
    }

    /**
     * Initialize the cause {@link Throwable} of this exception.
     */
    @Override
    public ConfigurationsException initCause(Throwable cause) {
        super.initCause(cause);
        return this;
    }

    /**
     * Return the location (a <em>file name</em>, <em>URL</em>, ... in
     * {@link String} format) associated with this
     * {@link ConfigurationsException} or <b>null</b>.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Return the line number in the {@link #getLocation() location} associated
     * with this {@link ConfigurationsException} or <b>-1</b>.
     */
    public int getLine() {
        return line;
    }

    /**
     * Return the column number in the {@link #getLocation() location}
     * associated with this {@link ConfigurationsException} or <b>-1</b>.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Return the message associated with this {@link ConfigurationsException}
     * possibly including location information.
     */
    @Override
    public String getMessage() {
        /* Original message */
        final StringBuilder builder = new StringBuilder(super.getMessage());

        /* Append location if needed */
        if ((location != null) || (line > 0) || (column > 0)) {
            String separator = " (";
            if (location != null) {
                builder.append(separator).append(location);
                separator = ", ";
            }
            if (line > 0) {
                builder.append(separator).append("line ").append(line);
                separator = ", ";
            }
            if (column > 0) {
                builder.append(separator).append("column ").append(column);
            }
            builder.append(")");
        }

        /* Return message */
        return builder.toString();
    }

    /**
     * Return an <em>unchecked</code> version of this exception.
     */
    protected IllegalArgumentException unchecked() {
        return new IllegalArgumentException(getMessage(), this);
    }
}
