/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

interface NamedMembers {

    /**
     * Gets the members of the shape, including mixin members.
     *
     * @return Returns the immutable member map.
     */
    Map<String, MemberShape> getAllMembers();

    /**
     * Returns an ordered list of member names based on the order they are
     * defined in the model, including mixin members.
     *
     * @return Returns an immutable list of member names.
     */
    List<String> getMemberNames();

    /**
     * Get a specific member by name.
     *
     * @param name Name of the member to retrieve.
     * @return Returns the optional member.
     */
    default Optional<MemberShape> getMember(String name) {
        return Optional.ofNullable(getAllMembers().get(name));
    }
}
