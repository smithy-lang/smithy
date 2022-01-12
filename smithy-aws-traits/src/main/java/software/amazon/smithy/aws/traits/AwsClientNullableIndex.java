/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.traits;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.shapes.MemberShape;

/**
 * Checks if a member is nullable by taking into account the
 * {@link ClientOptionalTrait} and base logic of NullableIndex.
 *
 * <p>This index extends from {@link NullableIndex} so that code generators
 * that need to conditionally honor the {@link ClientOptionalTrait} can
 * choose which specific NullableIndex they use during codegen.
 */
public final class AwsClientNullableIndex extends NullableIndex {

    public AwsClientNullableIndex(Model model) {
        super(model);
    }

    public static AwsClientNullableIndex of(Model model) {
        return model.getKnowledge(AwsClientNullableIndex.class, AwsClientNullableIndex::new);
    }

    /**
     * Checks if the given member should be generated as optional by an AWS client
     * that respects the {@link ClientOptionalTrait} and the base rules of Smithy's
     * {@link NullableIndex}.
     *
     * @param member Member to check.
     * @return Returns true if the shape should be optional in generated clients.
     */
    public boolean isMemberOptional(MemberShape member) {
        return member.hasTrait(ClientOptionalTrait.class) || super.isMemberOptional(member);
    }
}
