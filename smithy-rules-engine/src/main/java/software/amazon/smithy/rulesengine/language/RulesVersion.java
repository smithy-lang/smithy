/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Represents the rules engine version with major and minor components.
 */
@SmithyUnstableApi
public final class RulesVersion implements Comparable<RulesVersion> {

    private static final ConcurrentHashMap<String, RulesVersion> CACHE = new ConcurrentHashMap<>();

    public static final RulesVersion V1_0 = of("1.0");
    public static final RulesVersion V1_1 = of("1.1");

    private final int major;
    private final int minor;
    private final String stringValue;
    private final int hashCode;

    private RulesVersion(int major, int minor) {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("Version components must be non-negative");
        }

        this.major = major;
        this.minor = minor;
        this.stringValue = major + "." + minor;
        this.hashCode = Objects.hash(major, minor);
    }

    /**
     * Creates a RulesVersion from a string representation.
     *
     * @param version the version string (e.g., "1.0", "1.2")
     * @return the RulesVersion instance
     * @throws IllegalArgumentException if the version string is invalid
     */
    public static RulesVersion of(String version) {
        return CACHE.computeIfAbsent(version, RulesVersion::parse);
    }

    /**
     * Creates a RulesVersion from components.
     *
     * @param major the major version
     * @param minor the minor version
     * @return the RulesVersion instance
     */
    public static RulesVersion of(int major, int minor) {
        String key = major + "." + minor;
        return CACHE.computeIfAbsent(key, k -> new RulesVersion(major, minor));
    }

    private static RulesVersion parse(String version) {
        if (StringUtils.isEmpty(version)) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid version: `" + version + "`. Expected format: major.minor");
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return new RulesVersion(major, minor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version format: " + version, e);
        }
    }

    /**
     * Gets the major version component.
     *
     * @return the major version
     */
    public int getMajor() {
        return major;
    }

    /**
     * Gets the minor version component.
     *
     * @return the minor version
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Checks if this version is at least the specified version.
     *
     * @param other the version to compare against
     * @return true if this version >= other
     */
    public boolean isAtLeast(RulesVersion other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(RulesVersion other) {
        if (this == other) {
            return 0;
        }

        int result = Integer.compare(major, other.major);
        if (result != 0) {
            return result;
        } else {
            return Integer.compare(minor, other.minor);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof RulesVersion)) {
            return false;
        }
        RulesVersion other = (RulesVersion) obj;
        return major == other.major && minor == other.minor;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
