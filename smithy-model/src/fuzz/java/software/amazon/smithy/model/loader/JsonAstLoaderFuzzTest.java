/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import com.code_intelligence.jazzer.junit.FuzzTest;
import java.nio.charset.StandardCharsets;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * Fuzzes the JSON AST model loader end to end.
 *
 * <p>Arbitrary bytes are fed to the assembler as a {@code .json} model. Loading must never escape with
 * anything other than {@link SourceException} (the loader's declared failure mode); malformed input is
 * expected to surface as validation events, not crashes, hangs, or unexpected exception types.
 *
 * <p>Also exercises the already-parsed-node path ({@code addDocumentNode}) so both {@code AstReader}
 * implementations — {@code JsonAstReader} and {@code NodeAstReader} — are driven by the same loader.
 */
class JsonAstLoaderFuzzTest {

    private static final int MAX_INPUT = 64 * 1024;

    @FuzzTest
    void loadUnparsedJsonModel(byte[] data) {
        if (data.length > MAX_INPUT) {
            return;
        }
        String json = new String(data, StandardCharsets.UTF_8);
        try {
            ValidatedResult<Model> result = Model.assembler()
                    .addUnparsedModel("fuzz.json", json)
                    .assemble();
            // Touching the result must also be safe regardless of validation outcome.
            result.getValidationEvents();
            result.getResult();
        } catch (SourceException expected) {
            // The loader's declared failure mode for malformed models.
        }
    }

    @FuzzTest
    void loadDocumentNodeModel(byte[] data) {
        if (data.length > MAX_INPUT) {
            return;
        }
        String json = new String(data, StandardCharsets.UTF_8);
        Node node;
        try {
            node = Node.parse(json);
        } catch (ModelSyntaxException unparseable) {
            return; // Not valid JSON; the node path requires an already-parsed node.
        }
        try {
            Model.assembler().addDocumentNode(node).assemble().getValidationEvents();
        } catch (SourceException expected) {
            // Allowed.
        }
    }
}
