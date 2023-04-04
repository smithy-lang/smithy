/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
}
