package software.amazon.smithy.processor.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
public @interface TestProcessorAnnotation {
    String NAME = "software.amazon.smithy.processor.test.TestProcessorAnnotation";

    String namespace();
}
