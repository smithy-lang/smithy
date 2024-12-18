/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedMetadataTest {
    @Test
    public void detectsChangedMetadata() {
        Model modelA = Model.assembler().putMetadata("foo", Node.from("baz")).assemble().unwrap();
        Model modelB = Model.assembler().putMetadata("foo", Node.from("bam")).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMetadata").size(), equalTo(1));
    }
}
