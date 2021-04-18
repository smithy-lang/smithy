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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.utils.ListUtils;

public class PaginatedIndexTest {
    @Test
    public void findDirectChildren() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource(
                        "/software/amazon/smithy/model/errorfiles/validators/paginated/paginated-valid.json"))
                .assemble();
        Model model = result.getResult().get();
        PaginatedIndex index = PaginatedIndex.of(model);
        ShapeId service = ShapeId.from("ns.foo#Service");

        assertThat(index.getPaginationInfo(service, ShapeId.from("ns.foo#Valid2")).isPresent(), is(true));
        PaginationInfo info = index.getPaginationInfo(service, ShapeId.from("ns.foo#Valid2")).get();
        assertThat(info.getOperation().getId(), is(ShapeId.from("ns.foo#Valid2")));
        assertThat(info.getInput().getId(), is(ShapeId.from("ns.foo#ValidInput")));
        assertThat(info.getOutput().getId(), is(ShapeId.from("ns.foo#ValidOutput")));
        assertThat(info.getInputTokenMember().getMemberName(), equalTo("nextToken"));
        assertThat(info.getOutputTokenMember().getMemberName(), equalTo("nextToken"));
        assertThat(info.getOutputTokenMemberPath().isEmpty(), is(false));
        assertThat(info.getOutputTokenMemberPath().stream()
                .map(MemberShape::getMemberName)
                .collect(Collectors.toList()), equalTo(ListUtils.of("nextToken")));
        assertThat(info.getPageSizeMember().isPresent(), is(true));
        assertThat(info.getPageSizeMember().get().getMemberName(), equalTo("pageSize"));
        assertThat(info.getItemsMember().get().getMemberName(), equalTo("items"));
        assertThat(info.getItemsMemberPath().isEmpty(), is(false));
        assertThat(info.getItemsMemberPath().stream()
                .map(MemberShape::getMemberName)
                .collect(Collectors.toList()), equalTo(ListUtils.of("items")));
    }

    @Test
    public void findIndirectChildren() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource(
                        "/software/amazon/smithy/model/errorfiles/validators/paginated/paginated-valid.json"))
                .assemble();
        Model model = result.getResult().get();
        PaginatedIndex index = PaginatedIndex.of(model);

        ShapeId service = ShapeId.from("ns.foo#Service");
        ShapeId operation = ShapeId.from("ns.foo#ValidNestedOutputOperation");
        Optional<PaginationInfo> optionalInfo = index.getPaginationInfo(service, operation);
        assertThat(optionalInfo.isPresent(), is(true));

        PaginationInfo info = optionalInfo.get();
        assertThat(info.getOutputTokenMember().getMemberName(), equalTo("nextToken"));
        assertThat(info.getOutputTokenMemberPath().isEmpty(), is(false));
        assertThat(info.getOutputTokenMemberPath().stream()
                .map(MemberShape::getMemberName)
                .collect(Collectors.toList()), equalTo(ListUtils.of("result", "nextToken")));
        assertThat(info.getItemsMember().isPresent(), is(true));
        assertThat(info.getItemsMember().get().getMemberName(), equalTo("items"));
        assertThat(info.getItemsMemberPath().isEmpty(), is(false));
        assertThat(info.getItemsMemberPath().stream()
                .map(MemberShape::getMemberName)
                .collect(Collectors.toList()), equalTo(ListUtils.of("result", "items")));
    }
}
