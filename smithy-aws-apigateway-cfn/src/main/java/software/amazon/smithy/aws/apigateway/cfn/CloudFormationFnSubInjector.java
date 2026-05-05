/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

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

/**
 * Walks a Smithy JSON AST node tree and wraps string values containing
 * CloudFormation variable syntax in Fn::Sub intrinsic function objects.
 */
final class CloudFormationFnSubInjector extends NodeVisitor.Default<Node> {

    private static final Logger LOGGER = Logger.getLogger(CloudFormationFnSubInjector.class.getName());
    private static final String SUBSTITUTION_KEY = "Fn::Sub";
    private static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\$\\{.+}");

    static final List<String> PATHS = Arrays.asList(
            "shapes/*/traits/aws.apigateway#integration/uri",
            "shapes/*/traits/aws.apigateway#integration/credentials",
            "shapes/*/traits/aws.apigateway#integration/connectionId",
            "shapes/*/traits/aws.apigateway#integration/integrationTarget",
            "shapes/*/traits/aws.apigateway#authorizers/*/uri",
            "shapes/*/traits/aws.apigateway#authorizers/*/credentials",
            "shapes/*/traits/aws.auth#cognitoUserPools/providerArns/*");

    private final Deque<String> stack = new ArrayDeque<>();
    private final List<String[]> paths;

    CloudFormationFnSubInjector() {
        this(PATHS);
    }

    CloudFormationFnSubInjector(List<String> paths) {
        this.paths = paths.stream()
                .map(path -> path.split(Pattern.quote("/")))
                .collect(Collectors.toList());
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
                    "Wrapping CloudFormation variable in Fn::Sub at path %s: %s",
                    String.join("/", stack),
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
