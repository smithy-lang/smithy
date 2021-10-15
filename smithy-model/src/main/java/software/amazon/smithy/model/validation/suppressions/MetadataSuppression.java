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

package software.amazon.smithy.model.validation.suppressions;

import java.util.Collection;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * A suppression created from metadata.
 *
 * <p>"*" is used as a wildcard to suppress events in any namespace.
 */
final class MetadataSuppression implements Suppression {

    private static final String ID = "id";
    private static final String NAMESPACE = "namespace";
    private static final String REASON = "reason";
    private static final Collection<String> SUPPRESSION_KEYS = ListUtils.of(ID, NAMESPACE, REASON);

    private final String id;
    private final String namespace;
    private final String reason;

    MetadataSuppression(String id, String namespace, String reason) {
        this.id = id;
        this.namespace = namespace;
        this.reason = reason;
    }

    static MetadataSuppression fromNode(Node node) {
        ObjectNode rule = node.expectObjectNode();
        rule.warnIfAdditionalProperties(SUPPRESSION_KEYS);
        String id = rule.expectStringMember(ID).getValue();
        String namespace = rule.expectStringMember(NAMESPACE).getValue();
        String reason = rule.getStringMemberOrDefault(REASON, null);
        return new MetadataSuppression(id, namespace, reason);
    }

    @Override
    public boolean test(ValidationEvent event) {
        return event.getId().equals(id) && matchesNamespace(event);
    }

    @Override
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    private boolean matchesNamespace(ValidationEvent event) {
        return namespace.equals("*")
               || event.getShapeId().filter(id -> id.getNamespace().equals(namespace)).isPresent();
    }
}
