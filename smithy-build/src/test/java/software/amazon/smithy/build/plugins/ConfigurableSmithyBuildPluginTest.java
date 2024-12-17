/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;

public class ConfigurableSmithyBuildPluginTest {
    @Test
    public void loadsConfigurationClass() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/a.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .settings(Node.objectNode()
                        .withMember("foo", "hello")
                        .withMember("bar", 10)
                        .withMember("baz", true))
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .build();
        new Configurable().execute(context);

        String manifestString = manifest.getFileString("Config").get();
        assertThat(manifestString, containsString("hello"));
        assertThat(manifestString, containsString("10"));
        assertThat(manifestString, containsString("true"));
    }

    private static final class Configurable extends ConfigurableSmithyBuildPlugin<Config> {
        @Override
        public String getName() {
            return "configurable";
        }

        @Override
        public Class<Config> getConfigType() {
            return Config.class;
        }

        @Override
        protected void executeWithConfig(PluginContext context, Config config) {
            NodeMapper mapper = new NodeMapper();
            mapper.serialize(config);
            context.getFileManifest().writeJson("Config", mapper.serialize(config));
        }
    }

    public static final class Config {
        private String foo;
        private int bar;
        private boolean baz;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public int getBar() {
            return bar;
        }

        public void setBar(int bar) {
            this.bar = bar;
        }

        public boolean isBaz() {
            return baz;
        }

        public void setBaz(boolean baz) {
            this.baz = baz;
        }
    }
}
