/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class SnippetConfigTest {
    private static final ShapeId serviceId = ShapeId.from("com.example#BirdService");
    private static final ShapeId operationId = ShapeId.from("com.example#ListBirds");
    private static SnippetConfig snippetConfig;

    @BeforeAll
    public static void setup() {
        String id = "List Crows";
        Snippet pythonSnippet = Snippet.builder()
                .targetId(id)
                .title("Python")
                .addFile(SnippetFile.builder()
                        .language("python")
                        .filename("main.py")
                        .content(
                                "from bird_service import BirdClient\n\n"
                                        + "client = BirdClient()\n"
                                        + "response = await client.list_birds(ListBirdsInput(genus=\"corvus\"))\n"
                                        + "assert response == ListBirdsOutput(birds=[\n"
                                        + "    Bird(\n"
                                        + "        order=\"passiformes\",\n"
                                        + "        family=\"corvidae\",\n"
                                        + "        genus=\"corvus\",\n"
                                        + "        species=\"cornix\",\n"
                                        + "        common_names={\n"
                                        + "            \"en_US\": [\"Hooded Crow\"],\n"
                                        + "            \"de_DE\": [\"Nebelkrähe\"],\n"
                                        + "        },\n"
                                        + "    ),\n"
                                        + "])")
                        .build())
                .build();
        Snippet pythonProtocolSnippet = Snippet.builder()
                .targetId(id)
                .title("Python (restJson1)")
                .protocol(ShapeId.from("aws.protocols#restJson1"))
                .addFile(SnippetFile.builder()
                        .language("python")
                        .filename("main.py")
                        .content(
                                "from bird_service import BirdClient\n\n"
                                        + "client = BirdClient(protocol=RestJson1Protocol())\n"
                                        + "response = await client.list_birds(ListBirdsInput(genus=\"corvus\"))\n"
                                        + "assert response == ListBirdsOutput(birds=[\n"
                                        + "    Bird(\n"
                                        + "        order=\"passiformes\",\n"
                                        + "        family=\"corvidae\",\n"
                                        + "        genus=\"corvus\",\n"
                                        + "        species=\"cornix\",\n"
                                        + "        common_names={\n"
                                        + "            \"en_US\": [\"Hooded Crow\"],\n"
                                        + "            \"de_DE\": [\"Nebelkrähe\"],\n"
                                        + "        },\n"
                                        + "    ),\n"
                                        + "])")
                        .build())
                .build();
        Snippet httpSnippet = Snippet.builder()
                .targetId(id)
                .title("HTTP")
                .protocol(ShapeId.from("aws.protocols#restJson1"))
                .addFile(SnippetFile.builder()
                        .language("http")
                        .filename("request")
                        .content(
                                "POST /birds HTTP/1.1\n"
                                        + "Host: com.example.birds\n"
                                        + "Content-Type: application/json\n"
                                        + "Content-Length: 25\n\n"
                                        + "{\n"
                                        + "    \"genus\": \"corvus\"\n"
                                        + "}")
                        .build())
                .addFile(SnippetFile.builder()
                        .language("http")
                        .filename("response")
                        .content(
                                "HTTP/1.1 200 OK\n"
                                        + "Content-Type: application/json\n"
                                        + "Content-Length: 248\n\n"
                                        + "{\n"
                                        + "    \"birds\": [{\n"
                                        + "        \"order\": \"passiformes\"\n"
                                        + "        \"family\": \"corvidae\"\n"
                                        + "        \"genus\": \"corvus\"\n"
                                        + "        \"species\": \"cornix\"\n"
                                        + "        \"commonNames\": {\n"
                                        + "            \"en_US\": [\"Hooded Crow\"]\n"
                                        + "            \"de_DE\": [\"Nebelkrähe\"]\n"
                                        + "        }\n"
                                        + "    }]\n"
                                        + "}")
                        .build())
                .build();
        List<Snippet> snippets = ListUtils.of(pythonSnippet, pythonProtocolSnippet, httpSnippet);
        snippetConfig = SnippetConfig.builder()
                .putServiceSnippets(serviceId, MapUtils.of(operationId, snippets))
                .build();
    }

    @Test
    public void serializesToNode() throws Exception {
        Node result = snippetConfig.toNode();
        Node expected = Node.parse(getClass().getResource("snippet-config.json").openStream());
        assertEquals(result, expected);
    }

    @Test
    public void parsesNode() throws Exception {
        SnippetConfig actual = SnippetConfig.load(Paths.get(getClass().getResource("snippet-config.json").toURI()));
        assertEquals(actual, snippetConfig);
    }

    @Test
    public void roundTrips() throws Exception {
        Node expected = Node.parse(getClass().getResource("snippet-config.json").openStream());
        Node actual = SnippetConfig.fromNode(expected).toNode();
        Node.assertEquals(actual, expected);
    }

    @Test
    public void canMergeNewSnippets() {
        Snippet newSnippet = Snippet.builder()
                .targetId("newSnippet")
                .title("newSnippet")
                .addFile(SnippetFile.builder()
                        .language("txt")
                        .filename("newSnippet.txt")
                        .content("foo")
                        .build())
                .build();
        SnippetConfig merged = snippetConfig.toBuilder()
                .mergeSnippets(MapUtils.of(serviceId, MapUtils.of(operationId, ListUtils.of(newSnippet))))
                .build();

        List<Snippet> oldList = snippetConfig.getShapeSnippets(serviceId, operationId);
        List<Snippet> newList = merged.getShapeSnippets(serviceId, operationId);
        for (int i = 0; i < oldList.size(); i++) {
            assertEquals(oldList.get(i), newList.get(i));
        }
        assertEquals(newList.get(newList.size() - 1), newSnippet);
    }
}
