/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Replaces explicit idx traits on members with member index syntax.
 */
final class CanonicalizeMemberIndexes implements Function<TokenTree, TokenTree> {

    private static final String IDX = "smithy.protocols#idx";
    private static final String IDX_NAME = "idx";
    private static final String IDX_NAMESPACE = "smithy.protocols";

    @Override
    public TokenTree apply(TokenTree tree) {
        TreeCursor root = tree.zipper();
        if (!supportsMemberIndexes(root)) {
            return tree;
        }

        // If we're in the same namespace as the idx trait or if we've imported it then
        // we can safely check for unqualified usage.
        boolean checkUnqualifiedReference = isIdxNamespace(root) || importsIdx(root);

        // Search every member for the idx trait
        for (TreeCursor member : root.findChildrenByType(TreeType.SHAPE_MEMBER)) {
            canonicalizeMember(member, checkUnqualifiedReference);
        }
        return tree;
    }

    private static void canonicalizeMember(TreeCursor member, boolean checkUnqualifiedReference) {
        TreeCursor traits = member.getFirstChild(TreeType.TRAIT_STATEMENTS);
        if (traits == null) {
            return;
        }

        // Search for any applications of the idx trait on the member.
        // There could be more than one! There shouldn't be. But there could.
        List<IndexTrait> indexes = new ArrayList<>();
        for (TreeCursor trait : traits.getChildrenByType(TreeType.TRAIT)) {
            if (isIdxTrait(trait, checkUnqualifiedReference)) {
                IndexTrait index = parseIndexTrait(trait);
                if (index == null) {
                    return;
                }
                indexes.add(index);
            }
        }

        // If there's not any explicit trait usage, we can stop.
        if (indexes.isEmpty()) {
            return;
        }

        // Now we check for shorthand syntax usage
        TreeCursor shorthand = member.getFirstChild(TreeType.MEMBER_INDEX);

        // Every application of idx must agree on a value. If not, we bail
        // and let model validation yell at people.
        String expected = shorthand == null
                ? indexes.get(0).value
                : shorthand.getFirstChild(TreeType.NUMBER).getTree().concatTokens();
        if (indexes.stream().anyMatch(index -> !index.value.equals(expected))) {
            return;
        }

        // If there's not already a shorthand definition, add it.
        if (shorthand == null) {
            TokenTree memberIndex = TokenTree.of(TreeType.MEMBER_INDEX);
            memberIndex.appendChild(indexes.get(0).number);
            member.getTree().appendChild(memberIndex);
        }

        // Finally, remove any explicit trait usage. If there's an import,
        // that'll get cleaned up later in the pipeline.
        indexes.forEach(index -> traits.getTree().removeChild(index.trait));
        return;
    }

    private static boolean isIdxTrait(TreeCursor trait, boolean checkUnqualifiedReference) {
        TreeCursor id = trait.getFirstChild(TreeType.SHAPE_ID);
        if (id == null) {
            return false;
        }
        String value = id.getTree().concatTokens();
        return value.equals(IDX) || (checkUnqualifiedReference && value.equals(IDX_NAME));
    }

    private static IndexTrait parseIndexTrait(TreeCursor trait) {
        if (!trait.findChildrenByType(TreeType.COMMENT).isEmpty()) {
            return null;
        }

        TreeCursor body = trait.getFirstChild(TreeType.TRAIT_BODY);
        TreeCursor traitNode = body == null ? null : body.getFirstChild(TreeType.TRAIT_NODE);
        TreeCursor nodeValue = traitNode == null ? null : traitNode.getFirstChild(TreeType.NODE_VALUE);
        TreeCursor number = nodeValue == null ? null : nodeValue.getFirstChild(TreeType.NUMBER);
        if (number == null) {
            return null;
        }

        String value = number.getTree().concatTokens();
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || !Integer.toString(parsed).equals(value)) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return new IndexTrait(trait.getTree(), number.getTree(), value);
    }

    private static boolean isIdxNamespace(TreeCursor root) {
        return root.findChildrenByType(TreeType.NAMESPACE)
                .stream()
                .findFirst()
                .map(namespace -> namespace.getTree().concatTokens().equals(IDX_NAMESPACE))
                .orElse(false);
    }

    private static boolean importsIdx(TreeCursor root) {
        TreeCursor shapeSection = root.getFirstChild(TreeType.SHAPE_SECTION);
        TreeCursor useSection = shapeSection == null ? null : shapeSection.getFirstChild(TreeType.USE_SECTION);
        return useSection != null
                && useSection.findChildrenByType(TreeType.ABSOLUTE_ROOT_SHAPE_ID)
                        .stream()
                        .anyMatch(id -> id.getTree().concatTokens().equals(IDX));
    }

    private static boolean supportsMemberIndexes(TreeCursor root) {
        TreeCursor controlSection = root.getFirstChild(TreeType.CONTROL_SECTION);
        if (controlSection == null) {
            return false;
        }
        for (TreeCursor statement : controlSection.getChildrenByType(TreeType.CONTROL_STATEMENT)) {
            TreeCursor key = statement.getFirstChild(TreeType.NODE_OBJECT_KEY);
            if (key != null && key.getTree().concatTokens().equals("version")) {
                TreeCursor value = statement.getFirstChild(TreeType.NODE_VALUE);
                String version = value == null
                        ? null
                        : value.getTree()
                                .tokens()
                                .findFirst()
                                .map(CapturedToken::getStringContents)
                                .orElse(null);
                return supportsMemberIndexVersion(version);
            }
        }
        return false;
    }

    private static boolean supportsMemberIndexVersion(String version) {
        if (version == null) {
            return false;
        }

        int separator = version.indexOf('.');
        if (separator < 0 || version.indexOf('.', separator + 1) >= 0) {
            return false;
        }

        int firstMajorDigit = 0;
        while (firstMajorDigit < separator - 1 && version.charAt(firstMajorDigit) == '0') {
            firstMajorDigit++;
        }
        if (firstMajorDigit != separator - 1 || version.charAt(firstMajorDigit) != '2') {
            return false;
        }

        boolean positiveMinor = false;
        for (int i = separator + 1; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            positiveMinor |= c != '0';
        }
        return separator + 1 < version.length() && positiveMinor;
    }

    private static final class IndexTrait {
        private final TokenTree trait;
        private final TokenTree number;
        private final String value;

        private IndexTrait(TokenTree trait, TokenTree number, String value) {
            this.trait = trait;
            this.number = number;
            this.value = value;
        }
    }
}
