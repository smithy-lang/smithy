package software.amazon.smithy.openapi.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ExternalDocumentationTest {
    private static final ExternalDocumentation DOC_1 =
        ExternalDocumentation.builder()
            .url("url1")
            .description("description1")
            .build();

    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(ExternalDocumentation.builder().url("url1").description("description1").build(), 0),
            Arguments.of(ExternalDocumentation.builder().url("url1").description("description0").build(), 1),
            Arguments.of(ExternalDocumentation.builder().url("url1").description("description2").build(), -1),
            Arguments.of(ExternalDocumentation.builder().url("url0").description("description1").build(), 1),
            Arguments.of(ExternalDocumentation.builder().url("url2").description("description1").build(), -1),
            Arguments.of(ExternalDocumentation.builder().url("url1").build(), 1)
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void testCompareTo(ExternalDocumentation doc2, int expected) {
        assertThat(DOC_1.compareTo(doc2), equalTo(expected));
    }
}