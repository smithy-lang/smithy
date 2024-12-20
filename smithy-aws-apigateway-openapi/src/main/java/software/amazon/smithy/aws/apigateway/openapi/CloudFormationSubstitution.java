/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;

/**
 * Finds and replaces CloudFormation variables into Fn::Sub intrinsic functions.
 */
final class CloudFormationSubstitution implements ApiGatewayMapper {

    private static final Logger LOGGER = Logger.getLogger(CloudFormationSubstitution.class.getName());
    private static final String SUBSTITUTION_KEY = "Fn::Sub";
    private static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\$\\{.+}");

    /**
     * This is a hardcoded list of paths that are known to contain ARNs or identifiers
     * commonly extracted out of CloudFormation. This list may need to be updated over
     * time as new features are added. Note that this list only expands to simple
     * Fn::Sub. Anything more complex needs to be handled through JSON substitutions
     * via {@link OpenApiConfig#setSubstitutions} as can anything that does not appear
     * in this list.
     */
    private static final List<String> PATHS = Arrays.asList(
            "components/securitySchemes/*/x-amazon-apigateway-authorizer/providerARNs/*",
            "components/securitySchemes/*/x-amazon-apigateway-authorizer/authorizerCredentials",
            "components/securitySchemes/*/x-amazon-apigateway-authorizer/authorizerUri",
            "paths/*/*/x-amazon-apigateway-integration/connectionId",
            "paths/*/*/x-amazon-apigateway-integration/credentials",
            "paths/*/*/x-amazon-apigateway-integration/uri");

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST, ApiGatewayConfig.ApiType.HTTP);
    }

    @Override
    public byte getOrder() {
        return 127;
    }

    @Override
    public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
        if (!context.getConfig().getExtensions(ApiGatewayConfig.class).getDisableCloudFormationSubstitution()) {
            return node.accept(new CloudFormationFnSubInjector(PATHS)).expectObjectNode();
        }

        return node;
    }

    private static class CloudFormationFnSubInjector extends NodeVisitor.Default<Node> {
        private final Deque<String> stack = new ArrayDeque<>();
        private final List<String[]> paths;

        CloudFormationFnSubInjector(List<String> paths) {
            this.paths = paths.stream().map(path -> path.split(Pattern.quote("/"))).collect(Collectors.toList());
        }

        @Override
        protected Node getDefault(Node node) {
            return node;
        }

        @Override
        public Node arrayNode(ArrayNode node) {
            List<Node> result = new ArrayList<>();
            for (int i = 0; i < node.size(); i++) {
                Node member = node.get(i).get();
                stack.addLast(String.valueOf(i));
                result.add(member.accept(this));
                stack.removeLast();
            }

            return new ArrayNode(result, SourceLocation.NONE);
        }

        @Override
        public Node objectNode(ObjectNode node) {
            Map<StringNode, Node> result = new LinkedHashMap<>();
            for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
                stack.addLast(entry.getKey().getValue());
                result.put(entry.getKey(), entry.getValue().accept(this));
                stack.removeLast();
            }

            return new ObjectNode(result, SourceLocation.NONE);
        }

        @Override
        public Node stringNode(StringNode node) {
            if (SUBSTITUTION_PATTERN.matcher(node.getValue()).find() && isInPath()) {
                LOGGER.fine(() -> String.format(
                        "Detected CloudFormation variable syntax in %s; replacing with a `Fn::Sub` "
                                + "CloudFormation intrinsic function block",
                        node.getValue()));
                return Node.objectNode().withMember(SUBSTITUTION_KEY, node);
            }

            return node;
        }

        private boolean isInPath() {
            return paths.stream().anyMatch(this::matchesPath);
        }

        private boolean matchesPath(String[] path) {
            Iterator<String> iterator = stack.iterator();
            for (String segment : path) {
                if (!iterator.hasNext()) {
                    return false;
                }
                String current = iterator.next();
                if (!segment.equals(current) && !segment.equals("*")) {
                    return false;
                }
            }

            return true;
        }
    }
}
