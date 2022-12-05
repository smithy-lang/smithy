/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.cli;

/**
 * Environment variables used by the Smithy CLI.
 */
public enum EnvironmentVariable {
    /**
     * A custom location for the Maven local repository cache.
     *
     * <p>Example: {@code ~/.m2/repository}
     */
    SMITHY_MAVEN_CACHE,

    /**
     * A pipe-delimited list of Maven repositories to use.
     *
     * <p>Example: {@code https://example.com/repo1|https://example.com/repo2}
     */
    SMITHY_MAVEN_REPOS,

    /**
     * Configures if and how the Smithy CLI handles dependencies declared in smithy-build.json files.
     *
     * <ul>
     *     <li>ignore: ignore dependencies and assume that they are provided by the caller of the CLI.</li>
     *     <li>forbid: forbids dependencies from being declared and will fail the CLI if dependencies are declared.</li>
     *     <li>standard: the assumed default, will automatically resolve dependencies using Apache Maven.</li>
     * </ul>
     */
    SMITHY_DEPENDENCY_MODE {
        @Override
        public String get() {
            String result = super.get();
            return result == null ? "standard" : result;
        }
    },

    /** The current version of the CLI. This is set automatically by the CLI. */
    SMITHY_VERSION,

    /**
     * If set to any value, disable ANSI colors in the output.
     */
    NO_COLOR,

    /**
     * If set to any value, force enables the support of ANSI colors in the output.
     */
    FORCE_COLOR,

    /**
     * Used to detect if ANSI colors are supported.
     *
     * <ul>
     *     <li>If set to "dumb" colors are disabled.</li>
     *     <li>If not set and the operating system is detected as Windows, colors are disabled.</li>
     * </ul>
     */
    TERM;

    /**
     * Gets a system property or environment variable by name, in that order.
     *
     * @param name Variable to get.
     * @return Returns the found system property or environment variable or null.
     */
    public static String getByName(String name) {
        String value = System.getProperty(name);
        if (value == null) {
            value = System.getenv(name);
        }
        return value;
    }

    /**
     * Returns true if the system property or environment variables is set.
     *
     * @return Returns true if set.
     */
    public boolean isSet() {
        return get() != null;
    }

    /**
     * Gets the system property or the environment variable for the property, in that order.
     *
     * @return Returns the found system property or environment variable or null.
     */
    public String get() {
        return getByName(toString());
    }

    /**
     * Sets a system property for the environment variable.
     *
     * @param value Value to set.
     */
    public void set(String value) {
        System.setProperty(toString(), value);
    }

    /**
     * Clears the system property for the variable.
     */
    public void clear() {
        System.clearProperty(toString());
    }
}
