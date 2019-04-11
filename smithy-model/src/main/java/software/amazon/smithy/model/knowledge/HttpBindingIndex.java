/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Computes and indexes the explicit and implicit HTTP bindings of a model.
 *
 * <p>This index makes it easy to understand how members of the input or output
 * of a request/response are bound to an HTTP message by providing all of the
 * bindings in the model as a normalized {@link Binding} object. This can be
 * used to validate the bindings of an operation, generate code to [de]serialize
 * shapes, diff models to ensure backward compatibility at the wire level, etc.
 *
 * <p>This index does not perform validation of the underlying model.
 */
public final class HttpBindingIndex implements KnowledgeIndex {
    private final ShapeIndex index;
    private final Map<ShapeId, List<Binding>> requestBindings = new HashMap<>();
    private final Map<ShapeId, List<Binding>> responseBindings = new HashMap<>();

    public HttpBindingIndex(Model model) {
        index = model.getShapeIndex();
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);
        index.shapes(OperationShape.class).forEach(shape -> {
            if (shape.getTrait(HttpTrait.class).isPresent()) {
                requestBindings.put(shape.getId(), computeRequestBindings(opIndex, shape));
                responseBindings.put(shape.getId(), computeResponseBindings(opIndex, shape));
            } else {
                requestBindings.put(shape.getId(), ListUtils.of());
                responseBindings.put(shape.getId(), ListUtils.of());
            }
        });

        // Add error structure bindings.
        index.shapes(StructureShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, ErrorTrait.class))
                .forEach(pair -> responseBindings.put(
                        pair.getLeft().getId(),
                        createStructureBindings(pair.getLeft(), false)));
    }

    /**
     * Returns true if a shape has any HTTP request trait bindings.
     *
     * @param shape Shape to check.
     * @return Returns true if the shape is bound to an HTTP header,
     *  payload, prefix headers, query string, or label.
     */
    public static boolean hasHttpRequestBindings(Shape shape) {
        return shape.hasTrait(HttpHeaderTrait.class)
                || shape.hasTrait(HttpPrefixHeadersTrait.class)
                || shape.hasTrait(HttpPayloadTrait.class)
                || shape.hasTrait(HttpQueryTrait.class)
                || shape.hasTrait(HttpLabelTrait.class);
    }

    /**
     * Returns true if a shape has any HTTP response trait bindings.
     *
     * @param shape Shape to check.
     * @return Returns true if the shape is bound to an HTTP header,
     *  payload, of prefix headers.
     */
    public static boolean hasHttpResponseBindings(Shape shape) {
        return shape.hasTrait(HttpHeaderTrait.class)
               || shape.hasTrait(HttpPrefixHeadersTrait.class)
               || shape.hasTrait(HttpPayloadTrait.class);
    }

    private HttpTrait getHttpTrait(ShapeId operation) {
        return index.getShape(operation)
                .filter(Shape::isOperationShape)
                .flatMap(shape -> shape.getTrait(HttpTrait.class))
                .orElseThrow(() -> new IllegalArgumentException(operation + " is not an operation"));
    }

    /**
     * Gets the computed status code of an operation or error structure.
     *
     * @param shapeOrId Operation or error structure shape ID.
     * @return Returns the computed HTTP status code.
     * @throws IllegalArgumentException if the given shape is not an operation
     *  or an error structure.
     */
    public int getResponseCode(ToShapeId shapeOrId) {
        ShapeId id = shapeOrId.toShapeId();
        Shape shape = index.getShape(id).orElseThrow(() -> new IllegalArgumentException("Shape not found " + id));

        if (shape.isOperationShape()) {
            return getHttpTrait(id).getCode();
        } else if (shape.getTrait(HttpErrorTrait.class).isPresent()) {
            return shape.getTrait(HttpErrorTrait.class).get().getCode();
        } else if (shape.getTrait(ErrorTrait.class).isPresent()) {
            return shape.getTrait(ErrorTrait.class).get().getDefaultHttpStatusCode();
        }

        throw new IllegalStateException(shape + " must be an operation or error structure");
    }

    /**
     * Gets the request bindings of an operation as a map of member name to
     * the binding.
     *
     * @param operationShapeOrId Operation to get the request bindings for.
     * @return Map of unmodifiable bindings.
     * @throws IllegalArgumentException if the given shape is not an operation.
     */
    public Map<String, Binding> getRequestBindings(ToShapeId operationShapeOrId) {
        ShapeId id = operationShapeOrId.toShapeId();
        validateRequestBindingShapeId(id);
        return requestBindings.get(id).stream().collect(Collectors.toMap(Binding::getMemberName, Function.identity()));
    }

    private void validateRequestBindingShapeId(ShapeId id) {
        if (!requestBindings.containsKey(id)) {
            throw new IllegalArgumentException(id + " does not reference an operation with http bindings");
        }
    }

    /**
     * Gets the request bindings of an operation as a map of member name to
     * the binding for a specific location type.
     *
     * @param operationShapeOrId Operation to get the request bindings for.
     * @param requestLocation Location of the binding.
     * @return Map of unmodifiable bindings.
     * @throws IllegalArgumentException if the given shape is not an operation.
     */
    public List<Binding> getRequestBindings(ToShapeId operationShapeOrId, Location requestLocation) {
        ShapeId id = operationShapeOrId.toShapeId();
        validateRequestBindingShapeId(id);
        return requestBindings.get(id).stream()
                .filter(binding -> binding.getLocation() == requestLocation)
                .collect(Collectors.toList());
    }

    /**
     * Gets the computed HTTP message response bindings for an operation
     * or structure with an error trait.
     *
     * @param shapeOrId Operation or error structure shape or ID.
     * @return Map of unmodifiable bindings.
     * @throws IllegalArgumentException if the given shape is not an operation
     *  or error structure.
     */
    public Map<String, Binding> getResponseBindings(ToShapeId shapeOrId) {
        ShapeId id = shapeOrId.toShapeId();
        validateResponseBindingShapeId(id);
        return responseBindings.get(id).stream()
                .collect(Collectors.toMap(Binding::getMemberName, Function.identity()));
    }

    private void validateResponseBindingShapeId(ShapeId id) {
        if (!responseBindings.containsKey(id)) {
            throw new IllegalArgumentException(id + " does not reference an operation or error structure");
        }
    }

    /**
     * Gets the computed HTTP message response bindings for an operation
     * or structure with an error trait for a specific binding type.
     *
     * @param shapeOrId Operation or error structure shape or ID.
     * @param bindingLocation Binding location type.
     * @return List of found bindings.
     * @throws IllegalArgumentException if the given shape is not an operation
     *  or error structure.
     */
    public List<Binding> getResponseBindings(ToShapeId shapeOrId, Location bindingLocation) {
        ShapeId id = shapeOrId.toShapeId();
        validateResponseBindingShapeId(id);
        return responseBindings.get(id).stream()
                .filter(binding -> binding.getLocation() == bindingLocation)
                .collect(Collectors.toList());
    }

    /** HTTP binding types. */
    public enum Location { LABEL, DOCUMENT, PAYLOAD, HEADER, PREFIX_HEADERS, QUERY, UNBOUND }

    /**
     * Defines an HTTP message member binding.
     */
    public static final class Binding {

        private final MemberShape member;
        private final Location location;
        private final String locationName;
        private final Trait bindingTrait;

        Binding(MemberShape member, Location location, String locationName, Trait bindingTrait) {
            this.member = Objects.requireNonNull(member);
            this.location = Objects.requireNonNull(location);
            this.locationName = Objects.requireNonNull(locationName);
            this.bindingTrait = bindingTrait;
        }

        public MemberShape getMember() {
            return member;
        }

        public String getMemberName() {
            return member.getMemberName();
        }

        public Location getLocation() {
            return location;
        }

        public String getLocationName() {
            return locationName;
        }

        public Optional<Trait> getBindingTrait() {
            return Optional.ofNullable(bindingTrait);
        }

        @Override
        public String toString() {
            return member.getId() + " @ " + location.toString().toLowerCase(Locale.US) + " (" + locationName + ")";
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Binding)) {
                return false;
            } else {
                Binding otherBinding = (Binding) other;
                return getMember().equals(otherBinding.getMember())
                       && getLocation() == otherBinding.getLocation()
                       && getLocationName().equals(otherBinding.getLocationName());
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, member, locationName);
        }
    }

    private List<Binding> computeRequestBindings(OperationIndex opIndex, OperationShape shape) {
        return opIndex.getInput(shape.getId())
                .map(input -> createStructureBindings(input, true))
                .orElseGet(Collections::emptyList);
    }

    private List<Binding> computeResponseBindings(OperationIndex opIndex, OperationShape shape) {
        return opIndex.getOutput(shape.getId())
                .map(output -> createStructureBindings(output, false))
                .orElseGet(Collections::emptyList);
    }

    private List<Binding> createStructureBindings(StructureShape struct, boolean isRequest) {
        List<Binding> bindings = new ArrayList<>();
        List<MemberShape> unbound = new ArrayList<>();
        boolean foundPayload = false;

        for (MemberShape member : struct.getAllMembers().values()) {
            if (member.getTrait(HttpHeaderTrait.class).isPresent()) {
                HttpHeaderTrait trait = member.getTrait(HttpHeaderTrait.class).get();
                bindings.add(new Binding(member, Location.HEADER, trait.getValue(), trait));
            } else if (member.getTrait(HttpPrefixHeadersTrait.class).isPresent()) {
                HttpPrefixHeadersTrait trait = member.getTrait(HttpPrefixHeadersTrait.class).get();
                bindings.add(new Binding(member, Location.PREFIX_HEADERS, trait.getValue(), trait));
            } else if (isRequest && member.getTrait(HttpQueryTrait.class).isPresent()) {
                HttpQueryTrait trait = member.getTrait(HttpQueryTrait.class).get();
                bindings.add(new Binding(member, Location.QUERY, trait.getValue(), trait));
            } else if (member.getTrait(HttpPayloadTrait.class).isPresent()) {
                foundPayload = true;
                HttpPayloadTrait trait = member.getTrait(HttpPayloadTrait.class).get();
                bindings.add(new Binding(member, Location.PAYLOAD, member.getMemberName(), trait));
            } else if (isRequest && member.getTrait(HttpLabelTrait.class).isPresent()) {
                HttpLabelTrait trait = member.getTrait(HttpLabelTrait.class).get();
                bindings.add(new Binding(member, Location.LABEL, member.getMemberName(), trait));
            } else {
                unbound.add(member);
            }
        }

        if (!unbound.isEmpty()) {
            if (foundPayload) {
                unbound.forEach(member -> bindings.add(
                        new Binding(member, Location.UNBOUND, member.getMemberName(), null)));
            } else {
                unbound.forEach(member -> bindings.add(
                        new Binding(member, Location.DOCUMENT, member.getMemberName(), null)));
            }
        }

        return bindings;
    }
}
