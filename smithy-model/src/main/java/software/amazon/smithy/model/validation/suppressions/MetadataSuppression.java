/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
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
    private static final Predicate<ValidationEvent> STAR_MATCHER = event -> true;

    private final String id;
    private final String namespace;
    private final String reason;
    private final Predicate<ValidationEvent> namespaceMatcher;

    MetadataSuppression(String id, String namespace, String reason) {
        this.id = id;
        this.namespace = namespace;
        this.reason = reason;
        this.namespaceMatcher = namespace.equals("*") ? STAR_MATCHER : new NamespacePredicate(namespace);
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
        return event.containsId(id) && namespaceMatcher.test(event);
    }

    @Override
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }
}
