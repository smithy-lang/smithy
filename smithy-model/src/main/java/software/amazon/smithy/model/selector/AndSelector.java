/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.selector;

import java.util.List;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Maps input over a list of functions, passing the result of each to
 * the next.
 *
 * <p>The list of selectors is short-circuited if any selector returns
 * an empty result (by virtue of a selector not forwarding a shape to
 * the next selector).
 */
final class AndSelector {

    private AndSelector() {}

    static InternalSelector of(List<InternalSelector> selectors) {
        switch (selectors.size()) {
            case 0:
                // This happens when selectors are optimized (i.e., the first internal
                // selector is a shape type and it gets applied in Model.shape() before
                // pushing shapes through the selector.
                return InternalSelector.IDENTITY;
            case 1:
                // If there's only a single selector, then no need to wrap.
                return selectors.get(0);
            case 2:
                // Cases 2-7 are optimizations that make selectors about
                // 40% faster based on JMH benchmarks (at least on my machine,
                // JDK 11.0.5, Java HotSpot(TM) 64-Bit Server VM, 11.0.5+10-LTS).
                // I stopped at 7 because, it needs to stop somewhere, and it's lucky.
                return (c, s, n) -> {
                    return selectors.get(0).push(c, s, (c2, s2) -> {
                        return selectors.get(1).push(c2, s2, n);
                    });
                };
            case 3:
                return (c, s, n) -> {
                    return selectors.get(0).push(c, s, (c2, s2) -> {
                        return selectors.get(1).push(c2, s2, (c3, s3) -> {
                            return selectors.get(2).push(c3, s3, n);
                        });
                    });
                };
            case 4:
                return (c, s, n) -> {
                    return selectors.get(0).push(c, s, (c2, s2) -> {
                        return selectors.get(1).push(c2, s2, (c3, s3) -> {
                            return selectors.get(2).push(c3, s3, (c4, s4) -> {
                                return selectors.get(3).push(c4, s4, n);
                            });
                        });
                    });
                };
            case 5:
                return (c, s, n) -> {
                    return selectors.get(0).push(c, s, (c2, s2) -> {
                        return selectors.get(1).push(c2, s2, (c3, s3) -> {
                            return selectors.get(2).push(c3, s3, (c4, s4) -> {
                                return selectors.get(3).push(c4, s4, (c5, s5) -> {
                                    return selectors.get(4).push(c5, s5, n);
                                });
                            });
                        });
                    });
                };
            case 6:
                return (c, s, n) -> {
                    return selectors.get(0).push(c, s, (c2, s2) -> {
                        return selectors.get(1).push(c2, s2, (c3, s3) -> {
                            return selectors.get(2).push(c3, s3, (c4, s4) -> {
                                return selectors.get(3).push(c4, s4, (c5, s5) -> {
                                    return selectors.get(4).push(c5, s5, (c6, s6) -> {
                                        return selectors.get(5).push(c6, s6, n);
                                    });
                                });
                            });
                        });
                    });
                };
            case 7:
                return (c, s, n) -> {
                    return selectors.get(0).push(c, s, (c2, s2) -> {
                        return selectors.get(1).push(c2, s2, (c3, s3) -> {
                            return selectors.get(2).push(c3, s3, (c4, s4) -> {
                                return selectors.get(3).push(c4, s4, (c5, s5) -> {
                                    return selectors.get(4).push(c5, s5, (c6, s6) -> {
                                        return selectors.get(5).push(c6, s6, (c7, s7) -> {
                                            return selectors.get(6).push(c7, s7, n);
                                        });
                                    });
                                });
                            });
                        });
                    });
                };
            default:
                return new RecursiveAndSelector(selectors);
        }
    }

    static final class RecursiveAndSelector implements InternalSelector {

        private final List<InternalSelector> selectors;
        private final int terminalSelectorIndex;

        private RecursiveAndSelector(List<InternalSelector> selectors) {
            this.selectors = selectors;
            this.terminalSelectorIndex = this.selectors.size() - 1;
        }

        @Override
        public boolean push(Context context, Shape shape, Receiver next) {
            // This is safe since the number of selectors is always >= 2.
            return selectors.get(0).push(context, shape, new State(1, next));
        }

        private final class State implements Receiver {

            private final int position;
            private final Receiver downstream;

            private State(int position, Receiver downstream) {
                this.position = position;
                this.downstream = downstream;
            }

            @Override
            public boolean apply(Context context, Shape shape) {
                if (position == terminalSelectorIndex) {
                    return selectors.get(position).push(context, shape, downstream);
                } else {
                    return selectors.get(position).push(context, shape, new State(position + 1, downstream));
                }
            }
        }
    }
}
