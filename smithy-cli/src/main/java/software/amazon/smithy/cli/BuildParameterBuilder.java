/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * This builder can be used to build up a Smithy CLI command to
 * run `smithy build`.
 *
 * <p>This builder will take into account the "lib" and "build" classpaths
 * that points to JAR files when determining which JARs are added as sources
 * and which JARs are added as part of model discovery. This is determined
 * based on the value provided for {@link #projectionSource}.
 *
 * <p>If {@code projectionSource} is not set or set to "source", then classes
 * will be loaded using the lib and build classpath. However, models are
 * discovered only using the lib classpath, ensuring that consumers of the
 * generated JAR can find the required models using only the declared
 * dependencies of this package.
 *
 * <p>If {@code projectionSource} is not set to something other than
 * "source", then we build the model and discover models using only the build
 * classpath. This provides a layer of isolation between the sources that
 * created a projection from the downstream consumers of the projected model.
 * {@code projectionSourceTags} can be provided to find JARs in the build
 * classpath that have a META-INF/MANIFEST.MF "Smithy-Tags" value that matches
 * one or more of the provided tags. This is used to select which models from
 * your dependencies should be considered "sources" so that they show up in
 * the JAR you're projecting.
 */
public final class BuildParameterBuilder {
    private static final Logger LOGGER = Logger.getLogger(BuildParameterBuilder.class.getName());
    private static final String SMITHY_TAG_PROPERTY = "Smithy-Tags";
    private static final String SOURCE = "source";
    private static final String PATH_SEPARATOR = "path.separator";

    private String projectionSource = SOURCE;
    private Set<String> projectionSourceTags = new LinkedHashSet<>();
    private Set<String> buildClasspath = new LinkedHashSet<>();
    private Set<String> libClasspath = new LinkedHashSet<>();
    private Set<String> sources = new LinkedHashSet<>();
    private ClassPathTagMatcher tagMatcher;
    private Set<String> configs = new LinkedHashSet<>();
    private String output;
    private String projection;
    private String plugin;
    private boolean discover;
    private boolean allowUnknownTraits;
    private List<String> extraArgs = new ArrayList<>();

    /**
     * Sets the name of the projection being built as a source.
     *
     * <p>This means that the given projection is used to populate
     * a build artifact (for example, a JAR being built by Gradle).
     *
     * @param projectionSource Projection name.
     * @return Returns the builder.
     */
    public BuildParameterBuilder projectionSource(String projectionSource) {
        this.projectionSource = projectionSource == null || projectionSource.isEmpty() ? SOURCE : projectionSource;
        return this;
    }

    /**
     * Adds a collection of "source" model files.
     *
     * @param sources Sources to add.
     * @return Returns the builder.
     */
    public BuildParameterBuilder sources(Collection<String> sources) {
        if (sources != null) {
            this.sources.addAll(sources);
        }

        return this;
    }

    /**
     * Adds a collection of "source" model files only if they exist.
     *
     * @param sources Sources to add.
     * @return Returns the builder.
     */
    public BuildParameterBuilder addSourcesIfExists(Collection<String> sources) {
        if (sources != null) {
            for (String source : sources) {
                if (!source.isEmpty() && Files.exists(Paths.get(source))) {
                    this.sources.add(source);
                } else {
                    LOGGER.info("Skipping source that does not exist: " + source);
                }
            }
        }

        return this;
    }

    /**
     * Sets the build classpath.
     *
     * @param buildClasspath Classpath to set.
     * @return Returns the builder.
     */
    public BuildParameterBuilder buildClasspath(String buildClasspath) {
        this.buildClasspath.addAll(splitAndFilterString(System.getProperty(PATH_SEPARATOR), buildClasspath));
        return this;
    }

    private static Set<String> splitAndFilterString(String delimiter, String value) {
        if (value == null) {
            return SetUtils.of();
        }

        return Stream.of(value.split(Pattern.quote(delimiter)))
                .map(String::trim)
                .filter(FunctionalUtils.not(String::isEmpty))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Sets the lib classpath.
     *
     * @param libClasspath Classpath to set.
     * @return Returns the builder.
     */
    public BuildParameterBuilder libClasspath(String libClasspath) {
        this.libClasspath.addAll(splitAndFilterString(System.getProperty(PATH_SEPARATOR), libClasspath));
        return this;
    }

    /**
     * Sets the tags to find in the build classpath when projecting a JAR.
     *
     * <p>Tags can only be provided if {@link #projectionSource} has been set to
     * something other than "source".
     *
     * @param projectionSourceTags Comma separated list of projection tags.
     * @return Returns the builder.
     */
    public BuildParameterBuilder projectionSourceTags(String projectionSourceTags) {
        return projectionSourceTags(splitAndFilterString(",", projectionSourceTags));
    }

    /**
     * Sets the tags to find in the build classpath when projecting a JAR.
     *
     * <p>Tags can only be provided if {@link #projectionSource} has been set to
     * something other than "source".
     *
     * @param projectionSourceTags Projection tags.
     * @return Returns the builder.
     */
    public BuildParameterBuilder projectionSourceTags(Collection<String> projectionSourceTags) {
        if (projectionSourceTags != null) {
            this.projectionSourceTags.addAll(projectionSourceTags);
        }

        return this;
    }

    /**
     * Sets the implementation used to find JARs with the given tags.
     *
     * <p>A default implementation that loads JARs is automatically used if
     * an explicit implementation is not specified. You probably only need
     * to provide a custom implementation for testing.
     *
     * @param tagMatcher Tag matching implementation.
     * @return Returns the builder.
     */
    public BuildParameterBuilder tagMatcher(ClassPathTagMatcher tagMatcher) {
        this.tagMatcher = Objects.requireNonNull(tagMatcher);
        return this;
    }

    /**
     * Adds a configuration file to the builder.
     *
     * @param pathToConfig Path to the configuration file.
     * @return Returns the builder.
     */
    public BuildParameterBuilder addConfig(String pathToConfig) {
        if (pathToConfig != null && !pathToConfig.isEmpty()) {
            configs.add(pathToConfig);
        }

        return this;
    }

    /**
     * Adds a configuration file to the builder only if it exists.
     *
     * <p>This method is ignored if the file is null, empty, or does
     * not exist.
     *
     * @param pathToConfig Path to the configuration file.
     * @return Returns the builder.
     */
    public BuildParameterBuilder addConfigIfExists(String pathToConfig) {
        if (pathToConfig == null || pathToConfig.isEmpty()) {
            return this;
        } else if (!Files.exists(Paths.get(pathToConfig))) {
            LOGGER.info("Not setting --config to " + pathToConfig + " because it does not exist");
            return this;
        }

        return addConfig(pathToConfig);
    }

    /**
     * Sets the optional output directory.
     *
     * @param output Optional output directory to set.
     * @return Returns the builder.
     */
    public BuildParameterBuilder output(String output) {
        this.output = output;
        return this;
    }

    /**
     * Ensures that only the given projection is built. All other
     * projections are skipped.
     *
     * <p>This is not the same as calling {@link #projectionSource}.
     *
     * @param projection Projection to build, excluding others.
     * @return Returns the builder.
     */
    public BuildParameterBuilder projection(String projection) {
        this.projection = projection;
        return this;
    }

    /**
     * Ensures that only the given plugin is built in each projection.
     * All other plugins are skipped.
     *
     * @param plugin Plugin to build, excluding others.
     * @return Returns the builder.
     */
    public BuildParameterBuilder plugin(String plugin) {
        this.plugin = plugin;
        return this;
    }

    /**
     * Enables model discovery.
     *
     * @param discover Set to true to enable model discovery.
     * @return Returns the builder.
     */
    public BuildParameterBuilder discover(boolean discover) {
        this.discover = discover;
        return this;
    }

    /**
     * Ignores unknown traits when building models.
     *
     * @param allowUnknownTraits Set to true to allow unknown traits.
     * @return Returns the builder.
     */
    public BuildParameterBuilder allowUnknownTraits(boolean allowUnknownTraits) {
        this.allowUnknownTraits = allowUnknownTraits;
        return this;
    }

    /**
     * Adds extra arguments to the CLI arguments before positional arguments.
     *
     * @param args Arguments to add.
     * @return Returns the builder.
     */
    public BuildParameterBuilder addExtraArgs(String... args) {
        Collections.addAll(extraArgs, Objects.requireNonNull(args));
        return this;
    }

    /**
     * Computes the result object that is used when running smithy build.
     *
     * @return Returns the computed result.
     */
    public Result build() {
        if (projectionSource.equals(SOURCE)) {
            return configureSourceProjection();
        }

        // Create a default tag matcher.
        if (tagMatcher == null) {
            tagMatcher = new JarFileClassPathTagMatcher();
        }

        return configureProjection();
    }

    /**
     * When building the source projection, classes will be loaded using
     * the lib and build tool classpath. However, models are discovered only
     * using the lib classpath, ensuring that consumers of the generated JAR
     * can find the required models using only the declared dependencies of
     * this package.
     *
     * @return Returns the result.
     */
    private Result configureSourceProjection() {
        LOGGER.info("Configuring SmithyBuild classpaths for the `source` projection");

        if (!projectionSourceTags.isEmpty()) {
            throw new SmithyBuildException("Projection source tags cannot be set when building a source projection.");
        }

        // Create a discovery classpath that ensures that sources are not added
        // to the discovery classpath.
        Set<String> computedDiscovery = new LinkedHashSet<>(libClasspath);
        computedDiscovery.removeAll(sources);

        if (!discover) {
            computedDiscovery.clear();
        }

        // Create the combined classpath that contains build and lib dependencies.
        Set<String> combined = new LinkedHashSet<>(libClasspath);
        combined.addAll(buildClasspath);

        String discoveryClasspath = String.join(System.getProperty(PATH_SEPARATOR), computedDiscovery);
        String classpath = String.join(System.getProperty(PATH_SEPARATOR), combined);
        return new Result(this, discoveryClasspath, classpath, sources);
    }

    /**
     * When a projection is applied, we build the model and discover models
     * using only the build tool classpath. This provides a layer of isolation
     * between the sources that created a projection from the downstream
     * consumers of the projected model.
     *
     * @return Returns the result.
     */
    private Result configureProjection() {
        if (projectionSourceTags.isEmpty()) {
            LOGGER.warning("No projection source tags were set for the projection `" + projection + "`, so the "
                    + "projection will not have any sources in it other than files found in the sources of "
                    + "the package being built.");
            String buildCp = String.join(System.getProperty(PATH_SEPARATOR), buildClasspath);
            return new Result(this, buildCp, buildCp, sources);
        }

        LOGGER.fine("Configuring Smithy classpaths for projection `" + projection + "`");

        // Find all JARs that have a matching tag and add them to sources.
        Set<String> computedSources = new LinkedHashSet<>(sources);
        Set<String> tagSourceJars = tagMatcher.findJarsWithMatchingTags(buildClasspath, projectionSourceTags);
        computedSources.addAll(tagSourceJars);
        LOGGER.info("Found the following JARs that matched the Smithy projection tags query: " + tagSourceJars);

        // Create a discovery classpath that ensures that sources (both explicit and
        // discovered through tags) are not added to the discovery classpath.
        Set<String> computedDiscovery = new LinkedHashSet<>(buildClasspath);
        computedDiscovery.removeAll(computedSources);

        String discoveryClasspath = String.join(System.getProperty(PATH_SEPARATOR), computedDiscovery);
        String classpath = String.join(System.getProperty(PATH_SEPARATOR), buildClasspath);
        return new Result(this, discoveryClasspath, classpath, computedSources);
    }

    /**
     * Result class used to build source and projection JARs.
     */
    public static final class Result {
        /**
         * Smithy Build command line arguments.
         *
         * <p>This value can be explicitly mutated as needed.
         */
        public final List<String> args;

        /**
         * The set of source models, including computed sources used in the argument list.
         *
         * <p>This value can be explicitly mutated as needed.
         */
        public final Set<String> sources;

        /**
         * Smithy build classpath string.
         *
         * <p>This is the classpath that should be used when invoking the CLI.
         * The value is a colon (:) separate string.
         */
        public final String classpath;

        /**
         * Smithy build discovery classpath string.
         *
         * <p>This is the classpath that is also specified in the arguments list and
         * is used for model discovery. The value is a colon (:) separate string.
         */
        public final String discoveryClasspath;

        private Result(
                BuildParameterBuilder builder,
                String discoveryClasspath,
                String classpath,
                Set<String> sources
        ) {
            this.classpath = classpath;
            this.sources = new LinkedHashSet<>(sources);

            args = new ArrayList<>();
            args.add("build");
            args.addAll(builder.extraArgs);

            if (!builder.discover) {
                this.discoveryClasspath = "";
            } else {
                this.discoveryClasspath = discoveryClasspath;
                if (!discoveryClasspath.isEmpty()) {
                    args.add("--discover-classpath");
                    args.add(discoveryClasspath);
                } else {
                    args.add("--discover");
                }
            }

            if (builder.allowUnknownTraits) {
                args.add("--allow-unknown-traits");
            }

            builder.configs.forEach(config -> {
                args.add("--config");
                args.add(config);
            });

            if (builder.output != null) {
                args.add("--output");
                args.add(builder.output);
            }

            if (builder.projection != null) {
                args.add("--projection");
                args.add(builder.projection);
            }

            if (builder.plugin != null) {
                args.add("--plugin");
                args.add(builder.plugin);
            }

            args.addAll(sources);
        }
    }

    /**
     * Interface used to query a classpath for the given projection sources tags.
     */
    public interface ClassPathTagMatcher {
        /**
         * Finds all JARs that contain any of the given set of tags.
         *
         * @param classpath Set of paths to JARs to search.
         * @param tagsToFind Tags to search for in the classpath.
         * @return Returns the matching JARs in the classpath.
         */
        Set<String> findJarsWithMatchingTags(Set<String> classpath, Set<String> tagsToFind);
    }

    /**
     * Finds JARs by opening each JAR in the classpath and looking for
     * "Smithy-Tags" in the META-INF/MANIFEST.MF file.
     */
    public static final class JarFileClassPathTagMatcher implements ClassPathTagMatcher {
        @Override
        public Set<String> findJarsWithMatchingTags(Set<String> classpath, Set<String> tagsToFind) {
            Set<String> tagSourceJars = new LinkedHashSet<>();

            for (String jar : classpath) {
                if (!Files.exists(Paths.get(jar))) {
                    LOGGER.severe("Classpath entry not found: " + jar);
                    continue;
                }

                try (JarFile jarFile = new JarFile(jar)) {
                    Manifest manifest = jarFile.getManifest();

                    Attributes.Name name = new Attributes.Name(SMITHY_TAG_PROPERTY);
                    if (manifest == null || !manifest.getMainAttributes().containsKey(name)) {
                        continue;
                    }

                    Set<String> jarTags = loadTags((String) manifest.getMainAttributes().get(name));
                    LOGGER.info("Found Smithy-Tags in JAR dependency `" + jar + "`: " + jarTags);

                    for (String needle : tagsToFind) {
                        if (jarTags.contains(needle)) {
                            tagSourceJars.add(jar);
                            break;
                        }
                    }

                } catch (IOException e) {
                    throw new SmithyBuildException(
                            "Error reading manifest from JAR in build dependencies: " + e.getMessage(),
                            e);
                }
            }

            return tagSourceJars;
        }

        private Set<String> loadTags(String sourceTagString) {
            return splitAndFilterString(",", sourceTagString);
        }
    }
}
