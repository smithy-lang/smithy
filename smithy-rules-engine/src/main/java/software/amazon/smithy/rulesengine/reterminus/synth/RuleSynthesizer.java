/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.reterminus.synth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.reterminus.Endpoint;
import software.amazon.smithy.rulesengine.reterminus.EndpointRuleset;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Literal;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.BooleanEquals;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.PartitionFn;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.StringEquals;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.Builtins;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.Parameter;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.Parameters;
import software.amazon.smithy.rulesengine.reterminus.lang.rule.Condition;
import software.amazon.smithy.rulesengine.reterminus.lang.rule.Rule;
import software.amazon.smithy.rulesengine.reterminus.lang.rule.TreeRule;
import software.amazon.smithy.rulesengine.v1.PartitionV1;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public class RuleSynthesizer {
    public static final Identifier SIGNING_NAME = Identifier.of("signingName");
    public static final Identifier SIGNING_SCOPE = Identifier.of("signingScope");
    public static final String NO_DUALSTACK_SUPPORT =
            "DualStack is enabled but this partition does not support DualStack";
    public static final String NO_FIPS =
            "FIPS is enabled but this partition does not support FIPS";
    public static final String NO_FIPS_OR_DUALSTACK =
            "FIPS and DualStack are enabled, but this partition does not support one or both";
    private static final EndpointModel DEFAULT_ENDPOINT_MODEL;
    private static final Expr PARTITION_SUPPORTS_FIPS =
            Expr.parseShortform("PartitionResult#supportsFIPS", SourceLocation.none());
    private static final Expr PARTITION_SUPPORTS_DUAL_STACK =
            Expr.parseShortform("PartitionResult#supportsDualStack", SourceLocation.none());

    static {
        DEFAULT_ENDPOINT_MODEL = EndpointModel.builder()
                .hostname("{EndpointPrefix}.{Region}.{PartitionResult#dnsSuffix}")
                .authSchemes(SetUtils.of("v4"))
                .fips(VariantModel.builder()
                        .hostname("{EndpointPrefix}-fips.{Region}.{PartitionResult#dnsSuffix}")
                        .build())
                .dualStack(VariantModel.builder()
                        .hostname("{EndpointPrefix}.{Region}.{PartitionResult#dualStackDnsSuffix}")
                        .build())
                .fipsDualStack(VariantModel.builder()
                        .hostname("{EndpointPrefix}-fips.{Region}.{PartitionResult#dualStackDnsSuffix}")
                        .build())
                .build();
    }

    private final String serviceId;
    private final String endpointPrefix;
    private final String signingName;
    private final EndpointModel defaultEndpointModel;
    private final List<PartitionV1> partitions;
    private final List<IntermediateModel> intermediateModels;

    public RuleSynthesizer(ServiceMetadata service, List<PartitionV1> partitions) {
        this.serviceId = service.serviceId();
        this.endpointPrefix = service.endpointPrefix();
        this.signingName = service.signingName().orElse(this.endpointPrefix);

        defaultEndpointModel = DEFAULT_ENDPOINT_MODEL.toBuilder()
                .endpointPrefix(serviceId)
                .build();

        this.partitions = partitions;

        intermediateModels = this.partitions.stream()
                .filter(p -> !p.partition().startsWith("aws-iso"))
                .map(partition -> new Preprocessor(partition, endpointPrefix))
                .map(Preprocessor::preprocess)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

    }

    private static Parameters params(Parameter... parameters) {
        Parameters.Builder builder = Parameters.builder();
        for (Parameter p : parameters) {
            builder.addParameter(p);
        }
        return builder.build();
    }

    private static Rule.Builder baseFipsRule() {
        Condition condition = Builtins.FIPS.eq(true).condition();

        return Rule.builder().condition(condition)
                .validateOrElse(NO_FIPS,
                        ensurePartitionSupportsFips());
    }

    private static Condition ensurePartitionSupportsFips() {
        return BooleanEquals.ofExprs(Expr.of(true), PARTITION_SUPPORTS_FIPS).condition();
    }

    private static Rule.Builder baseFipsDualStackRule() {
        List<Condition> conditions = Arrays.asList(
                Builtins.FIPS.eq(true).condition(),
                Builtins.DUALSTACK.eq(true).condition()
        );

        return Rule.builder().conditions(conditions)
                .validateOrElse(NO_FIPS_OR_DUALSTACK,
                        ensurePartitionSupportsFipsDualStack().toArray(new Condition[0]));
    }

    private static List<Condition> ensurePartitionSupportsFipsDualStack() {
        return Arrays.asList(
                BooleanEquals.ofExprs(Expr.of(true), PARTITION_SUPPORTS_FIPS).condition(),
                BooleanEquals.ofExprs(Expr.of(true), PARTITION_SUPPORTS_DUAL_STACK).condition());
    }

    private static Rule.Builder baseDualStackRule() {
        Condition condition = Builtins.DUALSTACK.eq(true).condition();

        return Rule.builder()
                .condition(condition)
                .validateOrElse(NO_DUALSTACK_SUPPORT,
                        ensurePartitionSupportsDualStack());
    }

    private static Condition ensurePartitionSupportsDualStack() {
        return BooleanEquals.ofExprs(Expr.of(true), PARTITION_SUPPORTS_DUAL_STACK).condition();
    }

    private static EndpointView normalizeViewHostname(EndpointView view) {
        return view.toBuilder()
                .hostname(view.hostname().stream()
                        .map(RuleSynthesizer::mapTemplateStringPart)
                        .collect(Collectors.toList())
                )
                .build();
    }

    private static EndpointView standardFipsEndpointView() {
        return DEFAULT_ENDPOINT_MODEL.fipsView().get();
    }

    private static String mapTemplateStringPart(String p) {
        if (p.equals("{dnsSuffix}")) {
            return "{PartitionResult#dnsSuffix}";
        } else if (p.equals("{region}")) {
            return "{Region}";
        } else if (p.equals("{service}")) {
            return "{EndpointPrefix}";
        }
        return p;
    }

    public EndpointRuleset serviceRuleSet() {
        Rule rootRule = TreeRule.builder()
                .condition(PartitionFn.fromParam(Builtins.REGION).condition("PartitionResult"))
                .treeRule(Arrays.asList(
                        fipsDualStackEndpointRule(),
                        fipsEndpointRule(),
                        dualStackEndpointRule(),
                        defaultEndpointRule()
                ));

        return EndpointRuleset.builder()
                .version("1.1")
                .parameters(
                        params(
                                Builtins.REGION.toBuilder().required(true).build(),
                                Builtins.DUALSTACK,
                                Builtins.FIPS
                        )
                )
                .addRule(rootRule)
                .build();
    }

    private Rule fipsEndpointRule() {
        List<IntermediateModel> hasUniqueFips = intermediateModels.stream()
                .filter(im -> !im.uniqueFipsRegions().isEmpty()
                              || (im.defaultEndpointModel().fipsView().isPresent()
                                  && im.defaultEndpointModel().fipsView().get()
                                          .differsFrom(standardFipsEndpointView())))
                .collect(Collectors.toList());

        if (hasUniqueFips.isEmpty()) {
            return baseFipsRule().endpoint(defaultFipsEndpoint());
        }

        List<Rule> rules = new ArrayList<>();
        hasUniqueFips.stream().flatMap(im -> fipsEndpointRules(im).stream()).forEach(rules::add);
        rules.add(Rule.builder().endpoint(defaultFipsEndpoint()));

        return baseFipsRule().treeRule(rules);
    }

    private List<Rule> fipsEndpointRules(IntermediateModel im) {
        Set<String> uniqueRegions = im.uniqueFipsRegions();

        List<Rule> rules = new ArrayList<>();
        for (String r : uniqueRegions) {
            EndpointView regionFipsView = im.regionEndpointModel(r).get().fipsView().get();
            Rule rule = Rule.builder()
                    .condition(Builtins.REGION.expr().eq(r).condition())
                    .endpoint(viewAsEndpoint(regionFipsView));
            rules.add(rule);
        }

        EndpointView serviceDefaultFips = normalizeViewHostname(im.defaultEndpointModel().fipsView().get());

        if (serviceDefaultFips.differsFrom(defaultEndpointModel.fipsView().get())) {
            Rule partitionRule = Rule.builder()
                    .condition(StringEquals.ofExprs(Literal.of(im.partitionId()),
                            Expr.parseShortform("PartitionResult#name", SourceLocation.none())))
                    .endpoint(viewAsEndpoint(serviceDefaultFips));

            rules.add(partitionRule);
        }

        return rules;
    }

    private Rule defaultEndpointRule() {
        List<IntermediateModel> hasUniqueDefault = intermediateModels.stream()
                .filter(im -> !im.uniqueDefaultRegions().isEmpty())
                .collect(Collectors.toList());

        if (hasUniqueDefault.isEmpty()) {
            return Rule.builder().endpoint(defaultEndpoint());
        }

        List<Rule> rules = new ArrayList<>();
        hasUniqueDefault.stream().flatMap(im -> defaultEndpointRules(im).stream()).forEach(rules::add);
        rules.add(Rule.builder().endpoint(defaultEndpoint()));

        return Rule.builder().treeRule(rules);
    }

    private List<Rule> defaultEndpointRules(IntermediateModel im) {
        Set<String> uniqueRegions = im.uniqueDefaultRegions();

        List<Rule> rules = new ArrayList<>();
        for (String r : uniqueRegions) {
            EndpointView regionFipsView = im.regionEndpointModel(r).get().defaultView();
            Rule rule = Rule.builder()
                    .condition(Builtins.REGION.expr().eq(r).condition())
                    .endpoint(viewAsEndpoint(regionFipsView));
            rules.add(rule);
        }

        EndpointView serviceDefault = normalizeViewHostname(im.defaultEndpointModel().defaultView());

        if (serviceDefault.differsFrom(defaultEndpointModel.defaultView())) {
            Rule partitionRule = Rule.builder()
                    .condition(StringEquals.ofExprs(Literal.of(im.partitionId()),
                            Expr.parseShortform("PartitionResult#name", SourceLocation.none())))
                    .endpoint(viewAsEndpoint(serviceDefault));

            rules.add(partitionRule);
        }

        return rules;
    }

    private Endpoint viewAsEndpoint(EndpointView view) {
        String hostname = view.hostname().stream()
                .map(p -> {
                    if (p.equals("{EndpointPrefix}")) {
                        return this.endpointPrefix;
                    }
                    return p;
                })
                .collect(Collectors.joining());

        Map<Identifier, Map<Identifier, Literal>> authParams = new HashMap<>();

        view.authSchemes().forEach(e -> {
            Map<Identifier, Literal> exprValues = new HashMap<>();
            authParams.put(Identifier.of(e), authParamsForScheme(e));
        });

        List<Identifier> authSchemes = view.authSchemes().stream()
                .map(Identifier::of)
                .collect(Collectors.toList());

        return Endpoint.builder()
                .url(Expr.of("https://" + hostname))
                .authSchemes(authSchemes, authParams)
                .build();
    }

    private Rule fipsDualStackEndpointRule() {
        return baseFipsDualStackRule().endpoint(defaultFipsDualStackEndpoint());
    }

    private Endpoint defaultFipsDualStackEndpoint() {
        return viewAsEndpoint(defaultEndpointModel.fipsDualStackView().get());
    }

    private Endpoint defaultDualStackEndpoint() {
        return viewAsEndpoint(defaultEndpointModel.dualStackView().get());
    }

    private Endpoint defaultEndpoint() {
        return viewAsEndpoint(defaultEndpointModel.defaultView());
    }

    private Endpoint defaultFipsEndpoint() {
        return viewAsEndpoint(defaultEndpointModel.fipsView().get());
    }

    private Rule dualStackEndpointRule() {
        return baseDualStackRule().endpoint(defaultDualStackEndpoint());
    }

    private Map<Identifier, Literal> authParamsForScheme(String scheme) {
        // TODO: don't actually know if "s3" == "v2" for the purposes of auth params required
        switch (scheme) {
            case "v2":
            case "s3":
                return MapUtils.of(SIGNING_NAME, Literal.of(signingName));
            case "s3v4":
            case "v4":
                return MapUtils.of(
                        SIGNING_NAME, Literal.of(signingName),
                        SIGNING_SCOPE, Literal.of("{" + Builtins.REGION.getName().asString() + "}")
                );
            default:
                throw new RuntimeException("Unknown auth scheme: " + scheme);
        }
    }
}
