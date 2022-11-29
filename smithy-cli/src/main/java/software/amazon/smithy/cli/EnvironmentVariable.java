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
 * Publicly available Smithy environment variables / system properties.
 */
public enum EnvironmentVariable {
    /** A custom location for the Maven local repository cache. */
    SMITHY_MAVEN_CACHE,

    /** A pipe-delimited list of Maven repositories to use. */
    SMITHY_MAVEN_REPOS,

    /** The current version of the CLI. This is set automatically by the CLI. */
    SMITHY_VERSION;

    /**
     * Gets the system property or the environment variable for the property, in that order.
     *
     * @return Returns the found system property or environment variable or null.
     */
    public String getValue() {
        String name = toString();
        String value = System.getProperty(name);
        if (value == null) {
            value = System.getenv(name);
        }
        return value;
    }

    /**
     * Sets a system property for the environment variable.
     *
     * @param value Value to set.
     */
    public void setValue(String value) {
        System.setProperty(toString(), value);
    }
}
