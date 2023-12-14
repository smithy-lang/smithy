package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.traits.DeprecatedStringTraitTrait;
import org.junit.jupiter.api.Test;

class DeprecatedStringTest {
    @Test
    void checkForDeprecatedAnnotation() {
        Deprecated deprecated = DeprecatedStringTraitTrait.class.getAnnotation(Deprecated.class);
        assertNotNull(deprecated);
    }
}
