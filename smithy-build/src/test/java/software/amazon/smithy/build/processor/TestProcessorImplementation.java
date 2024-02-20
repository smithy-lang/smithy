package software.amazon.smithy.build.processor;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import software.amazon.smithy.model.node.Node;
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
    protected ObjectNode createPluginNode(TestProcessorAnnotation annotation, String packageName) {
        return Node.objectNodeBuilder()
                .withMember("packageName", packageName)
                .build();
    }
}
