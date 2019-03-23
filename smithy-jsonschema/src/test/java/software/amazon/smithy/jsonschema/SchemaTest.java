/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class SchemaTest {
    /**
     * This is a basic integration test that makes sure each disable setting
     * can be called and doesn't break the builder.
     */
    @Test
    public void canRemoveSettings() {
        Schema.Builder builder = Schema.builder();
        var values = Set.of(
                JsonSchemaConstants.DISABLE_CONTENT_MEDIA_TYPE,
                JsonSchemaConstants.DISABLE_ADDITIONAL_PROPERTIES,
                JsonSchemaConstants.DISABLE_ALL_OF,
                JsonSchemaConstants.DISABLE_ANY_OF,
                JsonSchemaConstants.DISABLE_COMMENT,
                JsonSchemaConstants.DISABLE_CONST,
                JsonSchemaConstants.DISABLE_CONTENT_ENCODING,
                JsonSchemaConstants.DISABLE_DEFAULT,
                JsonSchemaConstants.DISABLE_DESCRIPTION,
                JsonSchemaConstants.DISABLE_ENUM,
                JsonSchemaConstants.DISABLE_EXAMPLES,
                JsonSchemaConstants.DISABLE_EXCLUSIVE_MAXIMUM,
                JsonSchemaConstants.DISABLE_EXCLUSIVE_MINIMUM,
                JsonSchemaConstants.DISABLE_FORMAT,
                JsonSchemaConstants.DISABLE_ITEMS,
                JsonSchemaConstants.DISABLE_MAX_ITEMS,
                JsonSchemaConstants.DISABLE_MAX_LENGTH,
                JsonSchemaConstants.DISABLE_MAX_PROPERTIES,
                JsonSchemaConstants.DISABLE_MAXIMUM,
                JsonSchemaConstants.DISABLE_MIN_ITEMS,
                JsonSchemaConstants.DISABLE_MIN_LENGTH,
                JsonSchemaConstants.DISABLE_MIN_PROPERTIES,
                JsonSchemaConstants.DISABLE_MINIMUM,
                JsonSchemaConstants.DISABLE_MULTIPLE_OF,
                JsonSchemaConstants.DISABLE_NOT,
                JsonSchemaConstants.DISABLE_ONE_OF,
                JsonSchemaConstants.DISABLE_PATTERN,
                JsonSchemaConstants.DISABLE_PROPERTIES,
                JsonSchemaConstants.DISABLE_PROPERTY_NAMES,
                JsonSchemaConstants.DISABLE_READ_ONLY,
                JsonSchemaConstants.DISABLE_REQUIRED,
                JsonSchemaConstants.DISABLE_TITLE,
                JsonSchemaConstants.DISABLE_UNIUQE_ITEMS,
                JsonSchemaConstants.DISABLE_WRITE_ONLY
        );

        for (var value : values) {
            builder.disableProperty(value);
        }

        builder.build();
    }

    @Test
    public void basicEqualityTest() {
        Schema a = Schema.builder().title("foo").build();
        Schema b = Schema.builder().title("foo").build();
        Schema c = Schema.builder().type("string").build();

        assertThat(a, equalTo(b));
        assertThat(a, equalTo(a));
        assertThat(b, equalTo(a));
        assertThat(c, not(equalTo(a)));
    }
}
