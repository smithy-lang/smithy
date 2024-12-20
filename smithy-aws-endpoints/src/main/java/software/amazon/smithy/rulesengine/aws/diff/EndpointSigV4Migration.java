/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.diff;

import static software.amazon.smithy.rulesengine.language.EndpointRuleSet.EndpointPathCollector;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.diff.evaluators.AbstractDiffEvaluator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.aws.language.functions.EndpointAuthUtils;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Emit diff validation for SigV4 migration in the {@code @smithy.rules#endpointRuleSet} trait.
 *
 * Specifically, SigV4 ({@code aws.auth#sigv4}) to SigV4A ({@code aws.auth#sigv4a}) due to a subset of credentials
 * usable with SigV4 that are not usable with SigV4A.
 *
 * @see <a href="https://smithy.io/2.0/aws/aws-auth.html">AWS Authentication Traits</a>
 * @see <a href="https://smithy.io/2.0/additional-specs/rules-engine/specification.html#endpoint-authschemes-list-property">Endpoint {@code authSchemes} list property</a>
 */
@SmithyInternalApi
public final class EndpointSigV4Migration extends AbstractDiffEvaluator {
    private static final Identifier ID_NAME = Identifier.of("name");

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();

        Model oldModel = differences.getOldModel();
        ServiceIndex oldServiceIndex = ServiceIndex.of(oldModel);
        Model newModel = differences.getNewModel();
        ServiceIndex newServiceIndex = ServiceIndex.of(newModel);

        // Validate Service effective auth schemes
        List<ChangedShape<ServiceShape>> serviceChanges = differences
                .changedShapes(ServiceShape.class)
                .collect(Collectors.toList());
        for (ChangedShape<ServiceShape> change : serviceChanges) {
            ServiceShape oldServiceShape = change.getOldShape();
            ServiceShape newServiceShape = change.getNewShape();

            if (!oldServiceShape.hasTrait(EndpointRuleSetTrait.ID)
                    || !newServiceShape.hasTrait(EndpointRuleSetTrait.ID)) {
                continue;
            }

            Optional<Pair<EndpointRuleSetTrait, EndpointRuleSetTrait>> endpointRuleSetOpt =
                    change.getChangedTrait(EndpointRuleSetTrait.class);
            Optional<Pair<AuthTrait, AuthTrait>> authOpt =
                    change.getChangedTrait(AuthTrait.class);
            List<String> oldModeledAuthSchemes = getModeledAuthSchemes(oldServiceIndex, oldServiceShape);
            List<String> newModeledAuthSchemes = getModeledAuthSchemes(newServiceIndex, newServiceShape);
            // Validate diffs for changes to `@smithy.rules#endpointRuleSet` and `@auth` and effective auth schemes
            if (!endpointRuleSetOpt.isPresent()
                    && !authOpt.isPresent()
                    // Check modeled auth schemes since they could change without the `@auth` trait present
                    && oldModeledAuthSchemes.equals(newModeledAuthSchemes)) {
                continue;
            }

            EndpointRuleSetTrait oldErs = oldServiceShape.expectTrait(EndpointRuleSetTrait.class);
            Map<String, Endpoint> oldEndpoints = EndpointPathCollector.from(oldErs).collect();

            EndpointRuleSetTrait newErs = newServiceShape.expectTrait(EndpointRuleSetTrait.class);
            Map<String, Endpoint> newEndpoints = EndpointPathCollector.from(newErs).collect();

            // JSON path -> Endpoint entries that exist in both the old and new model and are changed
            Map<String, Pair<Endpoint, Endpoint>> changedEndpoints = newEndpoints.entrySet()
                    .stream()
                    .filter(e -> oldEndpoints.containsKey(e.getKey()))
                    .map(e -> new SimpleEntry<String, Pair<Endpoint, Endpoint>>(
                            e.getKey(),
                            Pair.of(oldEndpoints.get(e.getKey()), e.getValue())))
                    .filter(e -> !e.getValue().getLeft().equals(e.getValue().getRight()))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            for (Entry<String, Pair<Endpoint, Endpoint>> entry : changedEndpoints.entrySet()) {
                String jsonPath = entry.getKey();
                Endpoint oldEndpoint = entry.getValue().getLeft();
                Endpoint newEndpoint = entry.getValue().getRight();
                List<String> oldAuthSchemes = getAuthSchemes(oldEndpoint, oldModeledAuthSchemes);
                List<String> newAuthSchemes = getAuthSchemes(newEndpoint, newModeledAuthSchemes);

                boolean isOldSigV4Present = containsSigV4EquivalentAuthScheme(oldAuthSchemes);
                boolean isOldSigV4APresent = containsSigV4AEquivalentAuthScheme(oldAuthSchemes);
                boolean isNewSigV4Present = containsSigV4EquivalentAuthScheme(newAuthSchemes);
                boolean isNewSigV4APresent = containsSigV4AEquivalentAuthScheme(newAuthSchemes);

                boolean isSigV4Replaced = isOldSigV4Present && !isNewSigV4Present
                        && !isOldSigV4APresent
                        && isNewSigV4APresent;
                boolean isSigV4AReplaced = !isOldSigV4Present && isNewSigV4Present
                        && isOldSigV4APresent
                        && !isNewSigV4APresent;
                boolean noSigV4XRemoved = isOldSigV4Present && isNewSigV4Present
                        && isOldSigV4APresent
                        && isNewSigV4APresent;
                boolean isSigV4Added = !isOldSigV4Present && isNewSigV4Present
                        && isOldSigV4APresent
                        && isNewSigV4APresent;
                boolean isSigV4AAdded = isOldSigV4Present && isNewSigV4Present
                        && !isOldSigV4APresent
                        && isNewSigV4APresent;
                if (isSigV4Replaced) {
                    events.add(danger(
                            newServiceShape,
                            "The `aws.auth#sigv4` authentication scheme was replaced by the `aws.auth#sigv4a` "
                                    + "authentication scheme in the effective auth schemes for an endpoint in the "
                                    + "`@smithy.rules#endpointRuleSet` trait applied to `" + newServiceShape.getId()
                                    + "` at: `"
                                    + jsonPath + "`. "
                                    + "Replacing the `aws.auth#sigv4` authentication scheme with the `aws.auth#sigv4a` "
                                    + "authentication scheme directly is not backward compatible since not all credentials usable "
                                    + "by `aws.auth#sigv4` are compatible with `aws.auth#sigv4a`, and can break existing clients' "
                                    + "authentication."));
                } else if (isSigV4AReplaced) {
                    events.add(danger(
                            newServiceShape,
                            "The `aws.auth#sigv4a` authentication scheme was replaced by the `aws.auth#sigv4` "
                                    + "authentication scheme in the effective auth schemes for an endpoint in the "
                                    + "`@smithy.rules#endpointRuleSet` trait applied to `" + newServiceShape.getId()
                                    + "` at: `"
                                    + jsonPath + "`. "
                                    + "Replacing the `aws.auth#sigv4` authentication scheme with the `aws.auth#sigv4a` "
                                    + "authentication scheme directly may not be backward compatible if the signing scope was "
                                    + "narrowed (typically from `*`)."));
                } else if (noSigV4XRemoved) {
                    int oldSigV4Index = getIndexOfSigV4AuthScheme(oldAuthSchemes);
                    int oldSigV4aIndex = getIndexOfSigV4AAuthScheme(oldAuthSchemes);
                    int sigV4Index = getIndexOfSigV4AuthScheme(newAuthSchemes);
                    int sigV4aIndex = getIndexOfSigV4AAuthScheme(newAuthSchemes);
                    boolean isOldSigV4BeforeSigV4A = oldSigV4Index < oldSigV4aIndex;
                    boolean isSigV4BeforeSigV4A = sigV4Index < sigV4aIndex;
                    if (isOldSigV4BeforeSigV4A && !isSigV4BeforeSigV4A) {
                        events.add(danger(
                                newServiceShape,
                                "The `aws.auth#sigv4a` authentication scheme was moved before the `aws.auth#sigv4` "
                                        + "authentication scheme in the effective auth schemes for an endpoint in the "
                                        + "`@smithy.rules#endpointRuleSet` trait applied to `" + newServiceShape.getId()
                                        + "` at: `" + jsonPath + "`. "
                                        + "Moving the `aws.auth#sigv4a` authentication scheme before the `aws.auth#sigv4` "
                                        + "authentication scheme is not backward compatible since not all credentials usable by "
                                        + "`aws.auth#sigv4` are compatible with `aws.auth#sigv4a`, and can break existing "
                                        + "clients' authentication."));
                    }
                    if (!isOldSigV4BeforeSigV4A && isSigV4BeforeSigV4A) {
                        events.add(danger(
                                newServiceShape,
                                "The `aws.auth#sigv4` authentication scheme was moved before the `aws.auth#sigv4a` "
                                        + "authentication scheme in the effective auth schemes for an endpoint in the "
                                        + "`@smithy.rules#endpointRuleSet` trait applied to `" + newServiceShape.getId()
                                        + "` at: `" + jsonPath + "`. "
                                        + "Moving the `aws.auth#sigv4` authentication scheme before the `aws.auth#sigv4a` "
                                        + "authentication scheme may not be backward compatible if the signing scope was narrowed "
                                        + "(typically from `*`)."));
                    }
                } else if (isSigV4Added) {
                    int sigV4Index = getIndexOfSigV4AuthScheme(newAuthSchemes);
                    int sigV4aIndex = getIndexOfSigV4AAuthScheme(newAuthSchemes);
                    boolean isSigV4AddedBeforeSigV4A = sigV4Index < sigV4aIndex;
                    if (isSigV4AddedBeforeSigV4A) {
                        events.add(danger(
                                newServiceShape,
                                "The `aws.auth#sigv4` authentication scheme was added before the `aws.auth#sigv4a` "
                                        + "authentication scheme in the effective auth schemes for an endpoint in the "
                                        + "`@smithy.rules#endpointRuleSet` trait applied to `" + newServiceShape.getId()
                                        + "` at: `" + jsonPath + "`. "
                                        + "Adding the `aws.auth#sigv4` authentication scheme before an existing `aws.auth#sigv4a` "
                                        + "authentication scheme may not be backward compatible if the signing scope was narrowed "
                                        + "(typically from `*`)."));
                    }
                } else if (isSigV4AAdded) {
                    int sigV4Index = getIndexOfSigV4AuthScheme(newAuthSchemes);
                    int sigV4aIndex = getIndexOfSigV4AAuthScheme(newAuthSchemes);
                    boolean isSigV4AAddedBeforeSigV4 = sigV4aIndex < sigV4Index;
                    if (isSigV4AAddedBeforeSigV4) {
                        events.add(danger(
                                newServiceShape,
                                "The `aws.auth#sigv4a` authentication scheme was added before the `aws.auth#sigv4` "
                                        + "authentication scheme in the effective auth schemes for an endpoint in the "
                                        + "`@smithy.rules#endpointRuleSet` trait applied to `" + newServiceShape.getId()
                                        + "` at: `" + jsonPath + "`. "
                                        + "Adding the `aws.auth#sigv4a` authentication scheme before an existing `aws.auth#sigv4` "
                                        + "authentication scheme is not backward compatible since not all credentials usable by "
                                        + "`aws.auth#sigv4` are compatible with `aws.auth#sigv4a`, and can break existing clients' "
                                        + "authentication."));
                    }
                }
            }
        }

        return events;
    }

    private static List<String> getAuthSchemes(Endpoint endpoint, List<String> modeledAuthSchemes) {
        List<String> endpointAuthSchemes = endpoint.getEndpointAuthSchemes()
                .stream()
                .map(a -> a.get(ID_NAME).asStringLiteral().get().expectLiteral())
                .collect(Collectors.toList());
        return endpointAuthSchemes.size() == 0
                ? modeledAuthSchemes
                : endpointAuthSchemes;
    }

    private static List<String> getModeledAuthSchemes(ServiceIndex serviceIndex, ServiceShape serviceShape) {
        return serviceIndex.getEffectiveAuthSchemes(serviceShape)
                .keySet()
                .stream()
                .map(ShapeId::toString)
                .collect(Collectors.toList());
    }

    private static boolean containsSigV4EquivalentAuthScheme(List<String> authSchemes) {
        return getIndexOfSigV4AuthScheme(authSchemes) != -1;
    }

    private static boolean containsSigV4AEquivalentAuthScheme(List<String> authSchemes) {
        return getIndexOfSigV4AAuthScheme(authSchemes) != -1;
    }

    private static int getIndexOfSigV4AuthScheme(List<String> authSchemes) {
        return getIndexOfAuthScheme(authSchemes, EndpointAuthUtils::isSigV4EquivalentAuthScheme);
    }

    private static int getIndexOfSigV4AAuthScheme(List<String> authSchemes) {
        return getIndexOfAuthScheme(authSchemes, EndpointAuthUtils::isSigV4AEquivalentAuthScheme);
    }

    private static int getIndexOfAuthScheme(List<String> authSchemes, Predicate<String> identifier) {
        for (int i = 0; i < authSchemes.size(); i++) {
            if (identifier.test(authSchemes.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
