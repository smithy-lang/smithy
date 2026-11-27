/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class CheckForTrailersTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(CheckForTrailersTest.class.getResource("trailers.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void throwsOnTrailers() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#Trailers"));
            OpenApiConverter.create().config(config).convert(model);
        });

        assertThat(thrown.getMessage(), containsString("trailer"));
        assertThat(thrown.getMessage(), containsString("smithy.example#DownloadFileOutput$checksum"));
    }

    @Test
    public void ignoresTrailersOutsideOfService() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#NoTrailers"));
        OpenApiConverter.create().config(config).convert(model);
    }
}
