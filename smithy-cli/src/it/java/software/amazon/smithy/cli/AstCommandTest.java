/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class AstCommandTest {
    @Test
    public void validatesModelSuccess() {
        IntegUtils.run("model-with-warning", ListUtils.of("ast"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), containsString("\"smithy\""));
            assertThat(result.getOutput(), not(containsString("WARNING")));
        });
    }

    @Test
    public void showsErrorsForInvalidModels() {
        IntegUtils.run("model-with-syntax-error", ListUtils.of("ast"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("ERROR"));
            assertThat(result.getOutput(), containsString("bar // <- invalid syntax"));
        });
    }

    @Test
    public void doesNotFlattenModelsWithoutFlattenOption() {
        IntegUtils.run("model-with-mixins", ListUtils.of("ast"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), containsString("\"smithy.api#mixin\": {}"));
        });
    }

    @Test
    public void flattensModelsWithFlattenOption() {
        IntegUtils.run("model-with-mixins", ListUtils.of("ast", "--flatten"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), not(containsString("\"smithy.api#mixin\": {}")));
        });
    }
}
