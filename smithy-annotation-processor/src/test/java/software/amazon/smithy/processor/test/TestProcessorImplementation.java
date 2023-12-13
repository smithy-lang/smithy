package software.amazon.smithy.processor.test;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.processor.SmithyAnnotationProcessor;
import software.amazon.smithy.model.node.ObjectNode;

@SupportedAnnotationTypes(TestProcessorAnnotation.NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TestProcessorImplementation extends SmithyAnnotationProcessor<TestProcessorAnnotation> {
    @Override
    protected String getPluginName() {
        return "test-plugin";
    }

    @Override
    protected Class<TestProcessorAnnotation> getAnnotationClass() {
        return TestProcessorAnnotation.class;
    }

    @Override
    protected ObjectNode createPluginNode(TestProcessorAnnotation annotation) {
        return Node.objectNodeBuilder()
                .withMember("namespace", annotation.namespace())
                .build();
    }
}
