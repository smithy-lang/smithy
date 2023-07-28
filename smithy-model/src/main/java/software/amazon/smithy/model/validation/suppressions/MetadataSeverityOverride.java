/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.function.Predicate;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

final class MetadataSeverityOverride implements SeverityOverride {

    private static final String ID = "id";
    private static final String NAMESPACE = "namespace";
    private static final String SEVERITY = "severity";
    private static final Collection<String> KEYS = ListUtils.of(ID, NAMESPACE, SEVERITY);
    private static final Predicate<ValidationEvent> STAR_MATCHER = event -> true;
    private static final String[] SEVERITIES = {"WARNING", "DANGER"};

    private final String id;
    private final String namespace;
    private final Severity severity;
    private final Predicate<ValidationEvent> namespaceMatcher;

    MetadataSeverityOverride(String id, String namespace, Severity severity) {
        this.id = id;
        this.namespace = namespace;
        this.severity = severity;
        this.namespaceMatcher = namespace.equals("*") ? STAR_MATCHER : new NamespacePredicate(this.namespace);
    }

    static MetadataSeverityOverride fromNode(Node node) {
        ObjectNode rule = node.expectObjectNode();
        rule.warnIfAdditionalProperties(KEYS);
        String id = rule.expectStringMember(ID).getValue();
        String namespace = rule.expectStringMember(NAMESPACE).getValue();
        String severity = rule.expectStringMember(SEVERITY).expectOneOf(SEVERITIES);
        return new MetadataSeverityOverride(id, namespace, Severity.valueOf(severity));
    }

    @Override
    public Severity apply(ValidationEvent event) {
        return event.containsId(id) && namespaceMatcher.test(event) ? severity : event.getSeverity();
    }
}
