/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.time.Instant;
import java.util.List;
import software.amazon.smithy.utils.ListUtils;

public class TimestampPlugin2 implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "timestampPlugin2";
    }

    @Override
    public void execute(PluginContext context) {
        context.getFileManifest().writeFile("timestamp", String.valueOf(Instant.now().toEpochMilli()));
        try {
            Thread.sleep(1);
        } catch (InterruptedException ignored) {}
    }

    @Override
    public List<String> runBefore() {
        return ListUtils.of("timestampPlugin1");
    }

    @Override
    public boolean isSerial() {
        return true;
    }
}
