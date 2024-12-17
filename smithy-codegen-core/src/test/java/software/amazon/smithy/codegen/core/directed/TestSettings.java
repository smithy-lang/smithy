/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

public final class TestSettings {
    private String foo;

    public void foo(String foo) {
        this.foo = foo;
    }

    public String foo() {
        return foo;
    }
}
