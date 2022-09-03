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

package software.amazon.smithy.build.plugins;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.BoxTrait;

/**
 * Generates a JSON report that contains a mapping of every structure member to
 * whether the member is considered nullable in v1 and v2 implementations.
 */
public final class NullabilityReportPlugin implements SmithyBuildPlugin {

    static final String NULLABILITY_REPORT_PATH = "nullability-report.json";
    private static final String NAME = "nullabilityReport";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean requiresValidModel() {
        return false;
    }

    @Override
    public void execute(PluginContext context) {
        if (context.getOriginalModel().isPresent() && context.getProjection().isPresent()) {
            context.getFileManifest().writeJson(NULLABILITY_REPORT_PATH, serializeReport(context));
        }
    }

    private static Node serializeReport(PluginContext context) {
        ObjectNode.Builder root = Node.objectNodeBuilder();
        Model model = context.getModel();
        NullableIndex index = NullableIndex.of(model);

        for (StructureShape structure : model.getStructureShapes()) {
            // Only generate for structures in "sources" that have members.
            if (!structure.getAllMembers().isEmpty() && context.isSourceShape(structure)) {
                ObjectNode.Builder struct = Node.objectNodeBuilder();
                for (MemberShape member : structure.getAllMembers().values()) {
                    ObjectNode.Builder entry = Node.objectNodeBuilder();
                    entry.withMember("v1", index.isNullable(member));
                    entry.withMember("v1-via-box", member.getMemberTrait(model, BoxTrait.class).isPresent());
                    entry.withMember("v2-client",
                                     index.isMemberNullable(member, NullableIndex.CheckMode.CLIENT));
                    entry.withMember("v2-client-careful",
                                     index.isMemberNullable(member, NullableIndex.CheckMode.CLIENT_CAREFUL));
                    entry.withMember("v2-server",
                                     index.isMemberNullable(member, NullableIndex.CheckMode.SERVER));
                    struct.withMember(member.getMemberName(), entry.build());
                }
                root.withMember(structure.getId().toString(), struct.build());
            }
        }

        return root.build();
    }
}
