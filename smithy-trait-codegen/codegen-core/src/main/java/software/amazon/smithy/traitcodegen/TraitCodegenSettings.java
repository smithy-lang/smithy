/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.ObjectNode;

public final class TraitCodegenSettings {
    private final String packageName;
    private final List<String> headerLines;
    private final List<String> excludeTags;

    TraitCodegenSettings(String packageName, List<String> headerLines, List<String> excludeTags) {
        this.packageName = packageName;
        this.headerLines = headerLines;
        this.excludeTags = excludeTags;
    }

    public static TraitCodegenSettings from(ObjectNode settingsNode) {
        return new TraitCodegenSettings(
                settingsNode.expectStringMember("package").getValue(),
                settingsNode.expectArrayMember("header")
                        .getElementsAs(el -> el.expectStringNode().getValue()),
                settingsNode.getArrayMember("excludeTags")
                        .map(n -> n.getElementsAs(el -> el.expectStringNode().getValue()))
                        .orElse(new ArrayList<>())
        );
    }

    public String packageName() {
        return packageName;
    }

    public List<String> headerLines() {
        return headerLines;
    }

    public List<String> excludeTags() {
        return excludeTags;
    }
}
