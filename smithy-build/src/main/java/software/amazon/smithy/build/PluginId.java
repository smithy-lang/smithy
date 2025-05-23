/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.util.Objects;

final class PluginId {
    private final String pluginName;
    private final String artifactName;

    PluginId(String pluginName, String artifactName) {
        this.pluginName = Objects.requireNonNull(pluginName);
        this.artifactName = artifactName;
    }

    static PluginId from(String identifier) {
        String pluginName = identifier;
        String artifactName = null;

        int separatorPosition = identifier.indexOf("::");
        if (separatorPosition > -1) {
            pluginName = identifier.substring(0, separatorPosition);
            artifactName = identifier.substring(separatorPosition + 2);
        }

        return new PluginId(pluginName, artifactName);
    }

    String getPluginName() {
        return pluginName;
    }

    String getArtifactName() {
        return hasArtifactName() ? artifactName : pluginName;
    }

    boolean hasArtifactName() {
        return artifactName != null;
    }

    @Override
    public String toString() {
        if (!hasArtifactName()) {
            return pluginName;
        } else {
            return pluginName + "::" + artifactName;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof PluginId)) {
            return false;
        } else {
            PluginId pluginId = (PluginId) o;
            return pluginName.equals(pluginId.pluginName) && Objects.equals(artifactName, pluginId.artifactName);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginName, artifactName);
    }
}
