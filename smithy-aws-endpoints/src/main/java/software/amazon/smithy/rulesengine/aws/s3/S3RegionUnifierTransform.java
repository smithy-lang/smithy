/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.s3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.aws.language.functions.AwsPartition;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Ite;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.RecordLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.cfg.TreeMapper;

/**
 * Unifies region references across S3 endpoint rules.
 *
 * <p>This transform solves the problem where the same logical region appears in
 * syntactically different forms:
 * <ul>
 *   <li>{@code Region} - the input parameter</li>
 *   <li>{@code bucketArn#region} - region from a parsed bucket ARN</li>
 *   <li>Hardcoded values like {@code us-east-1} (for aws-global)</li>
 * </ul>
 *
 * <p>The transform injects computed variables that unify these:
 * <ul>
 *   <li>{@code _effective_std_region} = ite(Region == "aws-global", "us-east-1", Region)</li>
 *   <li>{@code _effective_arn_region} = ite(coalesce(UseArnRegion, true), bucketArn#region, Region)</li>
 * </ul>
 */
final class S3RegionUnifierTransform extends TreeMapper {

    // Computed variable names
    static final String VAR_EFFECTIVE_ARN_REGION = "_effective_arn_region";
    static final String VAR_EFFECTIVE_STD_REGION = "_effective_std_region";

    // Identifiers
    private static final Identifier ID_BUCKET_ARN = Identifier.of("bucketArn");
    private static final Identifier ID_USE_ARN_REGION = Identifier.of("UseArnRegion");
    private static final Identifier ID_REGION = Identifier.of("Region");
    private static final Identifier ID_SIGNING_REGION = Identifier.of("signingRegion");

    // Function names
    private static final String AWS_PARSE_ARN = "aws.parseArn";
    private static final String AWS_PARTITION = "aws.partition";
    private static final String IS_VALID_HOST_LABEL = "isValidHostLabel";

    // Scope tracking
    private boolean inBucketArnScope = false;
    private boolean signingRegionDefined = false;

    private int rewriteCount = 0;

    private S3RegionUnifierTransform() {}

    static S3RegionUnifierTransform create() {
        return new S3RegionUnifierTransform();
    }

    /**
     * Returns the number of region references that were unified.
     *
     * @return rewrite count.
     */
    int getRewriteCount() {
        return rewriteCount;
    }

    @Override
    public Rule treeRule(TreeRule tr) {
        // Save scope state before descending
        boolean savedArnScope = inBucketArnScope;
        boolean savedSigningScope = signingRegionDefined;

        Rule result = super.treeRule(tr);

        // Restore scope state when exiting branch
        inBucketArnScope = savedArnScope;
        signingRegionDefined = savedSigningScope;

        return result;
    }

    @Override
    public List<Condition> conditions(Rule rule, List<Condition> conditions) {
        List<Condition> result = new ArrayList<>(conditions.size() + 2);

        for (Condition cond : conditions) {
            Condition transformed = condition(rule, cond);
            if (transformed == null) {
                continue;
            }

            result.add(transformed);

            // Inject _signing_region after isSet(Region)
            if (!signingRegionDefined && isIsSetRegion(transformed)) {
                result.add(createSigningRegionIte());
                signingRegionDefined = true;
            } else if (isBucketArnBinding(transformed)) {
                // Inject _effective_region after bucketArn binding
                result.add(createEffectiveRegionIte());
                inBucketArnScope = true;
            }
        }

        return result;
    }

    @Override
    public Condition condition(Rule rule, Condition cond) {
        // In ARN scope, rewrite bucketArn#region in partition/validation calls
        if (inBucketArnScope) {
            Condition rewritten = rewriteBucketArnRegionInCondition(cond);
            if (rewritten != cond) {
                return rewritten;
            }
        }

        return super.condition(rule, cond);
    }

    @Override
    public Expression error(ErrorRule er, Expression e) {
        // Don't rewrite error messages
        return e;
    }

    @Override
    public Literal stringLiteral(StringLiteral str) {
        Template template = str.value();

        // Handle static URL strings with region patterns
        if (template.isStatic()) {
            String value = template.expectLiteral();
            String rewritten = rewriteUrlRegionPatterns(value);
            if (rewritten != null) {
                return Literal.stringLiteral(Template.fromString(rewritten));
            }
            return str;
        }

        // Handle dynamic templates: check for bucketArn#region
        return rewriteDynamicTemplate(str, template);
    }

    @Override
    public Literal recordLiteral(RecordLiteral record) {
        Map<Identifier, Literal> members = record.members();
        Map<Identifier, Literal> newMembers = new LinkedHashMap<>();
        boolean changed = false;

        for (Map.Entry<Identifier, Literal> entry : members.entrySet()) {
            Identifier key = entry.getKey();
            Literal value = entry.getValue();
            Literal rewritten;

            // Special handling for signingRegion property
            if (ID_SIGNING_REGION.equals(key) && signingRegionDefined) {
                rewritten = rewriteSigningRegionValue(value);
            } else {
                rewritten = (Literal) expression(value);
            }

            newMembers.put(key, rewritten);
            if (rewritten != value) {
                changed = true;
            }
        }

        return changed ? Literal.recordLiteral(newMembers) : record;
    }

    // ========== Condition detection helpers ==========

    // Matches: isSet(Region)
    private boolean isIsSetRegion(Condition cond) {
        if (cond.getResult().isPresent()) {
            return false;
        }

        LibraryFunction fn = cond.getFunction();
        if (!fn.getFunctionDefinition().equals(IsSet.getDefinition()) || fn.getArguments().isEmpty()) {
            return false;
        }

        Expression arg = fn.getArguments().get(0);
        return arg instanceof Reference && ID_REGION.equals(((Reference) arg).getName());
    }

    // Matches: bucketArn = aws.parseArn(...)
    private boolean isBucketArnBinding(Condition cond) {
        return cond.getResult().isPresent()
                && ID_BUCKET_ARN.equals(cond.getResult().get())
                && AWS_PARSE_ARN.equals(cond.getFunction().getName());
    }

    // Matches: bucketArn#region
    private boolean isBucketArnRegion(Expression expr) {
        if (!(expr instanceof GetAttr)) {
            return false;
        }
        GetAttr getAttr = (GetAttr) expr;
        List<Expression> args = getAttr.getArguments();
        if (args.isEmpty() || !(args.get(0) instanceof Reference)) {
            return false;
        }
        Reference ref = (Reference) args.get(0);
        if (!ID_BUCKET_ARN.equals(ref.getName())) {
            return false;
        }
        List<GetAttr.Part> path = getAttr.getPath();
        return path.size() == 1
                && path.get(0) instanceof GetAttr.Part.Key
                && "region".equals(((GetAttr.Part.Key) path.get(0)).key().toString());
    }

    // ========== ITE condition creation ==========

    // Creates: _effective_std_region = ite(Region == "aws-global", "us-east-1", Region)
    private Condition createSigningRegionIte() {
        Expression isGlobal = StringEquals.ofExpressions(Expression.getReference(ID_REGION), "aws-global");
        Ite ite = Ite.ofExpressions(isGlobal,
                Expression.of("us-east-1"),
                Expression.getReference(ID_REGION));
        return Condition.builder().fn(ite).result(VAR_EFFECTIVE_STD_REGION).build();
    }

    /**
     * Creates the effective region ITE for ARN scope.
     *
     * <p>This is only called after bucketArn is successfully bound, so we know
     * the bucket IS an ARN. The ITE selects between the ARN's region and the
     * input region based on UseArnRegion (defaulting to true).
     */
    private Condition createEffectiveRegionIte() {
        Expression useArnRegion = Coalesce.ofExpressions(
                Expression.getReference(ID_USE_ARN_REGION),
                Expression.of(true));
        Expression arnRegion = GetAttr.ofExpressions(Expression.getReference(ID_BUCKET_ARN), "region");
        Expression inputRegion = Expression.getReference(ID_REGION);

        Ite ite = Ite.ofExpressions(useArnRegion, arnRegion, inputRegion);
        return Condition.builder().fn(ite).result(VAR_EFFECTIVE_ARN_REGION).build();
    }

    // ========== Region rewriting ==========

    private Condition rewriteBucketArnRegionInCondition(Condition cond) {
        LibraryFunction fn = cond.getFunction();
        String fnName = fn.getName();

        if (!AWS_PARTITION.equals(fnName) && !IS_VALID_HOST_LABEL.equals(fnName)) {
            return cond;
        }
        if (fn.getArguments().isEmpty() || !isBucketArnRegion(fn.getArguments().get(0))) {
            return cond;
        }

        rewriteCount++;
        List<Expression> newArgs = new ArrayList<>(fn.getArguments());
        newArgs.set(0, Expression.getReference(Identifier.of(VAR_EFFECTIVE_ARN_REGION)));

        LibraryFunction newFn = fn.getFunctionDefinition()
                .createFunction(FunctionNode.ofExpressions(fnName, newArgs.toArray(new Expression[0])));

        return cond.toBuilder().fn(newFn).build();
    }

    /**
     * Rewrites static URL strings to unify region patterns.
     *
     * <p>Pattern order matters: more specific patterns must come first to avoid
     * partial matches.
     */
    private String rewriteUrlRegionPatterns(String url) {
        if (!signingRegionDefined) {
            return null;
        }

        String targetVar = inBucketArnScope ? VAR_EFFECTIVE_ARN_REGION : VAR_EFFECTIVE_STD_REGION;
        String result = url;
        boolean changed = false;

        // Order matters: replace more specific patterns first
        if (result.contains(".us-east-1.")) {
            result = result.replace(".us-east-1.", ".{" + targetVar + "}.");
            changed = true;
        }
        if (result.contains(".{Region}.")) {
            result = result.replace(".{Region}.", ".{" + targetVar + "}.");
            changed = true;
        }
        if (result.contains("{Region}")) {
            result = result.replace("{Region}", "{" + targetVar + "}");
            changed = true;
        }
        if (inBucketArnScope && result.contains("{bucketArn#region}")) {
            result = result.replace("{bucketArn#region}", "{" + VAR_EFFECTIVE_ARN_REGION + "}");
            changed = true;
        }

        if (changed) {
            rewriteCount++;
            return result;
        }
        return null;
    }

    /**
     * Rewrites dynamic template strings to unify region references.
     */
    private Literal rewriteDynamicTemplate(StringLiteral str, Template template) {
        List<Template.Part> parts = template.getParts();
        StringBuilder sb = new StringBuilder();
        boolean changed = false;
        String targetVar = inBucketArnScope ? VAR_EFFECTIVE_ARN_REGION : VAR_EFFECTIVE_STD_REGION;

        for (Template.Part part : parts) {
            if (part instanceof Template.Dynamic) {
                Expression expr = ((Template.Dynamic) part).toExpression();
                if (isBucketArnRegion(expr)) {
                    sb.append("{").append(VAR_EFFECTIVE_ARN_REGION).append("}");
                    rewriteCount++;
                    changed = true;
                    continue;
                }
                if (signingRegionDefined && expr instanceof Reference
                        && ID_REGION.equals(((Reference) expr).getName())) {
                    sb.append("{").append(targetVar).append("}");
                    rewriteCount++;
                    changed = true;
                    continue;
                }
                sb.append(part);
            } else if (part instanceof Template.Literal) {
                String literal = ((Template.Literal) part).getValue();
                if (signingRegionDefined && literal.contains(".us-east-1.")) {
                    literal = literal.replace(".us-east-1.", ".{" + targetVar + "}.");
                    rewriteCount++;
                    changed = true;
                }
                sb.append(literal);
            } else {
                sb.append(part);
            }
        }

        return changed ? Literal.stringLiteral(Template.fromString(sb.toString())) : str;
    }

    private Literal rewriteSigningRegionValue(Literal value) {
        if (!value.asStringLiteral().isPresent()) {
            return (Literal) expression(value);
        }

        Template template = value.asStringLiteral().get();
        String targetVar = inBucketArnScope ? VAR_EFFECTIVE_ARN_REGION : VAR_EFFECTIVE_STD_REGION;

        // Dynamic template: {Region} or {bucketArn#region}
        if (!template.isStatic()) {
            List<Template.Part> parts = template.getParts();
            if (parts.size() == 1 && parts.get(0) instanceof Template.Dynamic) {
                Expression expr = ((Template.Dynamic) parts.get(0)).toExpression();
                if (isBucketArnRegion(expr)
                        || (expr instanceof Reference && ID_REGION.equals(((Reference) expr).getName()))) {
                    rewriteCount++;
                    return Literal.stringLiteral(Template.fromString("{" + targetVar + "}"));
                }
            }
            return (Literal) expression(value);
        }

        // Static region value
        String staticValue = template.expectLiteral();
        if (isKnownRegion(staticValue)) {
            rewriteCount++;
            return Literal.stringLiteral(Template.fromString("{" + targetVar + "}"));
        }

        return value;
    }

    private boolean isKnownRegion(String value) {
        return value != null && ("aws-global".equals(value) || AwsPartition.findPartition(value) != null);
    }
}
