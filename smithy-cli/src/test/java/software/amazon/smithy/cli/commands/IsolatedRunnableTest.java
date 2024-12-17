/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliError;

public class IsolatedRunnableTest {
    @Test
    public void runsInThread() {
        Runnable isolated = new IsolatedRunnable(Collections.emptyList(), getClass().getClassLoader(), cl -> {
            try {
                Class<?> c = cl.loadClass("software.amazon.smithy.cli.commands.IsolatedRunnableTest$TestClass");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        isolated.run();
    }

    @Test
    public void runsInThreadAndRethrows() {
        Runnable isolated = new IsolatedRunnable(Collections.emptyList(), getClass().getClassLoader(), cl -> {
            throw new RuntimeException("Hello from thread");
        });

        Assertions.assertThrows(CliError.class, isolated::run);
    }

    public static final class TestClass {}
}
