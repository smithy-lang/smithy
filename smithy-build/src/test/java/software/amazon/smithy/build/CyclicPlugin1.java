/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.util.List;
import software.amazon.smithy.utils.ListUtils;

public class CyclicPlugin1 implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "cyclicplugin1";
    }

    @Override
    public void execute(PluginContext context) {}

    @Override
    public List<String> runBefore() {
        return ListUtils.of("cyclicplugin2");
    }
}
