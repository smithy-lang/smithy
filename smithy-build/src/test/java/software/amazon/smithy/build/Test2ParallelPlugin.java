package software.amazon.smithy.build;

public class Test2ParallelPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "test2Parallel";
    }

    @Override
    public boolean isSerial() {
        return false;
    }

    @Override
    public void execute(PluginContext context) {
        context.getFileManifest().writeFile("hello2Parallel", String.format("%s", System.nanoTime()));
    }
}
