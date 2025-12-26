/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Ite;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Split;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Rewrites S3 endpoint rules to use canonical, position-independent expressions.
 *
 * <p>This is a BDD pre-processing transform that makes the rules tree larger but enables dramatically better
 * BDD compilation. It solves the "SSA Trap" problem where semantically identical operations appear as syntactically
 * different expressions, preventing the BDD compiler from recognizing sharing opportunities.
 *
 * <h2>Internal use only</h2>
 * <p>Ideally this transform is deleted one day, and the rules that source it adopt these techniques (hopefully we
 * don't look back on this comment and laugh in 5 years). If/when that happens, this class will be deleted, whether
 * it breaks a consumer that uses it or not.
 *
 * <h2>Trade-off: Larger Rules, Smaller BDD</h2>
 * <p>This transform would be counterproductive for rule tree interpretation, but is highly beneficial when a
 * BDD compiler processes the output. It adds ITE (if-then-else) conditions to compute URL segments and auth scheme
 * names, increasing rule tree size by ~30%. However, this enables the BDD compiler to deduplicate endpoints that
 * were previously considered distinct, as of writing, reducing BDD results and node counts both by ~43%.
 *
 * <p>The key insight is that the BDD deduplicates by endpoint identity (URL template + properties). By making
 * URL templates identical through variable substitution, endpoints that differed only in FIPS/DualStack/auth variants
 * collapse into a single BDD result.
 *
 * <h2>Transformations performed:</h2>
 *
 * <h3>AZ Extraction Canonicalization</h3>
 *
 * <p>The original rules extract the availability zone ID using position-dependent substring operations.
 * Different bucket name lengths result in different extraction positions, creating 10+ SSA variants that can't
 * be shared in the BDD.
 *
 * <p>Before: Position-dependent substring extraction
 * <pre>{@code
 * {
 *   "conditions": [
 *     {
 *       "fn": "substring",
 *       "argv": [{"ref": "Bucket"}, 6, 14, true],
 *       "assign": "s3expressAvailabilityZoneId"
 *     }
 *   ],
 *   "rules": [...]
 * }
 * // Another branch with different positions:
 * {
 *   "conditions": [
 *     {
 *       "fn": "substring",
 *       "argv": [{"ref": "Bucket"}, 6, 20, true],
 *       "assign": "s3expressAvailabilityZoneId"
 *     }
 *   ],
 *   "rules": [...]
 * }
 * }</pre>
 *
 * <p>After: Position-independent split-based extraction
 * <pre>{@code
 * {
 *   "conditions": [
 *     {
 *       "fn": "getAttr",
 *       "argv": [
 *         {"fn": "split", "argv": [{"ref": "Bucket"}, "--", 0]},
 *         "[1]"
 *       ],
 *       "assign": "s3expressAvailabilityZoneId"
 *     }
 *   ],
 *   "rules": [...]
 * }
 * }</pre>
 *
 * <p>All branches now use the identical expression {@code split(Bucket, "--")[1]}, enabling
 * the BDD compiler to share nodes across all S3Express bucket handling paths. Because the expression only interacts
 * with Bucket, a constant value, there's no SSA transform performed on these expressions.
 *
 * <h3>URL Canonicalization</h3>
 *
 * <p>S3Express endpoints (currently) have 4 URL variants based on UseFIPS and UseDualStack flags. This creates
 * duplicate endpoints that differ only in URL structure.
 *
 * <p>Before: Separate endpoints for each FIPS/DualStack combination
 * <pre>{@code
 * // Branch 1: FIPS + DualStack
 * {
 *   "conditions": [
 *     {"fn": "booleanEquals", "argv": [{"ref": "UseFIPS"}, true]},
 *     {"fn": "booleanEquals", "argv": [{"ref": "UseDualStack"}, true]}
 *   ],
 *   "endpoint": {
 *     "url": "https://{Bucket}.s3express-fips-{s3expressAvailabilityZoneId}.dualstack.{Region}.amazonaws.com"
 *   }
 * }
 * // Branch 2: FIPS only
 * {
 *   "conditions": [
 *     {"fn": "booleanEquals", "argv": [{"ref": "UseFIPS"}, true]}
 *   ],
 *   "endpoint": {
 *     "url": "https://{Bucket}.s3express-fips-{s3expressAvailabilityZoneId}.{Region}.amazonaws.com"
 *   }
 * }
 * // Branch 3: DualStack only
 * // Branch 4: Neither
 * }</pre>
 *
 * <p>After: Single endpoint with ITE-computed URL segments
 * <pre>{@code
 * {
 *   "conditions": [
 *     {"fn": "ite", "argv": [{"ref": "UseFIPS"}, "-fips", ""], "assign": "_s3e_fips"},
 *     {"fn": "ite", "argv": [{"ref": "UseDualStack"}, ".dualstack", ""], "assign": "_s3e_ds"}
 *   ],
 *   "endpoint": {
 *     "url": "https://{Bucket}.s3express{_s3e_fips}-{s3expressAvailabilityZoneId}{_s3e_ds}.{Region}.amazonaws.com"
 *   }
 * }
 * }</pre>
 *
 * <p>The ITE conditions compute values branchlessly. The BDD sifting optimization naturally places these rare
 * S3Express-specific conditions late in the decision tree.
 *
 * <h3>Auth Scheme Canonicalization</h3>
 *
 * <p>S3Express endpoints use different auth schemes based on DisableS3ExpressSessionAuth.
 * This creates duplicate endpoints differing only in auth scheme name.
 *
 * <p>Before: Separate auth scheme names
 * <pre>{@code
 * // When DisableS3ExpressSessionAuth is true:
 * "authSchemes": [{"name": "sigv4", "signingName": "s3express", ...}]
 *
 * // When DisableS3ExpressSessionAuth is false/unset:
 * "authSchemes": [{"name": "sigv4-s3express", "signingName": "s3express", ...}]
 * }</pre>
 *
 * <p>After: ITE-computed auth scheme name
 * <pre>{@code
 * {
 *   "conditions": [
 *     {
 *       "fn": "ite",
 *       "argv": [
 *         {"fn": "coalesce", "argv": [{"ref": "DisableS3ExpressSessionAuth"}, false]},
 *         "sigv4",
 *         "sigv4-s3express"
 *       ],
 *       "assign": "_s3e_auth"
 *     }
 *   ],
 *   "endpoint": {
 *     "properties": {
 *       "authSchemes": [{"name": "{_s3e_auth}", "signingName": "s3express", ...}]
 *     }
 *   }
 * }
 * }</pre>
 */
@SmithyInternalApi
public final class S3TreeRewriter {
    private static final Logger LOGGER = Logger.getLogger(S3TreeRewriter.class.getName());

    // Variable names for the computed suffixes
    private static final String VAR_FIPS = "_s3e_fips";
    private static final String VAR_DS = "_s3e_ds";
    private static final String VAR_AUTH = "_s3e_auth";

    // Suffix values used in the URI templates
    private static final String FIPS_SUFFIX = "-fips";
    private static final String DS_SUFFIX = ".dualstack";
    private static final String EMPTY_SUFFIX = "";

    // Auth scheme values used with s3-express
    private static final String AUTH_SIGV4 = "sigv4";
    private static final String AUTH_SIGV4_S3EXPRESS = "sigv4-s3express";

    // Property and parameter identifiers
    private static final Identifier ID_AUTH_SCHEMES = Identifier.of("authSchemes");
    private static final Identifier ID_NAME = Identifier.of("name");
    private static final Identifier ID_BACKEND = Identifier.of("backend");
    private static final Identifier ID_BUCKET = Identifier.of("Bucket");
    private static final Identifier ID_AZ_ID = Identifier.of("s3expressAvailabilityZoneId");
    private static final Identifier ID_USE_FIPS = Identifier.of("UseFIPS");
    private static final Identifier ID_USE_DUAL_STACK = Identifier.of("UseDualStack");
    private static final Identifier ID_DISABLE_S3EXPRESS_SESSION_AUTH = Identifier.of("DisableS3ExpressSessionAuth");

    // Auth scheme name literal shared across all rewritten endpoints
    private static final Literal AUTH_NAME_LITERAL = Literal.stringLiteral(Template.fromString("{" + VAR_AUTH + "}"));

    // Patterns to match S3Express bucket endpoint URLs (with AZ)
    // Format: https://{Bucket}.s3express[-fips]-{AZ}[.dualstack].{Region}.amazonaws.com
    // (negative lookahead (?!dualstack) prevents matching dualstack variants in non-DS patterns)
    private static final Pattern S3EXPRESS_FIPS_DS = Pattern.compile("(s3express)-fips-([^.]+)\\.dualstack\\.(.+)$");
    private static final Pattern S3EXPRESS_FIPS = Pattern.compile("(s3express)-fips-([^.]+)\\.(?!dualstack)(.+)$");
    private static final Pattern S3EXPRESS_DS = Pattern.compile("(s3express)-([^.]+)\\.dualstack\\.(.+)$");
    private static final Pattern S3EXPRESS_PLAIN = Pattern.compile("(s3express)-([^.]+)\\.(?!dualstack)(.+)$");

    // Patterns to match S3Express control plane URLs (no AZ)
    // Format: https://s3express-control[-fips][.dualstack].{Region}.amazonaws.com
    private static final Pattern S3EXPRESS_CONTROL_FIPS_DS = Pattern.compile(
            "(s3express-control)-fips\\.dualstack\\.(.+)$");
    private static final Pattern S3EXPRESS_CONTROL_FIPS = Pattern.compile(
            "(s3express-control)-fips\\.(?!dualstack)(.+)$");
    private static final Pattern S3EXPRESS_CONTROL_DS = Pattern.compile(
            "(s3express-control)\\.dualstack\\.(.+)$");
    private static final Pattern S3EXPRESS_CONTROL_PLAIN = Pattern.compile(
            "(s3express-control)\\.(?!dualstack)(.+)$");

    // Cached canonical expression for AZ extraction: split(Bucket, "--", 0)
    private static final Split BUCKET_SPLIT = Split.ofExpressions(
            Expression.getReference(ID_BUCKET),
            Expression.of("--"),
            Expression.of(0));

    private int rewrittenCount = 0;
    private int totalS3ExpressCount = 0;

    private S3TreeRewriter() {}

    /**
     * Transforms the given endpoint rule set using canonical expressions.
     *
     * @param ruleSet the rule set to transform
     * @return the transformed rule set
     */
    public static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        return new S3TreeRewriter().run(ruleSet);
    }

    private EndpointRuleSet run(EndpointRuleSet ruleSet) {
        List<Rule> transformedRules = new ArrayList<>();
        for (Rule rule : ruleSet.getRules()) {
            transformedRules.add(transformRule(rule));
        }

        LOGGER.info(() -> String.format(
                "S3 tree rewriter: %s/%s S3Express endpoints rewritten",
                rewrittenCount,
                totalS3ExpressCount));

        return EndpointRuleSet.builder()
                .sourceLocation(ruleSet.getSourceLocation())
                .parameters(ruleSet.getParameters())
                .rules(transformedRules)
                .version(ruleSet.getVersion())
                .build();
    }

    private Rule transformRule(Rule rule) {
        if (rule instanceof TreeRule) {
            TreeRule tr = (TreeRule) rule;
            // Transform conditions
            List<Condition> transformedConditions = transformConditions(tr.getConditions());
            List<Rule> transformedChildren = new ArrayList<>();
            for (Rule child : tr.getRules()) {
                transformedChildren.add(transformRule(child));
            }
            return Rule.builder().conditions(transformedConditions).treeRule(transformedChildren);
        } else if (rule instanceof EndpointRule) {
            return rewriteEndpoint((EndpointRule) rule);
        } else {
            // Error rules pass through unchanged
            return rule;
        }
    }

    private List<Condition> transformConditions(List<Condition> conditions) {
        List<Condition> result = new ArrayList<>(conditions.size());
        for (Condition cond : conditions) {
            result.add(transformCondition(cond));
        }
        return result;
    }

    /**
     * Transforms a single condition.
     *
     * <p>Handles:
     * <pre>
     * AZ extraction: substring(Bucket, N, M) -> split(Bucket, "--")[1]
     * </pre>
     *
     * <p>Note: Delimiter checks (s3expressAvailabilityZoneDelim) are not currently transformed because they're part
     * of a complex fallback structure, and changing them breaks control flow. Possibly something we can improve, or
     * wait until the upstream rules are optimized.
     */
    private Condition transformCondition(Condition cond) {
        // Is this a condition fishing for delimiters?
        if (cond.getResult().isPresent()
                && ID_AZ_ID.equals(cond.getResult().get())
                && cond.getFunction() instanceof Substring
                && isSubstringOnBucket((Substring) cond.getFunction())) {
            // Replace with split-based extraction: split(Bucket, "--")[1]
            GetAttr azExpr = GetAttr.ofExpressions(BUCKET_SPLIT, "[1]");
            return cond.toBuilder().fn(azExpr).build();
        }

        return cond;
    }

    private boolean isSubstringOnBucket(Substring substring) {
        List<Expression> args = substring.getArguments();
        if (args.isEmpty()) {
            return false;
        }

        Expression target = args.get(0);
        return target instanceof Reference && ID_BUCKET.equals(((Reference) target).getName());
    }

    // Creates ITE conditions for branchless S3Express variable computation.
    private List<Condition> createIteConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createIteAssignment(VAR_FIPS, Expression.getReference(ID_USE_FIPS), FIPS_SUFFIX, EMPTY_SUFFIX));
        conditions.add(createIteAssignment(
                VAR_DS,
                Expression.getReference(ID_USE_DUAL_STACK),
                DS_SUFFIX,
                EMPTY_SUFFIX));
        // Auth scheme: sigv4 when session auth disabled, sigv4-s3express otherwise
        Expression sessionAuthDisabled = Coalesce.ofExpressions(
                Expression.getReference(ID_DISABLE_S3EXPRESS_SESSION_AUTH),
                Expression.of(false));
        conditions.add(createIteAssignment(VAR_AUTH, sessionAuthDisabled, AUTH_SIGV4, AUTH_SIGV4_S3EXPRESS));
        return conditions;
    }

    // Creates an ITE-based assignment condition.
    private Condition createIteAssignment(String varName, Expression condition, String trueValue, String falseValue) {
        return Condition.builder()
                .fn(Ite.ofStrings(condition, trueValue, falseValue))
                .result(varName)
                .build();
    }

    // Rewrites an endpoint rule to use canonical S3Express URLs and auth schemes.
    private Rule rewriteEndpoint(EndpointRule rule) {
        Endpoint endpoint = rule.getEndpoint();
        Expression urlExpr = endpoint.getUrl();

        // Extract the raw URL string from the expression (IFF it's a static string, rarely is anything else).
        String urlStr = extractUrlString(urlExpr);
        if (urlStr == null) {
            return rule;
        }

        // Check if this is an S3Express endpoint by URL or backend property.
        // Note: while `contains("s3express")` is broad and could theoretically match path/query components,
        // the subsequent matchUrl() call validates the hostname pattern before any rewriting occurs.
        boolean isS3ExpressUrl = urlStr.contains("s3express");
        boolean isS3ExpressBackend = isS3ExpressBackend(endpoint);

        if (!isS3ExpressUrl && !isS3ExpressBackend) {
            return rule;
        }

        totalS3ExpressCount++;

        // For URL override endpoints (backend=S3Express but URL doesn't match s3express hostname),
        // just canonicalize the auth scheme - no URL rewriting needed
        if (isS3ExpressBackend && !isS3ExpressUrl) {
            // Canonicalize auth scheme to use {_s3e_auth}
            Map<Identifier, Literal> newProperties = canonicalizeAuthScheme(endpoint.getProperties());

            if (newProperties == endpoint.getProperties()) {
                // No changes needed
                return rule;
            }

            rewrittenCount++;

            Endpoint newEndpoint = Endpoint.builder()
                    .url(urlExpr)
                    .headers(endpoint.getHeaders())
                    .properties(newProperties)
                    .sourceLocation(endpoint.getSourceLocation())
                    .build();

            // Add auth ITE condition for URL override endpoints
            List<Condition> allConditions = new ArrayList<>(rule.getConditions());
            allConditions.add(createAuthIteCondition());

            return Rule.builder()
                    .conditions(allConditions)
                    .endpoint(newEndpoint);
        }

        // Standard S3Express URL - match and rewrite
        UrlMatchResult match = matchUrl(urlStr);
        if (match == null) {
            return rule;
        }

        rewrittenCount++;

        // Rewrite the URL to use the ITE-assigned variables
        String newUrl = match.rewriteUrl();

        // Canonicalize auth scheme for bucket endpoints (not control plane)
        // Control plane always uses sigv4, bucket endpoints vary based on DisableS3ExpressSessionAuth
        Map<Identifier, Literal> newProperties = endpoint.getProperties();
        if (match instanceof BucketUrlMatchResult) {
            newProperties = canonicalizeAuthScheme(endpoint.getProperties());
        }

        // Build the new endpoint with canonicalized URL and properties
        Endpoint newEndpoint = Endpoint.builder()
                .url(Expression.of(newUrl))
                .headers(endpoint.getHeaders())
                .properties(newProperties)
                .sourceLocation(endpoint.getSourceLocation())
                .build();

        // Add ITE conditions: original conditions first, then ITE conditions at the end.
        List<Condition> allConditions = new ArrayList<>(rule.getConditions());
        allConditions.addAll(createIteConditions());

        return Rule.builder()
                .conditions(allConditions)
                .endpoint(newEndpoint);
    }

    // Checks if the endpoint has `backend` property set to "S3Express".
    private boolean isS3ExpressBackend(Endpoint endpoint) {
        Literal backend = endpoint.getProperties().get(ID_BACKEND);
        if (backend == null) {
            return false;
        }

        return backend.asStringLiteral()
                .filter(Template::isStatic)
                .map(t -> "S3Express".equalsIgnoreCase(t.expectLiteral()))
                .orElse(false);
    }

    // Creates fresh auth ITE condition for URL override endpoints.
    private Condition createAuthIteCondition() {
        Expression isSessionAuthDisabled = Coalesce.ofExpressions(
                Expression.getReference(ID_DISABLE_S3EXPRESS_SESSION_AUTH),
                Expression.of(false));
        return createIteAssignment(VAR_AUTH, isSessionAuthDisabled, AUTH_SIGV4, AUTH_SIGV4_S3EXPRESS);
    }

    // Canonicalizes the authScheme name in endpoint properties to use the ITE variable.
    private Map<Identifier, Literal> canonicalizeAuthScheme(Map<Identifier, Literal> properties) {
        Literal authSchemes = properties.get(ID_AUTH_SCHEMES);
        if (authSchemes == null) {
            return properties;
        }

        List<Literal> schemes = authSchemes.asTupleLiteral().orElse(null);
        if (schemes == null || schemes.isEmpty()) {
            return properties;
        }

        // Rewrite each auth scheme's name field
        List<Literal> newSchemes = new ArrayList<>();
        for (Literal scheme : schemes) {
            Map<Identifier, Literal> record = scheme.asRecordLiteral().orElse(null);
            if (record == null) {
                // Auth is always a record, but maybe that changes in the future, so pass it through.
                newSchemes.add(scheme);
                continue;
            }

            Literal nameLiteral = record.get(ID_NAME);
            if (nameLiteral == null) {
                // "name" should always be set, but pass through if not.
                newSchemes.add(scheme);
                continue;
            }

            // Only transform string literals we recognize.
            String name = nameLiteral.asStringLiteral()
                    .filter(Template::isStatic)
                    .map(Template::expectLiteral)
                    .orElse(null);

            // Only rewrite if it's one of the S3Express auth schemes
            if (AUTH_SIGV4.equals(name) || AUTH_SIGV4_S3EXPRESS.equals(name)) {
                Map<Identifier, Literal> newRecord = new LinkedHashMap<>(record);
                newRecord.put(ID_NAME, AUTH_NAME_LITERAL);
                newSchemes.add(Literal.recordLiteral(newRecord));
            } else {
                newSchemes.add(scheme);
            }
        }

        Map<Identifier, Literal> newProperties = new LinkedHashMap<>(properties);
        newProperties.put(ID_AUTH_SCHEMES, Literal.tupleLiteral(newSchemes));
        return newProperties;
    }

    // Extracts the raw URL string from a URL expression.
    private String extractUrlString(Expression urlExpr) {
        return urlExpr.toNode().asStringNode().map(StringNode::getValue).orElse(null);
    }

    // Matches an S3Express URL and returns the pattern match info. Tries to match in most specific order.
    private UrlMatchResult matchUrl(String url) {
        Matcher m;

        // First try control plane patterns (no AZ) since these are more specific
        m = S3EXPRESS_CONTROL_FIPS_DS.matcher(url);
        if (m.find()) {
            return new ControlPlaneUrlMatchResult(url, m);
        }

        m = S3EXPRESS_CONTROL_FIPS.matcher(url);
        if (m.find()) {
            return new ControlPlaneUrlMatchResult(url, m);
        }

        m = S3EXPRESS_CONTROL_DS.matcher(url);
        if (m.find()) {
            return new ControlPlaneUrlMatchResult(url, m);
        }

        m = S3EXPRESS_CONTROL_PLAIN.matcher(url);
        if (m.find()) {
            return new ControlPlaneUrlMatchResult(url, m);
        }

        // Next, try bucket endpoint patterns (with AZ)
        m = S3EXPRESS_FIPS_DS.matcher(url);
        if (m.find()) {
            return new BucketUrlMatchResult(url, m);
        }

        m = S3EXPRESS_FIPS.matcher(url);
        if (m.find()) {
            return new BucketUrlMatchResult(url, m);
        }

        m = S3EXPRESS_DS.matcher(url);
        if (m.find()) {
            return new BucketUrlMatchResult(url, m);
        }

        m = S3EXPRESS_PLAIN.matcher(url);
        if (m.find()) {
            return new BucketUrlMatchResult(url, m);
        }

        return null;
    }

    /**
     * Result of matching an S3Express URL pattern.
     */
    private abstract static class UrlMatchResult {
        protected final String prefix;

        UrlMatchResult(String prefix) {
            this.prefix = prefix;
        }

        abstract String rewriteUrl();
    }

    /**
     * Match result for bucket endpoints (with AZ): {prefix}s3express{fips}-{AZ}{ds}.{region}
     */
    private static final class BucketUrlMatchResult extends UrlMatchResult {
        private final String s3express;
        private final String az;
        private final String regionSuffix;

        BucketUrlMatchResult(String url, Matcher m) {
            super(url.substring(0, m.start()));
            this.s3express = m.group(1);
            this.az = m.group(2);
            this.regionSuffix = m.group(3);
        }

        @Override
        String rewriteUrl() {
            return String.format("%s%s{%s}-%s{%s}.%s", prefix, s3express, VAR_FIPS, az, VAR_DS, regionSuffix);
        }
    }

    /**
     * Match result for control plane endpoints (no AZ): {prefix}s3express-control{fips}{ds}.{region}
     */
    private static final class ControlPlaneUrlMatchResult extends UrlMatchResult {
        private final String s3expressControl;
        private final String regionSuffix;

        ControlPlaneUrlMatchResult(String url, Matcher m) {
            super(url.substring(0, m.start()));
            this.s3expressControl = m.group(1);
            this.regionSuffix = m.group(2);
        }

        @Override
        String rewriteUrl() {
            return String.format("%s%s{%s}{%s}.%s", prefix, s3expressControl, VAR_FIPS, VAR_DS, regionSuffix);
        }
    }
}
