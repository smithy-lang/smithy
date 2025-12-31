/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.s3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Ite;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.cfg.TreeMapper;

/**
 * Canonicalizes S3Express endpoint URLs and auth schemes.
 *
 * <p>S3Express endpoints have multiple URL variants based on FIPS and DualStack settings.
 * This transform rewrites them to use ITE-computed URL segments, enabling BDD sharing
 * across all variants.
 *
 * <p>Transformations:
 * <ul>
 *   <li>URL patterns: {@code s3express-fips-{az}.dualstack.{region}} →
 *       {@code s3express{_fips}-{az}{_ds}.{region}} with ITE-computed variables</li>
 *   <li>Auth schemes: {@code sigv4}/{@code sigv4-s3express} → {@code {_s3e_auth}}
 *       with ITE based on DisableS3ExpressSessionAuth</li>
 * </ul>
 */
final class S3ExpressEndpointTransform extends TreeMapper {

    // Computed variable names
    private static final String VAR_FIPS = "_s3e_fips";
    private static final String VAR_DS = "_s3e_ds";
    private static final String VAR_AUTH = "_s3e_auth";

    // Identifiers
    private static final Identifier ID_USE_FIPS = Identifier.of("UseFIPS");
    private static final Identifier ID_USE_DUAL_STACK = Identifier.of("UseDualStack");
    private static final Identifier ID_DISABLE_S3EXPRESS_SESSION_AUTH = Identifier.of("DisableS3ExpressSessionAuth");
    private static final Identifier ID_AUTH_SCHEMES = Identifier.of("authSchemes");
    private static final Identifier ID_NAME = Identifier.of("name");
    private static final Identifier ID_BACKEND = Identifier.of("backend");

    // Auth scheme values
    private static final String AUTH_SIGV4 = "sigv4";
    private static final String AUTH_SIGV4_S3EXPRESS = "sigv4-s3express";
    private static final Literal AUTH_NAME_TEMPLATE = Literal.stringLiteral(Template.fromString("{" + VAR_AUTH + "}"));

    // Metrics
    private int rewriteCount = 0;
    private int totalCount = 0;

    private S3ExpressEndpointTransform() {}

    static S3ExpressEndpointTransform create() {
        return new S3ExpressEndpointTransform();
    }

    int getRewriteCount() {
        return rewriteCount;
    }

    int getTotalCount() {
        return totalCount;
    }

    @Override
    public Rule endpointRule(EndpointRule er) {
        Endpoint endpoint = er.getEndpoint();
        String url = extractUrlString(endpoint.getUrl());
        if (url == null) {
            return er;
        }

        boolean isS3ExpressUrl = S3ExpressUrlCanonicalizer.isS3ExpressUrl(url);
        boolean isS3ExpressBackend = hasS3ExpressBackend(endpoint);

        if (!isS3ExpressUrl && !isS3ExpressBackend) {
            return er;
        }

        totalCount++;

        // Custom endpoint with S3Express backend: just canonicalize auth
        if (isS3ExpressBackend && !isS3ExpressUrl) {
            return rewriteS3ExpressAuth(er);
        }

        // Standard S3Express URL: rewrite URL pattern
        return rewriteS3ExpressUrl(er, url);
    }

    private Rule rewriteS3ExpressAuth(EndpointRule rule) {
        Endpoint endpoint = rule.getEndpoint();
        Map<Identifier, Literal> newProps = canonicalizeAuthScheme(endpoint.getProperties());

        if (newProps == endpoint.getProperties()) {
            return rule;
        }

        rewriteCount++;
        List<Condition> conditions = new ArrayList<>(rule.getConditions());
        conditions.add(createAuthIte());

        return Rule.builder()
                .conditions(conditions)
                .endpoint(Endpoint.builder()
                        .url(endpoint.getUrl())
                        .headers(endpoint.getHeaders())
                        .properties(newProps)
                        .build());
    }

    // Adds: _s3e_fips = ite(UseFIPS, "-fips", ""), _s3e_ds = ite(UseDualStack, ".dualstack", ""), _s3e_auth = ...
    // Then, rewrites URL to use {_s3e_fips} and {_s3e_ds}, and auth schemes to use {_s3e_auth}.
    private Rule rewriteS3ExpressUrl(EndpointRule rule, String url) {
        S3ExpressUrlCanonicalizer.CanonicalizedUrl canonicalized = S3ExpressUrlCanonicalizer.canonicalize(url);
        if (canonicalized == null) {
            return rule;
        }

        rewriteCount++;
        Endpoint endpoint = rule.getEndpoint();

        // Note: _s3e_auth could technically be omitted for control plane, but including it reduces sifted
        // BDD size, so just keeping it as-is for now.
        List<Condition> conditions = new ArrayList<>(rule.getConditions());
        conditions.add(createIte(VAR_FIPS, Expression.getReference(ID_USE_FIPS), "-fips", ""));
        conditions.add(createIte(VAR_DS, Expression.getReference(ID_USE_DUAL_STACK), ".dualstack", ""));
        conditions.add(createAuthIte());

        Map<Identifier, Literal> newProps = canonicalized.isBucketPattern()
                ? canonicalizeAuthScheme(endpoint.getProperties())
                : endpoint.getProperties();

        return Rule.builder()
                .conditions(conditions)
                .endpoint(Endpoint.builder()
                        .url(Expression.of(canonicalized.toCanonicalUrl()))
                        .headers(endpoint.getHeaders())
                        .properties(newProps)
                        .build());
    }

    // Creates: {varName} = ite(condition, trueVal, falseVal)
    private Condition createIte(String varName, Expression condition, String trueVal, String falseVal) {
        return Condition.builder()
                .fn(Ite.ofStrings(condition, trueVal, falseVal))
                .result(varName)
                .build();
    }

    // Creates: _s3e_auth = ite(coalesce(DisableS3ExpressSessionAuth, false), "sigv4", "sigv4-s3express")
    private Condition createAuthIte() {
        Expression disabled = Coalesce.ofExpressions(
                Expression.getReference(ID_DISABLE_S3EXPRESS_SESSION_AUTH),
                Expression.of(false));
        return createIte(VAR_AUTH, disabled, AUTH_SIGV4, AUTH_SIGV4_S3EXPRESS);
    }

    // Checks for: backend = "S3Express"
    private boolean hasS3ExpressBackend(Endpoint endpoint) {
        Literal backend = endpoint.getProperties().get(ID_BACKEND);
        if (backend == null) {
            return false;
        }
        return backend.asStringLiteral()
                .filter(Template::isStatic)
                .map(t -> "S3Express".equalsIgnoreCase(t.expectLiteral()))
                .orElse(false);
    }

    // Rewrites: authSchemes[].name: "sigv4"/"sigv4-s3express" → "{_s3e_auth}"
    private Map<Identifier, Literal> canonicalizeAuthScheme(Map<Identifier, Literal> properties) {
        Literal authSchemes = properties.get(ID_AUTH_SCHEMES);
        if (authSchemes == null || !authSchemes.asTupleLiteral().isPresent()) {
            return properties;
        }

        List<Literal> schemes = authSchemes.asTupleLiteral().get();
        List<Literal> newSchemes = new ArrayList<>(schemes.size());
        boolean changed = false;

        for (Literal scheme : schemes) {
            if (!scheme.asRecordLiteral().isPresent()) {
                newSchemes.add(scheme);
                continue;
            }

            Map<Identifier, Literal> record = scheme.asRecordLiteral().get();
            Literal nameLit = record.get(ID_NAME);
            String name = null;
            if (nameLit != null && nameLit.asStringLiteral().isPresent()) {
                Template template = nameLit.asStringLiteral().get();
                if (template.isStatic()) {
                    name = template.expectLiteral();
                }
            }

            if (AUTH_SIGV4.equals(name) || AUTH_SIGV4_S3EXPRESS.equals(name)) {
                Map<Identifier, Literal> newRecord = new LinkedHashMap<>(record);
                newRecord.put(ID_NAME, AUTH_NAME_TEMPLATE);
                newSchemes.add(Literal.recordLiteral(newRecord));
                changed = true;
            } else {
                newSchemes.add(scheme);
            }
        }

        if (!changed) {
            return properties;
        }

        Map<Identifier, Literal> newProps = new LinkedHashMap<>(properties);
        newProps.put(ID_AUTH_SCHEMES, Literal.tupleLiteral(newSchemes));
        return newProps;
    }

    private String extractUrlString(Expression urlExpr) {
        return urlExpr.toNode().asStringNode().map(StringNode::getValue).orElse(null);
    }
}
