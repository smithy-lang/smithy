/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

/**
 * This is a kind of integration test of the examples in the
 * specification.
 *
 * Each test "category" is prefixed with "[name]_" to group them.
 *
 * TODO: It would be great to consolidate other tests from SelectorTest here to better organize tests.
 */
public class SpecIntegTest {

    private static Model allowedTagsModel;
    private static Model attributeExistenceModel;

    @BeforeAll
    public static void before() {
        attributeExistenceModel = Model.assembler()
                .addImport(SelectorTest.class.getResource("attribute-existence.smithy"))
                .assemble()
                .unwrap();
        allowedTagsModel = Model.assembler()
                .addImport(SelectorTest.class.getResource("allowed-tags-example.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void attributeExistence_MatchesShapesWithTrait() {
        // Simple existence check.
        assertThat(SelectorTest.exampleIds(attributeExistenceModel, "[trait|deprecated]"),
                containsInAnyOrder("smithy.example#DeprecatedString"));

        // Empty tags traits still exist.
        assertThat(SelectorTest.exampleIds(attributeExistenceModel, "[trait|tags]"),
                containsInAnyOrder("smithy.example#MyString2"));

        assertThat(SelectorTest.exampleIds(attributeExistenceModel, "[trait|enum]"),
                containsInAnyOrder("smithy.example#MyString3",
                        "smithy.example#MyString4",
                        "smithy.example#MyString5"));
    }

    @Test
    public void attributeExistence_MatchesShapesWithProjection() {
        // Empty projections don't exist.
        assertThat(SelectorTest.exampleIds(attributeExistenceModel, "[trait|tags|(values)]"), empty());

        // An empty projection does not exist.
        assertThat(SelectorTest.exampleIds(attributeExistenceModel, "[trait|enum|(values)]"),
                containsInAnyOrder("smithy.example#MyString3",
                        "smithy.example#MyString4",
                        "smithy.example#MyString5"));

        assertThat(SelectorTest.exampleIds(attributeExistenceModel, "[trait|enum|(values)|tags]"),
                containsInAnyOrder("smithy.example#MyString5"));

        // An empty projection does not exist.
        assertThat(SelectorTest.exampleIds(attributeExistenceModel, "[trait|enum|(values)|tags|(values)]"),
                containsInAnyOrder("smithy.example#MyString5"));
    }

    @Test
    public void allowedTags_MatchesShapesThatViolateExample() {
        Set<String> ids = SelectorTest.ids(allowedTagsModel,
                "service\n"
                        + "[trait|smithy.example#allowedTags]\n"
                        + "$service(*)\n"
                        + "~>\n"
                        + "[trait|tags]\n"
                        + ":not([@: @{trait|tags|(values)} = @{var|service|trait|smithy.example#allowedTags|(values)}])");

        assertThat(ids, contains("smithy.example#OperationD"));
    }

    @Test
    public void allowedTags_illustratesWhyProjectionComparatorsExist() {
        Set<String> noneMatch = SelectorTest.ids(allowedTagsModel,
                "service\n"
                        + "[trait|smithy.example#allowedTags]\n"
                        + "$service(*)\n"
                        + "~>\n"
                        + "[trait|enum]\n"
                        + ":not([@: @{trait|enum|(values)|tags|(values)}"
                        + "         = @{var|service|trait|smithy.example#allowedTags|(values)}])");

        Set<String> notWhatWeWanted = SelectorTest.ids(allowedTagsModel,
                "service\n"
                        + "[trait|smithy.example#allowedTags]\n"
                        + "$service(*)\n"
                        + "~>\n"
                        + "[trait|enum]\n"
                        + "[@: @{trait|enum|(values)|tags|(values)}"
                        + "    != @{var|service|trait|smithy.example#allowedTags|(values)}]");

        assertThat(noneMatch, empty());
        assertThat(notWhatWeWanted, containsInAnyOrder("smithy.example#GoodEnum", "smithy.example#BadEnum"));
    }

    @Test
    public void allowedTags_matchesEnumsUsingSubset() {
        Set<String> ids = SelectorTest.ids(allowedTagsModel,
                "service\n"
                        + "[trait|smithy.example#allowedTags]\n"
                        + "$service(*)\n"
                        + "~>\n"
                        + "[trait|enum]\n"
                        + ":not([@: @{trait|enum|(values)|tags|(values)}"
                        + "         {<} @{var|service|trait|smithy.example#allowedTags|(values)}])");

        assertThat(ids, contains("smithy.example#BadEnum"));
    }
}
