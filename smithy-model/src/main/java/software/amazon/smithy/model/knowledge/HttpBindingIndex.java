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

package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * Computes and indexes the explicit and implicit HTTP bindings of a model.
 *
 * <p>This index makes it easy to understand how members of the input or output
 * of a request/response are bound to an HTTP message by providing all of the
 * bindings in the model as a normalized {@link HttpBinding} object. This can be
 * used to validate the bindings of an operation, generate code to [de]serialize
 * shapes, diff models to ensure backward compatibility at the wire level, etc.
 *
 * <p>This index does not perform validation of the underlying model.
 */
public final class HttpBindingIndex implements KnowledgeIndex {
    private final WeakReference<Model> model;
    private final Map<ShapeId, List<HttpBinding>> requestBindings = new HashMap<>();
    private final Map<ShapeId, List<HttpBinding>> responseBindings = new HashMap<>();

    public HttpBindingIndex(Model model) {
        this.model = new WeakReference<>(model);
        OperationIndex opIndex = OperationIndex.of(model);

        for (OperationShape operation : model.getOperationShapesWithTrait(HttpTrait.class)) {
            requestBindings.put(operation.getId(), computeRequestBindings(opIndex, operation));
            responseBindings.put(operation.getId(), computeResponseBindings(opIndex, operation));
        }

        // Add error structure bindings.
        for (StructureShape structure : model.getStructureShapesWithTrait(ErrorTrait.class)) {
            responseBindings.put(structure.getId(), createStructureBindings(structure, false));
        }
    }

    public static HttpBindingIndex of(Model model) {
        return model.getKnowledge(HttpBindingIndex.class, HttpBindingIndex::new);
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
                || shape.hasTrait(HttpQueryParamsTrait.class)
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
                || shape.hasTrait(HttpPayloadTrait.class)
                || shape.hasTrait(HttpResponseCodeTrait.class);
    }

    private HttpTrait getHttpTrait(ToShapeId operation) {
        ShapeId id = operation.toShapeId();
        return getModel().getShape(id)
                .orElseThrow(() -> new IllegalArgumentException(id + " is not a valid shape"))
                .asOperationShape()
                .orElseThrow(() -> new IllegalArgumentException(id + " is not an operation shape"))
                .getTrait(HttpTrait.class)
                .orElseThrow(() -> new IllegalArgumentException(id + " has no http binding trait"));
    }

    private Model getModel() {
        return Objects.requireNonNull(model.get(), "The dereferenced WeakReference<Model> is null");
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
        Shape shape = getModel().getShape(id).orElseThrow(() -> new IllegalArgumentException("Shape not found " + id));

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
     */
    public Map<String, HttpBinding> getRequestBindings(ToShapeId operationShapeOrId) {
        ShapeId id = operationShapeOrId.toShapeId();
        return requestBindings.getOrDefault(id, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(HttpBinding::getMemberName, Function.identity()));
    }

    /**
     * Gets the request bindings of an operation as a map of member name to
     * the binding for a specific location type.
     *
     * @param operationShapeOrId Operation to get the request bindings for.
     * @param requestLocation Location of the binding.
     * @return Map of unmodifiable bindings.
     */
    public List<HttpBinding> getRequestBindings(ToShapeId operationShapeOrId, HttpBinding.Location requestLocation) {
        ShapeId id = operationShapeOrId.toShapeId();
        return requestBindings.getOrDefault(id, Collections.emptyList())
                .stream()
                .filter(binding -> binding.getLocation() == requestLocation)
                .collect(Collectors.toList());
    }

    /**
     * Gets the computed HTTP message response bindings for an operation
     * or structure with an error trait.
     *
     * @param shapeOrId Operation or error structure shape or ID.
     * @return Map of unmodifiable bindings.
     */
    public Map<String, HttpBinding> getResponseBindings(ToShapeId shapeOrId) {
        ShapeId id = shapeOrId.toShapeId();
        return responseBindings.getOrDefault(id, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(HttpBinding::getMemberName, Function.identity()));
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
    public List<HttpBinding> getResponseBindings(ToShapeId shapeOrId, HttpBinding.Location bindingLocation) {
        ShapeId id = shapeOrId.toShapeId();
        return responseBindings.getOrDefault(id, Collections.emptyList())
                .stream()
                .filter(binding -> binding.getLocation() == bindingLocation)
                .collect(Collectors.toList());
    }

    /**
     * Determines the appropriate timestamp format for a member shape bound to
     * a specific location.
     *
     * @param member Member to derive the timestamp format.
     * @param location Location the member is bound to.
     * @param defaultFormat The format to use for the body or a default.
     * @return Returns the determined timestamp format.
     */
    public TimestampFormatTrait.Format determineTimestampFormat(
            ToShapeId member,
            HttpBinding.Location location,
            TimestampFormatTrait.Format defaultFormat
    ) {
        Model model = getModel();
        return model.getShape(member.toShapeId())
                // Use the timestampFormat trait on the member or target if present.
                .flatMap(shape -> shape.getMemberTrait(model, TimestampFormatTrait.class))
                .map(TimestampFormatTrait::getFormat)
                .orElseGet(() -> {
                    // Determine the format based on the location.
                    switch (location) {
                        case PREFIX_HEADERS:
                        case HEADER:
                            return TimestampFormatTrait.Format.HTTP_DATE;
                        case QUERY:
                        case LABEL:
                            return TimestampFormatTrait.Format.DATE_TIME;
                        default:
                            return defaultFormat;
                    }
                });
    }

    /**
     * Returns the expected request Content-Type of the given operation.
     *
     * <p>See {@link #determineRequestContentType(ToShapeId, String, String)}
     * for documentation on how the content-type is resolved.
     *
     * @param operation Operation to determine the content-type of.
     * @param documentContentType Content-Type to use for protocol documents.
     * @return Returns the optionally resolved content-type of the request.
     */
    public Optional<String> determineRequestContentType(ToShapeId operation, String documentContentType) {
        return determineRequestContentType(operation, documentContentType, null);
    }

    /**
     * Returns the expected request Content-Type of the given operation.
     *
     * <p>If members are sent in the "document" body, then the default
     * {@code documentContentType} value is returned. If a member is bound
     * to the payload, then the following checks are made:
     *
     * <ul>
     *     <li>If the payload has the {@link StreamingTrait}, then the
     *     {@code eventStreamContentType} is returned.</li>
     *     <li>If the targeted shape is a structure or document type, then
     *     the {@code documentContentType} is returned.</li>
     *     <li>If the targeted shape has the {@link MediaTypeTrait}, then
     *     the value of the trait is returned.</li>
     *     <li>If the targeted shape is a blob, then "application/octet-stream"
     *     is returned.</li>
     *     <li>If the targeted shape is a string, then "text/plain" is
     *     returned.</li>
     * </ul>
     *
     * <p>If no members are sent in the payload, an empty Optional is
     * returned.
     *
     * @param operation Operation to determine the content-type of.
     * @param documentContentType Content-Type to use for protocol documents.
     * @param eventStreamContentType Content-Type to use for event streams.
     * @return Returns the optionally resolved content-type of the request.
     */
    public Optional<String> determineRequestContentType(
            ToShapeId operation,
            String documentContentType,
            String eventStreamContentType
    ) {
        Collection<HttpBinding> bindings = getRequestBindings(operation).values();
        return Optional.ofNullable(determineContentType(bindings, documentContentType, eventStreamContentType));
    }

    /**
     * Returns the expected response Content-Type of the given operation
     * or error.
     *
     * <p>See {@link #determineResponseContentType(ToShapeId, String, String)}
     * for documentation on how the content-type is resolved.
     *
     * @param operationOrError Operation or error to determine the content-type of.
     * @param documentContentType Content-Type to use for protocol documents.
     * @return Returns the optionally resolved content-type of the response.
     */
    public Optional<String> determineResponseContentType(ToShapeId operationOrError, String documentContentType) {
        return determineResponseContentType(operationOrError, documentContentType, null);
    }

    /**
     * Returns the expected response Content-Type of the given operation
     * or error.
     *
     * <p>If members are sent in the "document" body, then the default
     * {@code documentContentType} value is returned. If a member is bound
     * to the payload, then the following checks are made:
     *
     * <ul>
     *     <li>If the payload has the {@link StreamingTrait}, then the
     *     {@code eventStreamContentType} is returned.</li>
     *     <li>If the targeted shape is a structure or document type, then
     *     the {@code documentContentType} is returned.</li>
     *     <li>If the targeted shape has the {@link MediaTypeTrait}, then
     *     the value of the trait is returned.</li>
     *     <li>If the targeted shape is a blob, then "application/octet-stream"
     *     is returned.</li>
     *     <li>If the targeted shape is a string, then "text/plain" is
     *     returned.</li>
     * </ul>
     *
     * <p>If no members are sent in the payload, an empty Optional is
     * returned.
     *
     * @param operationOrError Operation or error to determine the content-type of.
     * @param documentContentType Content-Type to use for protocol documents.
     * @param eventStreamContentType Content-Type used for event streams.
     * @return Returns the optionally resolved content-type of the response.
     */
    public Optional<String> determineResponseContentType(
            ToShapeId operationOrError,
            String documentContentType,
            String eventStreamContentType
    ) {
        Collection<HttpBinding> bindings = getResponseBindings(operationOrError).values();
        return Optional.ofNullable(determineContentType(bindings, documentContentType, eventStreamContentType));
    }

    private String determineContentType(
            Collection<HttpBinding> bindings,
            String documentContentType,
            String eventStreamContentType
    ) {
        Model model = getModel();

        for (HttpBinding binding : bindings) {
            if (binding.getLocation() == HttpBinding.Location.DOCUMENT) {
                return documentContentType;
            }

            if (binding.getLocation() == HttpBinding.Location.PAYLOAD) {
                Shape target = model.getShape(binding.getMember().getTarget()).orElse(null);
                if (target == null) {
                    // Can't determine the content-type because the model is broken :(
                    // Let other parts of the validation system point this out.
                    break;
                } else if (StreamingTrait.isEventStream(target)) {
                    return eventStreamContentType;
                } else if (target.isDocumentShape() || target.isStructureShape() || target.isUnionShape()
                        || target.isListShape() || target.isMapShape()) {
                    // Document type and structure targets are always the document content-type.
                    return documentContentType;
                } else if (target.getTrait(MediaTypeTrait.class).isPresent()) {
                    // Use the @mediaType trait if available.
                    return target.getTrait(MediaTypeTrait.class).get().getValue();
                } else if (target.isBlobShape()) {
                    return "application/octet-stream";
                } else if (target.isStringShape()) {
                    return "text/plain";
                }
            }
        }

        return null;
    }

    /**
     * Returns true if the request has a modeled body.
     *
     * @param operation Operation to check.
     * @return Returns true if the operation has document or payload bindings.
     */
    public boolean hasRequestBody(ToShapeId operation) {
        return hasPayloadBindings(getRequestBindings(operation).values());
    }

    /**
     * Returns true if the response has a modeled body.
     *
     * @param operation Operation to check.
     * @return Returns true if the operation has document or payload bindings.
     */
    public boolean hasResponseBody(ToShapeId operation) {
        return hasPayloadBindings(getResponseBindings(operation).values());
    }

    private boolean hasPayloadBindings(Collection<HttpBinding> bindings) {
        for (HttpBinding binding : bindings) {
            if (binding.getLocation() == HttpBinding.Location.DOCUMENT
                    || binding.getLocation() == HttpBinding.Location.PAYLOAD) {
                return true;
            }
        }
        return false;
    }

    private List<HttpBinding> computeRequestBindings(OperationIndex opIndex, OperationShape shape) {
        return createStructureBindings(opIndex.expectInputShape(shape.getId()), true);
    }

    private List<HttpBinding> computeResponseBindings(OperationIndex opIndex, OperationShape shape) {
        return createStructureBindings(opIndex.expectOutputShape(shape.getId()), false);
    }

    private List<HttpBinding> createStructureBindings(StructureShape struct, boolean isRequest) {
        List<HttpBinding> bindings = new ArrayList<>();
        List<MemberShape> unbound = new ArrayList<>();
        boolean foundPayload = false;

        for (MemberShape member : struct.getAllMembers().values()) {
            if (member.getTrait(HttpHeaderTrait.class).isPresent()) {
                HttpHeaderTrait trait = member.getTrait(HttpHeaderTrait.class).get();
                bindings.add(new HttpBinding(member, HttpBinding.Location.HEADER, trait.getValue(), trait));
            } else if (member.getTrait(HttpPrefixHeadersTrait.class).isPresent()) {
                HttpPrefixHeadersTrait trait = member.getTrait(HttpPrefixHeadersTrait.class).get();
                bindings.add(new HttpBinding(member, HttpBinding.Location.PREFIX_HEADERS, trait.getValue(), trait));
            } else if (isRequest && member.getTrait(HttpQueryTrait.class).isPresent()) {
                HttpQueryTrait trait = member.getTrait(HttpQueryTrait.class).get();
                bindings.add(new HttpBinding(member, HttpBinding.Location.QUERY, trait.getValue(), trait));
            } else if (isRequest && member.getTrait(HttpQueryParamsTrait.class).isPresent()) {
                HttpQueryParamsTrait trait = member.getTrait(HttpQueryParamsTrait.class).get();
                bindings.add(new HttpBinding(member, HttpBinding.Location.QUERY_PARAMS, member.getMemberName(), trait));
            } else if (member.getTrait(HttpPayloadTrait.class).isPresent()) {
                foundPayload = true;
                HttpPayloadTrait trait = member.getTrait(HttpPayloadTrait.class).get();
                bindings.add(new HttpBinding(member, HttpBinding.Location.PAYLOAD, member.getMemberName(), trait));
            } else if (isRequest && member.getTrait(HttpLabelTrait.class).isPresent()) {
                HttpLabelTrait trait = member.getTrait(HttpLabelTrait.class).get();
                bindings.add(new HttpBinding(member, HttpBinding.Location.LABEL, member.getMemberName(), trait));
            } else if (!isRequest && member.getTrait(HttpResponseCodeTrait.class).isPresent()) {
                HttpResponseCodeTrait trait = member.getTrait(HttpResponseCodeTrait.class).get();
                bindings.add(new HttpBinding(
                        member, HttpBinding.Location.RESPONSE_CODE, member.getMemberName(), trait));
            } else {
                unbound.add(member);
            }
        }

        if (!unbound.isEmpty()) {
            if (foundPayload) {
                unbound.forEach(member -> bindings.add(
                        new HttpBinding(member, HttpBinding.Location.UNBOUND, member.getMemberName(), null)));
            } else {
                unbound.forEach(member -> bindings.add(
                        new HttpBinding(member, HttpBinding.Location.DOCUMENT, member.getMemberName(), null)));
            }
        }

        return bindings;
    }
}
