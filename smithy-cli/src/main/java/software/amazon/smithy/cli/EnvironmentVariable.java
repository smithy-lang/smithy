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

import java.util.logging.Logger;

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
     *
     * <p>If the CLI detects that Gradle is calling the CLI, dependency resolution is disabled and defaults to
     * {@code ignore}. You can explicitly enable dependency resolution by setting {@code SMITHY_DEPENDENCY_MODE} to
     * {@code standard}.
     */
    SMITHY_DEPENDENCY_MODE {
        @Override
        public String get() {
            String result = super.get();
            if (result != null) {
                return result;
            } else if (System.getProperty("org.gradle.appname") != null) {
                // Maintainer's note: Ideally this would just be in the Gradle plugin itself; however, previous
                // versions of the Gradle plugin can use newer versions of Smithy. This means that even if Gradle is
                // updated to disable dependency resolution, previous versions could inadvertently use it.
                LOGGER.info("Detected that the Smithy CLI is running in Gradle, so dependency resolution is disabled. "
                            + "This can be overridden by setting SMITHY_DEPENDENCY_MODE environment variable to "
                            + "'standard'.");
                return "ignore";
            } else {
                return "standard";
            }
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
    TERM,

    /**
     * Used to determine a proxy host to use for Maven dependency resolution.
     *
     * <p>Set the host, protocol, and port information with a valid url.
     * <pre>
     * {@code export SMITHY_PROXY_HOST=protocol://host:port}
     * </pre>
     * <strong>NOTE:</strong> this setting will be used for all repositories
     * defined in the smithy-build maven configuration unless a repo-specific
     * configuration is provided.
     *
     */
    SMITHY_PROXY_HOST,

    /**
     * Used to determine a proxy credentials to use for Maven dependency resolution through
     * a proxy.
     *
     * <p>Use this setting in conjunction with the {@link #SMITHY_PROXY_HOST} variable to configure
     * proxy settings for dependency resolution.
     *
     * <p>Set both the username and password information as a ":" separated string.
     * <pre>
     * {@code export SMITHY_PROXY_CREDENTIALS=user:pass}
     * </pre>
     * <strong>NOTE:</strong> this setting will be used for all repositories
     * defined in the smithy-build maven configuration unless a repo-specific
     * configuration is provided.
     */
    SMITHY_PROXY_CREDENTIALS;

    private static final Logger LOGGER = Logger.getLogger(EnvironmentVariable.class.getName());

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
