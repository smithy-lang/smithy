package software.amazon.smithy.build.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
public @interface TestProcessorAnnotation {
    String NAME = "software.amazon.smithy.build.processor.TestProcessorAnnotation";
}
