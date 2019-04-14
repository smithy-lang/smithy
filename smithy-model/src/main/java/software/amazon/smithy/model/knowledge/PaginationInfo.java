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

import java.util.Optional;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PaginatedTrait;

/**
 * Resolved and valid pagination information about an operation.
 */
public final class PaginationInfo {

    private final OperationShape operation;
    private final StructureShape input;
    private final StructureShape output;
    private final PaginatedTrait paginatedTrait;
    private final MemberShape inputToken;
    private final MemberShape outputToken;

    PaginationInfo(
            OperationShape operation,
            StructureShape input,
            StructureShape output,
            PaginatedTrait paginatedTrait,
            MemberShape inputToken,
            MemberShape outputToken
    ) {
        this.operation = operation;
        this.input = input;
        this.output = output;
        this.paginatedTrait = paginatedTrait;
        this.inputToken = inputToken;
        this.outputToken = outputToken;
    }

    public OperationShape getOperation() {
        return operation;
    }

    public StructureShape getInput() {
        return input;
    }

    public StructureShape getOutput() {
        return output;
    }

    public PaginatedTrait getPaginatedTrait() {
        return paginatedTrait;
    }

    public MemberShape getInputTokenMember() {
        return inputToken;
    }

    public MemberShape getOutputTokenMember() {
        return outputToken;
    }

    public Optional<MemberShape> getItemsMember() {
        return paginatedTrait.getItems().flatMap(output::getMember);
    }

    public Optional<MemberShape> getPageSizeMember() {
        return paginatedTrait.getPageSize().flatMap(input::getMember);
    }
}
