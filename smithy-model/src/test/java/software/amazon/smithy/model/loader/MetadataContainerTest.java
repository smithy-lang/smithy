/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class MetadataContainerTest {

    private static final SourceLocation A = new SourceLocation("a.smithy", 1, 1);
    private static final SourceLocation B = new SourceLocation("b.smithy", 2, 2);
    private static final SourceLocation C = new SourceLocation("c.smithy", 3, 3);

    private static ArrayNode array(SourceLocation location, String... values) {
        List<Node> nodes = new ArrayList<>();
        for (String value : values) {
            nodes.add(Node.from(value));
        }
        return new ArrayNode(nodes, location);
    }

    @Test
    public void concatenatesArraysInOrderWithLastLocation() {
        MetadataContainer container = new MetadataContainer();
        List<ValidationEvent> events = new ArrayList<>();
        container.putMetadata("foo", array(A, "a"), events);
        container.putMetadata("foo", array(B, "b", "c"), events);
        container.putMetadata("foo", array(C, "d"), events);

        Node result = container.getData().get("foo");

        assertThat(events, empty());
        assertThat(result, equalTo(array(SourceLocation.NONE, "a", "b", "c", "d")));
        assertThat(result.getSourceLocation(), equalTo(C));
    }

    @Test
    public void reportsConflictWhenNonArrayFollowsMergedArray() {
        MetadataContainer container = new MetadataContainer();
        List<ValidationEvent> events = new ArrayList<>();
        container.putMetadata("foo", array(A, "a"), events);
        container.putMetadata("foo", array(B, "b"), events);
        container.putMetadata("foo", new StringNode("x", C), events);

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getSeverity(), equalTo(Severity.ERROR));
        assertThat(events.get(0).getSourceLocation(), equalTo(C));
        assertThat(events.get(0).getMessage(), containsString(C.toString()));
        assertThat(events.get(0).getMessage(), containsString(B.toString()));
        assertThat(container.getData().get("foo"), equalTo(array(SourceLocation.NONE, "a", "b")));
    }

    @Test
    public void concatenatesSameLocationArrayWithDifferentContents() {
        MetadataContainer container = new MetadataContainer();
        List<ValidationEvent> events = new ArrayList<>();
        container.putMetadata("foo", array(A, "a"), events);
        container.putMetadata("foo", array(B, "b"), events);
        container.putMetadata("foo", array(B, "c"), events);

        assertThat(events, empty());
        assertThat(container.getData().get("foo"), equalTo(array(SourceLocation.NONE, "a", "b", "c")));
    }

    @Test
    public void preservesKeyOrder() {
        MetadataContainer container = new MetadataContainer();
        List<ValidationEvent> events = new ArrayList<>();
        container.putMetadata("first", array(A, "a"), events);
        container.putMetadata("second", Node.from("s"), events);
        container.putMetadata("first", array(B, "b"), events);
        container.putMetadata("third", Node.from("t"), events);

        assertThat(container.getData().keySet(), contains("first", "second", "third"));
    }

    @Test
    public void supportsRepeatedGetDataAndPutsAfterGetData() {
        MetadataContainer container = new MetadataContainer();
        List<ValidationEvent> events = new ArrayList<>();
        container.putMetadata("foo", array(A, "a"), events);
        container.putMetadata("foo", array(B, "b"), events);

        assertThat(container.getData().get("foo"), equalTo(array(SourceLocation.NONE, "a", "b")));
        assertThat(container.getData().get("foo"), equalTo(array(SourceLocation.NONE, "a", "b")));

        container.putMetadata("foo", array(C, "c"), events);
        container.putMetadata("foo", array(A, "d"), events);
        Map<String, Node> data = container.getData();

        assertThat(events, empty());
        assertThat(data.get("foo"), equalTo(array(SourceLocation.NONE, "a", "b", "c", "d")));
        assertThat(data.get("foo").getSourceLocation(), equalTo(A));
    }
}
