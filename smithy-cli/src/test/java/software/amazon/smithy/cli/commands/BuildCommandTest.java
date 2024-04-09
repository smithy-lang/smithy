/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliUtils;

public class BuildCommandTest {
    @Test
    public void hasLongHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("build", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Builds"));
    }

    @Test
    public void hasShortHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("build", "-h");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Builds"));
    }

    @Test
    public void dumpsOutValidationErrorsAndFails() throws Exception {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("build", model);

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("smithy.example#MyString"));
        assertThat(result.stderr(), containsString("ERROR: 1"));
        assertThat(result.stderr(), containsString("FAILURE"));
    }

    @Test
    public void allowsUnknownTraitWithFlag() throws Exception {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("build", "--allow-unknown-traits", model);

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("Completed projection source"));
        assertThat(result.stderr(), containsString("Smithy built "));
    }

    @Test
    public void printsSuccessfulProjections() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("build", model);

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("Completed projection source"));
        assertThat(result.stderr(), containsString("Smithy built "));
    }

    @Test
    public void validationFailuresCausedByProjectionsAreDetected() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        String config = Paths.get(getClass().getResource("projection-build-failure.json").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("build", "--debug", "--config", config, model);

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("ResourceLifecycle"));
        assertThat(result.stderr(),
                   containsString("The following 1 Smithy build projection(s) failed: [exampleProjection]"));
    }

    @Test
    public void projectionUnknownTraitsAreDisallowed() throws Exception {
        String config = Paths.get(getClass().getResource("projection-model-import.json").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("build", "--config", config);

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Unable to resolve trait `some.unknown#trait`"));
        assertThat(result.stderr(), containsString("Smithy build projection(s) failed: [exampleProjection]\n"));
    }

    @Test
    public void projectionUnknownTraitsAreAllowedWithFlag() throws Exception {
        String config = Paths.get(getClass().getResource("projection-model-import.json").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("build", "--allow-unknown-traits",  "--config", config);

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("Completed projection exampleProjection"));
        assertThat(result.stderr(), containsString("Smithy built "));
    }

    @Test
    public void projectionUnknownTraitsAreAllowedWithShortFlag() throws Exception {
        String config = Paths.get(getClass().getResource("projection-model-import.json").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("build", "--aut",  "--config", config);

        assertThat(result.code(), equalTo(0));
    }

    @Test
    public void exceptionsThrownByProjectionsAreDetected() {
        // TODO: need to make a plugin throw an exception
    }

    @Test
    public void canHideModelsPositional() {
        CliUtils.Result result = CliUtils.runSmithy("diff", "-h");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), not(containsString("[<MODELS>]")));
    }
}
