/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class EnvironmentVariableTest {
    @Test
    public void ignoreDependenciesWhenRunningInGradle() {
        Object originalValue = System.getProperty("org.gradle.appname");

        try {
            System.setProperty("org.gradle.appname", "foo");
            assertThat(EnvironmentVariable.SMITHY_DEPENDENCY_MODE.get(), equalTo("ignore"));
        } finally {
            if (originalValue == null) {
                System.clearProperty("org.gradle.appname");
            } else {
                System.setProperty("org.gradle.appname", originalValue.toString());
            }
        }
    }

    @Test
    public void useStandardDependencyModeWhenRunningInGradle() {
        Object originalValue = System.getProperty("org.gradle.appname");

        try {
            System.clearProperty("org.gradle.appname");
            assertThat(EnvironmentVariable.SMITHY_DEPENDENCY_MODE.get(), equalTo("standard"));
        } finally {
            if (originalValue != null) {
                System.setProperty("org.gradle.appname", originalValue.toString());
            }
        }
    }
}
