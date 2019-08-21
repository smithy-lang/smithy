package software.amazon.smithy.build;

import java.util.concurrent.TimeUnit;

public class Test1SerialPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "test1Serial";
    }

    @Override
    public boolean isSerial() {
        return true;
    }

    @Override
    public void execute(PluginContext context) {
        try {
            TimeUnit.SECONDS.sleep(1);
            context.getFileManifest().writeFile("hello1Serial", String.format("%s", System.currentTimeMillis()));
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
