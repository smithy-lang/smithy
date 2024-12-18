/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import java.util.List;
import software.amazon.smithy.cli.commands.SmithyCommand;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.MavenDependencyResolver;
import software.amazon.smithy.utils.IoUtils;

/**
 * Entry point of the Smithy CLI.
 */
public final class SmithyCli {

    private ClassLoader classLoader;
    private DependencyResolver.Factory dependencyResolverFactory;

    private SmithyCli() {}

    /**
     * Creates a new instance of the CLI.
     *
     * @return Returns the CLI instance.
     */
    public static SmithyCli create() {
        return new SmithyCli();
    }

    /**
     * Executes the CLI.
     *
     * @param args Arguments to parse and execute.
     */
    public static void main(String... args) {
        try {
            int exitCode = SmithyCli.create().run(args);
            // Only exit with a non-zero status on error since 0 is the default exit code.
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        } catch (CliError e) {
            System.exit(e.code);
        } catch (Exception e) {
            System.exit(1);
        }
    }

    /**
     * Sets a custom class loader to use when executing commands.
     *
     * @param classLoader Class loader used to find models, traits, etc.
     * @return Returns the CLI.
     */
    public SmithyCli classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Sets a custom dependency resolver factory to use when resolving dependencies.
     *
     * <p>Note that the CLI will automatically handle caching the resolved classpath and ensuring that
     * resolved dependencies are consistent with the versions of JARs used by the CLI.
     *
     * @param dependencyResolverFactory Factory to use when resolving dependencies.
     * @return Returns the CLI.
     */
    public SmithyCli dependencyResolverFactory(DependencyResolver.Factory dependencyResolverFactory) {
        this.dependencyResolverFactory = dependencyResolverFactory;
        return this;
    }

    /**
     * Runs the CLI using a list of arguments.
     *
     * @param args Arguments to parse and execute.
     * @return Returns the exit code.
     */
    public int run(List<String> args) {
        return run(args.toArray(new String[0]));
    }

    /**
     * Runs the CLI.
     *
     * @param args Arguments to parse and execute.
     * @return Returns the exit code.
     */
    public int run(String... args) {
        return createCli().run(args);
    }

    /**
     * Creates a runnable CLI.
     *
     * @return Returns the created CLI.
     */
    public Cli createCli() {
        if (dependencyResolverFactory == null) {
            dependencyResolverFactory = (config, env) -> {
                return new MavenDependencyResolver(EnvironmentVariable.SMITHY_MAVEN_CACHE.get());
            };
        }

        return new Cli(new SmithyCommand(dependencyResolverFactory), classLoader);
    }

    /**
     * Get the Smithy CLI version of the running CLI.
     *
     * @return Returns the CLI version (e.g., "1.26.0").
     */
    public static String getVersion() {
        return IoUtils.readUtf8Resource(SmithyCli.class, "cli-version").trim();
    }
}
