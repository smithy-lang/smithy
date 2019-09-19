/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.codegen.core;

import java.util.Objects;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a dependency that is introduced by a {@link Symbol}.
 *
 * <p>{@link SymbolProvider} implementations sometimes need to refer to
 * {@link Symbol} values that require a dependency to be brought in when
 * generating code. A dependency can be associated with the Symbol to
 * specify the relationship of a Symbol to a dependency.
 *
 * <p>This dependency class was designed to be as generic as possible while
 * still allowing for some extension points through <em>typed properties</em>.
 * If a feature you need is missing (for example, specifying a GitHub
 * repository), use {@link TypedPropertiesBag.Builder#putProperty} to add a
 * property on the dependency that can be understood by you code generator.
 *
 * <p>It's up to code generators to make sense of the values provided in a
 * dependency and to aggregate them in a meaningful way. This class uses a
 * package + version combination to define the coordinates of a dependency.
 * Some dependency managers like Maven use a group + package + version
 * combination. In cases like this, it is recommended to specify the
 * {@code package} of the symbol as the group + package name (e.g.,
 * "software.amazon.smithy.model:0.9.3" becomes a package of
 * "software.amazon.smithy.model" and a version of "0.9.3").
 *
 * <p>The {@code dependencyType} of a dependency is application and
 * target-specific. When omitted, it defaults to an empty string (""). An
 * arbitrary string value can be provided and should refer to something that
 * makes sense for the target language. For illustrative purposed only:
 * a code generator that targets JavaScript and NPM could set the
 * {@code dependencyType} of a dependency to "devDependencies" to add the
 * dependency to the "devDependencies" property of a generated package.json.
 *
 * <p>{@code version} is also an opaque values that is target-specific and
 * can even be specific to a {@code dependencyType}. For example, PHP's
 * Composer provides a section named "suggest" that is a map of package names
 * to a description of the suggestion. A {@code SymbolDependency} that is
 * meant to define a "suggest" entry for a composer.json file could set the
 * {@code dependencyType} to "suggest", the {@code packageName} to the name
 * of the suggested package, and {@code version} to the description of the
 * suggestion.
 */
public final class SymbolDependency extends TypedPropertiesBag implements ToSmithyBuilder<SymbolDependency> {

    private final String dependencyType;
    private final String packageName;
    private final String version;

    private SymbolDependency(Builder builder) {
        super(builder.properties);
        this.dependencyType = SmithyBuilder.requiredState("dependencyType", builder.dependencyType);
        this.packageName = SmithyBuilder.requiredState("packageName", builder.packageName);
        this.version = SmithyBuilder.requiredState("version", builder.version);
    }

    /**
     * @return Returns a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the type of dependency (for example, "dev", "optional", etc).
     *
     * <p>This value defaults to an empty string if not explicitly set.
     *
     * @return Returns the dependency type.
     */
    public String getDependencyType() {
        return dependencyType;
    }

    /**
     * Gets the package name referenced by the dependency.
     *
     * @return Returns the package name.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets the version string of the dependency.
     *
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .dependencyType(dependencyType)
                .packageName(packageName)
                .version(version)
                .properties(getProperties());
    }

    @Override
    public String toString() {
        return "SymbolDependency{"
               + "dependencyType='" + dependencyType + '\''
               + ", packageName='" + packageName + '\''
               + ", version='" + version + '\''
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SymbolDependency)) {
            return false;
        }

        SymbolDependency that = (SymbolDependency) o;
        return dependencyType.equals(that.dependencyType)
               && packageName.equals(that.packageName)
               && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencyType, packageName, version);
    }

    /**
     * Builds a SymbolDependency.
     */
    public static final class Builder
            extends TypedPropertiesBag.Builder<Builder>
            implements SmithyBuilder<SymbolDependency> {

        private String dependencyType = "";
        private String packageName;
        private String version;

        private Builder() {}

        @Override
        public SymbolDependency build() {
            return new SymbolDependency(this);
        }

        /**
         * Sets the type of dependency (for example, "dev", "optional", etc).
         *
         * <p>Defaults to an empty string if not explicitly set.
         *
         * @param dependencyType Dependency type to set.
         * @return Returns the builder.
         */
        public Builder dependencyType(String dependencyType) {
            this.dependencyType = dependencyType;
            return this;
        }

        /**
         * Sets the package name of the dependency.
         *
         * @param packageName Package name to set.
         * @return Returns the builder.
         */
        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        /**
         * Sets the version string of the dependency.
         *
         * @param version Opaque version string to set.
         * @return Returns the builder.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }
    }
}
