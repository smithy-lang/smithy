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

package software.amazon.smithy.rulesengine.language.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.visit.DefaultVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Utility to determine with path of conditions that must be matched to reach a condition in a rule-set.
 * <p>
 * See `PathFinder.findPath`
 */
@SmithyUnstableApi
public final class PathFinder {
    private PathFinder() {
    }

    /**
     * Determine the path of positive and negative conditions to reach a given condition.
     * <p>
     * This can be used for debugging as well as other forms of static reachability analysis.
     *
     * @param ruleset The endpoint rule-set to search.
     * @param target The target condition to reach.
     * @return Path
     */
    public static Optional<Path> findPath(EndpointRuleSet ruleset, Condition target) {
        return new PathFinderVisitor(target).search(ruleset);
    }

    public static final class Path {
        private final List<List<Condition>> negated;
        private final List<Condition> positive;

        public Path(List<List<Condition>> negated, List<Condition> positive) {
            this.negated = negated;
            this.positive = positive;
        }

        Path negated(List<Condition> negated) {
            ArrayList<List<Condition>> neg = new ArrayList<>(this.negated);
            neg.add(negated);
            return new Path(neg, positive);
        }

        Optional<Path> merge(Optional<Path> optionalPath) {
            return optionalPath.map(path ->
                    new Path(
                            Stream.concat(this.negated.stream(), path.negated.stream()).collect(Collectors.toList()),
                            Stream.concat(this.positive.stream(), path.positive.stream()).collect(Collectors.toList())
                    )
            );
        }

        Path merge(Path path) {
            return new Path(
                    Stream.concat(this.negated.stream(), path.negated.stream()).collect(Collectors.toList()),
                    Stream.concat(this.positive.stream(), path.positive.stream()).collect(Collectors.toList())
            );
        }

        public List<List<Condition>> negated() {
            return negated;
        }

        public List<Condition> positive() {
            return positive;
        }

        @Override
        public int hashCode() {
            return Objects.hash(negated, positive);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Path that = (Path) obj;
            return Objects.equals(this.negated, that.negated)
                   && Objects.equals(this.positive, that.positive);
        }

        @Override
        public String toString() {
            return "Path["
                   + "negated=" + negated + ", "
                   + "positive=" + positive + ']';
        }

    }

    private static class PathFinderVisitor extends DefaultVisitor<Optional<Path>> {
        private final Condition target;

        PathFinderVisitor(Condition target) {
            this.target = target;
        }

        public Optional<Path> search(EndpointRuleSet ruleset) {
            for (Rule rule : ruleset.getRules()) {
                Optional<Path> pathInRule = handleRule(rule);
                if (pathInRule.isPresent()) {
                    return pathInRule;
                }
            }
            return Optional.empty();
        }

        private Optional<Path> handleRule(Rule rule) {
            Optional<List<Condition>> thisRuleMatches = searchLeafList(rule.getConditions());
            if (thisRuleMatches.isPresent()) {
                return Optional.of(new Path(new ArrayList<>(), thisRuleMatches.get()));
            } else {
                Optional<Path> subruleContainsTarget = rule.accept(this);
                return new Path(new ArrayList<>(), rule.getConditions()).merge(subruleContainsTarget);
            }
        }

        /**
         * Search a list of conditions for a target condition.
         *
         * @param conditions input list of conditions
         * @return conditions encountered on the path to the target
         */
        private Optional<List<Condition>> searchLeafList(List<Condition> conditions) {
            List<Condition> out = new ArrayList<>();
            for (Condition condition : conditions) {
                out.add(condition);
                if (condition == target) {
                    return Optional.of(out);
                }
            }
            return Optional.empty();

        }

        @Override
        public Optional<Path> getDefault() {
            return Optional.empty();
        }

        @Override
        public Optional<Path> visitTreeRule(List<Rule> rules) {
            Path out = new Path(new ArrayList<>(), new ArrayList<>());
            for (Rule subrule : rules) {
                // this will be non-None if the subrule contains the condition
                Optional<Path> inner = handleRule(subrule);
                if (inner.isPresent()) {
                    out = out.merge(inner.get());
                    return Optional.of(out);
                } else {
                    // otherwise, the conditions in this rule are conditions we _didn't_ match
                    out = out.negated(subrule.getConditions());
                }
            }
            return Optional.empty();
        }

    }
}

