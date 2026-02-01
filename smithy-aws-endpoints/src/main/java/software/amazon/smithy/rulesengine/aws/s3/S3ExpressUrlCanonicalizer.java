/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.s3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Handles URL pattern matching and canonicalization for S3Express endpoints.
 *
 * <p>S3Express URLs have multiple variants based on FIPS and DualStack settings.
 * This class detects these variants and produces canonical URLs that use ITE
 * variable references instead of hardcoded segments.
 *
 * <h2>Supported patterns:</h2>
 * <ul>
 *   <li>Control plane: {@code s3express-control[-fips][.dualstack].{Region}...}</li>
 *   <li>Bucket operations: {@code s3express[-fips]-{AZ}[.dualstack].{Region}...}</li>
 * </ul>
 */
@SmithyInternalApi
final class S3ExpressUrlCanonicalizer {

    // ITE variable names injected by S3TreeRewriter
    private static final String VAR_FIPS = "_s3e_fips";
    private static final String VAR_DS = "_s3e_ds";

    // URL patterns ordered most specific first to avoid partial matches
    private static final UrlPattern[] PATTERNS = {
            // Control plane: s3express-control[-fips][.dualstack].{Region}
            new UrlPattern("(s3express-control)-fips\\.dualstack\\.(.+)$", false),
            new UrlPattern("(s3express-control)-fips\\.(?!dualstack)(.+)$", false),
            new UrlPattern("(s3express-control)\\.dualstack\\.(.+)$", false),
            new UrlPattern("(s3express-control)\\.(?!dualstack)(.+)$", false),
            // Bucket: s3express[-fips]-{AZ}[.dualstack].{Region}
            new UrlPattern("(s3express)-fips-([^.]+)\\.dualstack\\.(.+)$", true),
            new UrlPattern("(s3express)-fips-([^.]+)\\.(?!dualstack)(.+)$", true),
            new UrlPattern("(s3express)-([^.]+)\\.dualstack\\.(.+)$", true),
            new UrlPattern("(s3express)-([^.]+)\\.(?!dualstack)(.+)$", true),
    };

    private S3ExpressUrlCanonicalizer() {}

    /**
     * Checks if a URL is an S3Express URL that can be canonicalized.
     *
     * @param url The URL to check.
     * @return true if the URL contains S3Express patterns.
     */
    static boolean isS3ExpressUrl(String url) {
        return url != null && url.contains("s3express");
    }

    /**
     * Attempts to match and canonicalize an S3Express URL.
     *
     * @param url The URL to canonicalize.
     * @return A {@link CanonicalizedUrl} if the URL matched a pattern, or null if no match.
     */
    static CanonicalizedUrl canonicalize(String url) {
        if (url == null) {
            return null;
        }
        for (UrlPattern pattern : PATTERNS) {
            Matcher m = pattern.pattern.matcher(url);
            if (m.find()) {
                return new CanonicalizedUrl(url, m, pattern.isBucketPattern);
            }
        }
        return null;
    }

    /**
     * Holds a regex pattern and whether it matches bucket-level operations.
     */
    private static final class UrlPattern {
        final Pattern pattern;
        final boolean isBucketPattern;

        UrlPattern(String regex, boolean isBucketPattern) {
            this.pattern = Pattern.compile(regex);
            this.isBucketPattern = isBucketPattern;
        }
    }

    /**
     * Represents a successfully matched and canonicalized S3Express URL.
     */
    static final class CanonicalizedUrl {
        private final String prefix;
        private final String service;
        private final String az; // null for control plane patterns
        private final String regionSuffix;
        private final boolean isBucketPattern;

        private CanonicalizedUrl(String url, Matcher m, boolean isBucketPattern) {
            this.prefix = url.substring(0, m.start());
            this.isBucketPattern = isBucketPattern;
            if (isBucketPattern) {
                this.service = m.group(1);
                this.az = m.group(2);
                this.regionSuffix = m.group(3);
            } else {
                this.service = m.group(1);
                this.az = null;
                this.regionSuffix = m.group(2);
            }
        }

        /**
         * Returns whether this is a bucket-level URL pattern (vs control plane).
         *
         * @return true if bucket pattern.
         */
        public boolean isBucketPattern() {
            return isBucketPattern;
        }

        /**
         * Builds the canonicalized URL string using ITE variable references.
         *
         * <p>Example: {@code s3express-fips-use1-az1.dualstack.us-east-1...} →
         * {@code s3express{_s3e_fips}-use1-az1{_s3e_ds}.us-east-1...}
         *
         * @return The canonicalized URL string.
         */
        public String toCanonicalUrl() {
            if (!isBucketPattern) {
                return String.format("%s%s{%s}{%s}.%s",
                        prefix,
                        service,
                        VAR_FIPS,
                        VAR_DS,
                        regionSuffix);
            } else if (az == null) {
                throw new IllegalStateException("az must be non-null for bucket patterns");
            } else {
                return String.format("%s%s{%s}-%s{%s}.%s",
                        prefix,
                        service,
                        VAR_FIPS,
                        az,
                        VAR_DS,
                        regionSuffix);
            }
        }
    }
}
