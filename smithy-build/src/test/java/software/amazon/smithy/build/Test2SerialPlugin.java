package software.amazon.smithy.build;

import java.util.concurrent.TimeUnit;

public class Test2SerialPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "testSerial2";
    }

    @Override
    public boolean isSerial() {
        return true;
    }

    @Override
    public void execute(PluginContext context) {
        try {
            TimeUnit.SECONDS.sleep(1);
            context.getFileManifest().writeFile("hello2Serial", String.format("%s", System.currentTimeMillis()));
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
