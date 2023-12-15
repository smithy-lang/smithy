/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedPaginatedTraitTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(ChangedPaginatedTraitTest.class.getResource("paginated-trait-tests.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void ignoresUnchangedTraits() {
        List<ValidationEvent> events = ModelDiff.compare(model, model);

        assertThat(TestHelper.findEvents(events, "ChangedPaginatedTrait"), empty());
    }

    @Test
    public void detectsRemovalOfTrait() {
        // Note that this is detected using diff tags. This is more of a sanity check.
        Model newModel = ModelTransformer.create()
                .removeTraitsIf(model, (shape, trait) -> trait instanceof PaginatedTrait);

        List<ValidationEvent> events = ModelDiff.compare(model, newModel);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
    }

    @Test
    public void detectsRemovalOfItems() {
        // Remove the items member from the new model.
        Model newModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().items(null).build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(model, newModel);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
        assertThat(changedTraitEvents.get(0).getMessage(),
                   containsString("Removed trait contents from `smithy.api#paginated` at path `/items`. Removed value: `things`"));
    }

    @Test
    public void detectsAdditionOfItems() {
        // Remove the items member from the old model.
        Model oldModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().items(null).build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(oldModel, model);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
        assertThat(changedTraitEvents.get(0).getMessage(),
                   containsString("Added trait contents to `smithy.api#paginated` at path `/items` with value `things`"));
    }

    @Test
    public void detectsChangeToItems() {
        // Change the items value in the new model.
        Model newModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().items("otherThings").build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(model, newModel);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
        assertThat(changedTraitEvents.get(0).getMessage(),
                   containsString("Changed trait contents of `smithy.api#paginated` at path `/items` from `things` to `otherThings`"));
    }

    @Test
    public void detectsRemovalOfPageSize() {
        // Remove the pageSize from the new model.
        Model newModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().pageSize(null).build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(model, newModel);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
        assertThat(changedTraitEvents.get(0).getMessage(),
                   containsString("Removed trait contents from `smithy.api#paginated` at path `/pageSize`. Removed value: `maxResults`"));
    }

    @Test
    public void ignoresAdditionOfPageSize() {
        // Remove the pageSize from the old model, making it detect the addition of a pageSize.
        Model oldModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().pageSize(null).build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(oldModel, model);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents, empty());
    }

    @Test
    public void detectsChangeToPageSize() {
        // Change the pageSize value in the new model.
        Model newModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().pageSize("otherMaxResults").build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(model, newModel);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
        assertThat(changedTraitEvents.get(0).getMessage(),
                   containsString("Changed trait contents of `smithy.api#paginated` at path `/pageSize` from `maxResults` to `otherMaxResults`"));
    }

    @Test
    public void detectsAnyChangeToInputToken() {
        Model newModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().inputToken("otherToken").build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(model, newModel);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
        assertThat(changedTraitEvents.get(0).getMessage(),
                   containsString("Changed trait contents of `smithy.api#paginated` at path `/inputToken` from `token` to `otherToken`"));
    }

    @Test
    public void detectsAnyChangeToOutputToken() {
        Model newModel = ModelTransformer.create().mapTraits(model, (shape, trait) -> {
            return trait instanceof PaginatedTrait
                   ? ((PaginatedTrait) trait).toBuilder().outputToken("otherToken").build()
                   : trait;
        });

        List<ValidationEvent> events = ModelDiff.compare(model, newModel);
        List<ValidationEvent> changedTraitEvents = TestHelper.findEvents(events, "TraitBreakingChange");

        assertThat(changedTraitEvents.size(), equalTo(1));
        assertThat(changedTraitEvents.get(0).getMessage(),
                   containsString("Changed trait contents of `smithy.api#paginated` at path `/outputToken` from `token` to `otherToken`"));
    }
}
