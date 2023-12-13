package software.amazon.smithy.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
public @interface TestProcessorAnnotation {
    String NAME = "software.amazon.smithy.processor.TestProcessorAnnotation";

    String namespace();
}
