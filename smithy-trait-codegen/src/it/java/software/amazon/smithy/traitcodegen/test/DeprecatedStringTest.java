package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.traits.DeprecatedStringTrait;
import org.junit.jupiter.api.Test;

class DeprecatedStringTest {
    @Test
    void checkForDeprecatedAnnotation() {
        Deprecated deprecated = DeprecatedStringTrait.class.getAnnotation(Deprecated.class);
        assertNotNull(deprecated);
    }
}
