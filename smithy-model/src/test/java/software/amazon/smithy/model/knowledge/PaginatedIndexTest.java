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

package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.ValidatedResult;
import software.amazon.smithy.model.shapes.ShapeId;

public class PaginatedIndexTest {
    @Test
    public void findDirectChildren() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource(
                        "/software/amazon/smithy/model/errorfiles/validators/paginated-trait-test.json"))
                .assemble();
        Model model = result.getResult().get();
        PaginatedIndex index = model.getKnowledge(PaginatedIndex.class);

        assertThat(index.getValidationEvents(), not(empty()));
        assertThat(index.getPaginationInfo(ShapeId.from("ns.foo#Valid2")).isPresent(), is(true));
        PaginationInfo info = index.getPaginationInfo(ShapeId.from("ns.foo#Valid2")).get();
        assertThat(info.getOperation().getId(), is(ShapeId.from("ns.foo#Valid2")));
        assertThat(info.getInput().getId(), is(ShapeId.from("ns.foo#ValidInput")));
        assertThat(info.getOutput().getId(), is(ShapeId.from("ns.foo#ValidOutput")));
        assertThat(info.getInputTokenMember().getMemberName(), equalTo("nextToken"));
        assertThat(info.getOutputTokenMember().getMemberName(), equalTo("nextToken"));
        assertThat(info.getPageSizeMember().isPresent(), is(true));
        assertThat(info.getPageSizeMember().get().getMemberName(), equalTo("pageSize"));
        assertThat(info.getItemsMember().get().getMemberName(), equalTo("items"));
    }
}
