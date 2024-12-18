/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class CaseUtilsTest {
    @Test
    public void convertsSnakeToLowerCamelCase() {
        assertThat(CaseUtils.snakeToCamelCase("foo_bar"), equalTo("fooBar"));
        assertThat(CaseUtils.snakeToCamelCase("Foo_bar"), equalTo("fooBar"));
        assertThat(CaseUtils.snakeToCamelCase("__foo_bar"), equalTo("fooBar"));
    }

    @Test
    public void convertsSnakeToPascalCase() {
        assertThat(CaseUtils.snakeToPascalCase("foo_bar"), equalTo("FooBar"));
        assertThat(CaseUtils.snakeToPascalCase("Foo_bar"), equalTo("FooBar"));
        assertThat(CaseUtils.snakeToPascalCase("__foo_bar"), equalTo("FooBar"));
    }

    @Test
    public void convertsToSnakeCase() {
        assertThat(CaseUtils.toSnakeCase("foo"), equalTo("foo"));
        assertThat(CaseUtils.toSnakeCase(" foo"), equalTo("_foo"));
        assertThat(CaseUtils.toSnakeCase(" fooBar"), equalTo("_foo_bar"));
        assertThat(CaseUtils.toSnakeCase("10-foo"), equalTo("10_foo"));
        assertThat(CaseUtils.toSnakeCase("_foo"), equalTo("_foo"));
        assertThat(CaseUtils.toSnakeCase("FooBar"), equalTo("foo_bar"));
        assertThat(CaseUtils.toSnakeCase("FooAPI"), equalTo("foo_api"));
        assertThat(CaseUtils.toSnakeCase("Ec2Foo"), equalTo("ec2_foo"));
        assertThat(CaseUtils.toSnakeCase("foo_bar"), equalTo("foo_bar"));
    }
}
