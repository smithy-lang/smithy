/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Writes a fully resolved Smithy Model to SMF binary format.
 *
 * <p>Use the {@link Builder} to configure filtering options that control
 * which shapes, traits, and metadata are serialized.
 */
@SmithyUnstableApi
public final class SmfWriter {

    private final Predicate<String> metadataFilter;
    private final Predicate<Shape> shapeFilter;
    private final Predicate<Trait> traitFilter;
    private final Predicate<Trait> coldTraitFilter;
    private final boolean splitColdTraits;

    private Model model;
    private List<Shape> shapes;
    private Map<String, Integer> symbolIndex;
    private List<String> localSymbols;
    private Map<ByteBuffer, Integer> traitValueIndex;
    private List<byte[]> traitValues;
    private Map<ByteBuffer, Integer> coldTraitValueIndex;
    private List<byte[]> coldTraitValues;
    // Map each interned trait-value Node to its index in the (hot/cold) trait
    // value table, so repeated trait values resolve to the same ref without
    // re-serializing. Reset per serialize() call like the other mutable state.
    private Map<Node, Integer> nodeToTraitValueRef;
    private Map<Node, Integer> coldNodeToTraitValueRef;
    private byte[] buf;
    private int pos;

    private SmfWriter(Builder builder) {
        this.metadataFilter = builder.metadataFilter;
        if (!builder.includePrelude) {
            this.shapeFilter = builder.shapeFilter.and(FunctionalUtils.not(Prelude::isPreludeShape));
        } else {
            this.shapeFilter = builder.shapeFilter;
        }
        this.traitFilter = builder.traitFilter.and(FunctionalUtils.not(Trait::isSynthetic));
        this.coldTraitFilter = builder.coldTraitFilter;
        this.splitColdTraits = builder.splitColdTraits;
    }

    /**
     * @return Returns a builder used to create a {@link SmfWriter}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Serializes a model to SMF bytes.
     *
     * @param model Model to serialize.
     * @return SMF encoded bytes.
     */
    public byte[] serialize(Model model) {
        this.model = model;
        this.shapes = new ArrayList<>();
        this.symbolIndex = new HashMap<>();
        this.localSymbols = new ArrayList<>();
        this.traitValueIndex = new HashMap<>();
        this.traitValues = new ArrayList<>();
        this.coldTraitValueIndex = new HashMap<>();
        this.coldTraitValues = new ArrayList<>();
        this.nodeToTraitValueRef = new HashMap<>();
        this.coldNodeToTraitValueRef = new HashMap<>();
        this.buf = new byte[4096];
        this.pos = 0;
        return doSerialize();
    }

    private byte[] doSerialize() {
        collectShapes();
        buildSymbolTable();
        buildTraitValueTable();

        // Phase 1: serialize each shape independently to get byte lengths
        List<byte[]> shapeBytes = new ArrayList<>(shapes.size());
        for (Shape shape : shapes) {
            pos = 0;
            writeShape(shape);
            byte[] data = new byte[pos];
            System.arraycopy(buf, 0, data, 0, pos);
            shapeBytes.add(data);
        }

        // Phase 2: write the full file
        pos = 0;
        writeHeader();
        writeSymbolTable();
        writeTraitValueTable();
        writeShapeIndex(shapeBytes);
        writeMetadataSection();
        writeShapesSection(shapeBytes);

        // Append CRC-32C over all preceding bytes
        int crc = Crc32C.compute(buf, 0, pos);
        ensure(4);
        buf[pos++] = (byte) crc;
        buf[pos++] = (byte) (crc >> 8);
        buf[pos++] = (byte) (crc >> 16);
        buf[pos++] = (byte) (crc >> 24);

        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
        return result;
    }

    private void collectShapes() {
        for (Shape shape : model.toSet()) {
            if (shape.getType() != ShapeType.MEMBER && shapeFilter.test(shape)) {
                shapes.add(shape);
            }
        }
        shapes.sort(Comparator.comparing(Shape::getId));
    }

    private void buildSymbolTable() {
        // Count frequency of all strings that will be referenced as SymRefs
        Map<String, int[]> freq = new LinkedHashMap<>();

        // Metadata keys (only those that pass the filter)
        for (String key : model.getMetadata().keySet()) {
            if (metadataFilter.test(key)) {
                countSymbol(freq, key);
            }
        }

        for (Shape shape : shapes) {
            countSymbol(freq, shape.getId().toString());
            for (Map.Entry<ShapeId, Trait> entry : shape.getAllTraits().entrySet()) {
                if (traitFilter.test(entry.getValue())) {
                    countSymbol(freq, entry.getKey().toString());
                    // Intern strings/keys only from hot trait values. Cold
                    // traits (docs/examples/etc.) are decoded only on full
                    // reads, are dominated by unique long strings, and would
                    // pollute the symbol table with frequency-1 entries the
                    // selective hot path must still carry. They fall back to
                    // inline string/object encodings.
                    if (!isColdTrait(entry.getValue())) {
                        countDynamicValueSymbols(freq, entry.getValue().toNode());
                    }
                }
            }
            for (MemberShape member : shape.members()) {
                countSymbol(freq, member.getMemberName());
                countSymbol(freq, member.getTarget().toString());
                for (Map.Entry<ShapeId, Trait> entry : member.getAllTraits().entrySet()) {
                    if (traitFilter.test(entry.getValue())) {
                        countSymbol(freq, entry.getKey().toString());
                        if (!isColdTrait(entry.getValue())) {
                            countDynamicValueSymbols(freq, entry.getValue().toNode());
                        }
                    }
                }
            }
            countShapeSpecificSymbols(freq, shape);
        }

        for (Map.Entry<String, Node> entry : model.getMetadata().entrySet()) {
            if (metadataFilter.test(entry.getKey())) {
                countDynamicValueSymbols(freq, entry.getValue());
            }
        }

        // Remove symbols already in the shared table
        freq.keySet().removeAll(SmfSharedSymbols.REVERSE_INDEX.keySet());

        // Sort by frequency descending, assign IDs
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(freq.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));

        int nextId = SmfSharedSymbols.size() + 1;
        for (Map.Entry<String, int[]> entry : sorted) {
            symbolIndex.put(entry.getKey(), nextId++);
            localSymbols.add(entry.getKey());
        }

        // Add shared symbols to the lookup index so symRef() can resolve them.
        // They were excluded from local symbol assignment above (they're never
        // written to the file) but still need valid IDs for serialization.
        for (Map.Entry<String, Integer> entry : SmfSharedSymbols.REVERSE_INDEX.entrySet()) {
            symbolIndex.put(entry.getKey(), entry.getValue());
        }
    }

    private void buildTraitValueTable() {
        for (Shape shape : shapes) {
            for (Map.Entry<ShapeId, Trait> entry : shape.getAllTraits().entrySet()) {
                if (traitFilter.test(entry.getValue())) {
                    internTraitValue(entry.getValue());
                }
            }
            for (MemberShape member : shape.members()) {
                for (Map.Entry<ShapeId, Trait> entry : member.getAllTraits().entrySet()) {
                    if (traitFilter.test(entry.getValue())) {
                        internTraitValue(entry.getValue());
                    }
                }
            }
        }
    }

    private void internTraitValue(Trait trait) {
        Node value = trait.toNode();
        if (!splitColdTraits || !isColdTrait(trait)) {
            // Hot values may use symref string/object-key encodings.
            internTraitValue(value, nodeToTraitValueRef, traitValueIndex, traitValues, true);
        } else {
            // Cold values are inline-only: they are decoded only on full reads,
            // and their strings are not interned into the symbol table.
            internTraitValue(value, coldNodeToTraitValueRef, coldTraitValueIndex, coldTraitValues, false);
        }
    }

    private void internTraitValue(
            Node value,
            Map<Node, Integer> nodeRefs,
            Map<ByteBuffer, Integer> valueIndex,
            List<byte[]> values,
            boolean useSymrefs
    ) {
        if (nodeRefs.containsKey(value)) {
            return;
        }
        // Serialize the value to bytes using a temporary buffer
        int savedPos = pos;
        pos = 0;
        writeDynamicValue(value, useSymrefs);
        byte[] encoded = new byte[pos];
        System.arraycopy(buf, 0, encoded, 0, pos);
        pos = savedPos;

        ByteBuffer key = ByteBuffer.wrap(encoded);
        Integer existing = valueIndex.get(key);
        if (existing != null) {
            nodeRefs.put(value, existing);
            return;
        }
        int idx = values.size();
        valueIndex.put(key, idx);
        values.add(encoded);
        nodeRefs.put(value, idx);
    }

    private boolean isColdTrait(Trait trait) {
        return splitColdTraits && coldTraitFilter.test(trait);
    }

    private int getTraitValueRef(Trait trait) {
        Node value = trait.toNode();
        boolean cold = isColdTrait(trait);
        Map<Node, Integer> nodeRefs = cold ? coldNodeToTraitValueRef : nodeToTraitValueRef;
        Map<ByteBuffer, Integer> valueIndex = cold ? coldTraitValueIndex : traitValueIndex;
        Integer ref = nodeRefs.get(value);
        if (ref != null) {
            return ref;
        }
        // Fallback: serialize and look up by bytes
        boolean useSymrefs = !cold;
        int savedPos = pos;
        pos = 0;
        writeDynamicValue(value, useSymrefs);
        byte[] encoded = new byte[pos];
        System.arraycopy(buf, 0, encoded, 0, pos);
        pos = savedPos;
        ref = valueIndex.get(ByteBuffer.wrap(encoded));
        if (ref == null) {
            throw new IllegalStateException("Trait value not interned");
        }
        return ref;
    }

    private void countSymbol(Map<String, int[]> freq, String s) {
        freq.computeIfAbsent(s, k -> new int[1])[0]++;
    }

    // Counts object keys inside a dynamic value so they can be interned and
    // referenced as SymRefs. Only called for hot
    // trait values and metadata.
    //
    // Note: string VALUES are intentionally not counted here. They are encoded
    // as symrefs at write time only when they already match a structural symbol
    // (member name, shape id, target) that was interned anyway. This captures
    // the large class of enumValue/ec2QueryName values that equal a member name
    // without adding a single new symbol to the table, so unique free-form
    // strings (documentation-like) never bloat it.
    private void countDynamicValueSymbols(Map<String, int[]> freq, Node node) {
        if (node.isObjectNode()) {
            for (Map.Entry<StringNode, Node> entry : node.expectObjectNode().getMembers().entrySet()) {
                countSymbol(freq, entry.getKey().getValue());
                countDynamicValueSymbols(freq, entry.getValue());
            }
        } else if (node.isArrayNode()) {
            for (Node element : node.expectArrayNode().getElements()) {
                countDynamicValueSymbols(freq, element);
            }
        }
    }

    private void countShapeSpecificSymbols(Map<String, int[]> freq, Shape shape) {
        switch (shape.getType()) {
            case OPERATION:
                OperationShape op = (OperationShape) shape;
                op.getInput().ifPresent(id -> countSymbol(freq, id.toString()));
                op.getOutput().ifPresent(id -> countSymbol(freq, id.toString()));
                op.getErrors().forEach(id -> countSymbol(freq, id.toString()));
                break;
            case RESOURCE:
                ResourceShape res = (ResourceShape) shape;
                res.getPut().ifPresent(id -> countSymbol(freq, id.toString()));
                res.getCreate().ifPresent(id -> countSymbol(freq, id.toString()));
                res.getRead().ifPresent(id -> countSymbol(freq, id.toString()));
                res.getUpdate().ifPresent(id -> countSymbol(freq, id.toString()));
                res.getDelete().ifPresent(id -> countSymbol(freq, id.toString()));
                res.getList().ifPresent(id -> countSymbol(freq, id.toString()));
                res.getIdentifiers().forEach((n, id) -> {
                    countSymbol(freq, n);
                    countSymbol(freq, id.toString());
                });
                res.getProperties().forEach((n, id) -> {
                    countSymbol(freq, n);
                    countSymbol(freq, id.toString());
                });
                res.getOperations().forEach(id -> countSymbol(freq, id.toString()));
                res.getCollectionOperations().forEach(id -> countSymbol(freq, id.toString()));
                res.getResources().forEach(id -> countSymbol(freq, id.toString()));
                break;
            case SERVICE:
                ServiceShape svc = (ServiceShape) shape;
                svc.getOperations().forEach(id -> countSymbol(freq, id.toString()));
                svc.getResources().forEach(id -> countSymbol(freq, id.toString()));
                svc.getErrors().forEach(id -> countSymbol(freq, id.toString()));
                svc.getRename().keySet().forEach(id -> countSymbol(freq, id.toString()));
                break;
            default:
                break;
        }
        // Mixins
        shape.getMixins().forEach(id -> countSymbol(freq, id.toString()));
    }

    private int symRef(String s) {
        Integer id = symbolIndex.get(s);
        if (id == null) {
            throw new IllegalStateException("Symbol not in table: " + s);
        }
        return id;
    }

    // --- Section writers ---

    private void writeHeader() {
        ensure(SmfConstants.HEADER_SIZE);
        buf[pos++] = (byte) (SmfConstants.MAGIC >> 24);
        buf[pos++] = (byte) (SmfConstants.MAGIC >> 16);
        buf[pos++] = (byte) (SmfConstants.MAGIC >> 8);
        buf[pos++] = (byte) SmfConstants.MAGIC;
        buf[pos++] = SmfConstants.FORMAT_VERSION;
        buf[pos++] = 2; // Smithy major version
        buf[pos++] = 0; // Smithy minor version
        int flags = 0;
        if (hasFilteredMetadata()) {
            flags |= SmfConstants.FLAG_HAS_METADATA;
        }
        flags |= SmfConstants.FLAG_HAS_SHAPE_INDEX; // always write index for now
        flags |= SmfConstants.FLAG_HAS_TRAIT_VALUE_OFFSETS;
        if (splitColdTraits) {
            flags |= SmfConstants.FLAG_SPLIT_COLD_TRAITS;
        }
        buf[pos++] = (byte) flags;
    }

    private boolean hasFilteredMetadata() {
        for (String key : model.getMetadata().keySet()) {
            if (metadataFilter.test(key)) {
                return true;
            }
        }
        return false;
    }

    private void writeSymbolTable() {
        writeVarUInt(1); // sharedTableId = smithy-core
        writeVarUInt(SmfSharedSymbols.VERSION);
        writeVarUInt(localSymbols.size());
        for (String s : localSymbols) {
            writeString(s);
        }
    }

    private void writeTraitValueTable() {
        writeEncodedTraitValueTable(traitValues);
        if (splitColdTraits) {
            writeEncodedTraitValueTable(coldTraitValues);
        }
    }

    private void writeEncodedTraitValueTable(List<byte[]> values) {
        writeVarUInt(values.size());
        int totalValueBytes = 0;
        for (byte[] value : values) {
            totalValueBytes += value.length;
        }
        writeVarUInt(totalValueBytes);

        int offset = 0;
        ensure(values.size() * 4);
        for (byte[] value : values) {
            buf[pos++] = (byte) offset;
            buf[pos++] = (byte) (offset >> 8);
            buf[pos++] = (byte) (offset >> 16);
            buf[pos++] = (byte) (offset >> 24);
            offset += value.length;
        }

        for (byte[] value : values) {
            ensure(value.length);
            System.arraycopy(value, 0, buf, pos, value.length);
            pos += value.length;
        }
    }

    private void writeShapeIndex(List<byte[]> shapeBytes) {
        // Build index data: collect entries sorted by symref for binary search
        int[][] entries = new int[shapes.size()][]; // [symref, type, offset, neighborStart, neighborCount]
        List<int[]> allNeighbors = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < shapes.size(); i++) {
            Shape shape = shapes.get(i);
            int sym = symRef(shape.getId().toString());
            List<ShapeId> neighbors = getNeighbors(shape);
            // The shape index encodes neighborCount as a uint16 (2 bytes) to keep
            // index entries at a fixed 15 bytes, which enables a compact,
            // cache-friendly binary-searchable table. A shape with more than
            // 65,535 direct structural neighbors would silently overflow this
            // field and corrupt the index, so fail hard instead. No known model
            // approaches this limit; if one ever does, the index entry layout
            // (and this field width) must be revised with a format version bump.
            if (neighbors.size() > 0xFFFF) {
                throw new IllegalStateException("Shape " + shape.getId()
                        + " has " + neighbors.size() + " neighbors, which exceeds the "
                        + "SMF shape index limit of 65535 (uint16 neighborCount)");
            }
            int neighborStart = allNeighbors.size();
            for (ShapeId neighbor : neighbors) {
                allNeighbors.add(new int[] {symRef(neighbor.toString())});
            }
            entries[i] = new int[] {sym,
                    SmfConstants.shapeTypeToByte(shape.getType()) & 0xFF,
                    offset,
                    neighborStart,
                    neighbors.size()};
            offset += shapeBytes.get(i).length;
        }
        // Sort by symref for binary search
        Arrays.sort(entries, (a, b) -> Integer.compare(a[0], b[0]));

        // Write: entryCount + totalNeighborCount + fixed-size table + flat neighbors
        writeVarUInt(entries.length);
        writeVarUInt(allNeighbors.size());

        // Fixed-size entry table (15 bytes each)
        ensure(entries.length * SmfConstants.INDEX_ENTRY_SIZE);
        for (int[] e : entries) {
            // symref: 4 bytes LE
            buf[pos++] = (byte) e[0];
            buf[pos++] = (byte) (e[0] >> 8);
            buf[pos++] = (byte) (e[0] >> 16);
            buf[pos++] = (byte) (e[0] >> 24);
            // type: 1 byte
            buf[pos++] = (byte) e[1];
            // offset: 4 bytes LE
            buf[pos++] = (byte) e[2];
            buf[pos++] = (byte) (e[2] >> 8);
            buf[pos++] = (byte) (e[2] >> 16);
            buf[pos++] = (byte) (e[2] >> 24);
            // neighborStart: 4 bytes LE
            buf[pos++] = (byte) e[3];
            buf[pos++] = (byte) (e[3] >> 8);
            buf[pos++] = (byte) (e[3] >> 16);
            buf[pos++] = (byte) (e[3] >> 24);
            // neighborCount: 2 bytes LE
            buf[pos++] = (byte) e[4];
            buf[pos++] = (byte) (e[4] >> 8);
        }

        // Flat neighbor array (4 bytes each)
        ensure(allNeighbors.size() * 4);
        for (int[] n : allNeighbors) {
            buf[pos++] = (byte) n[0];
            buf[pos++] = (byte) (n[0] >> 8);
            buf[pos++] = (byte) (n[0] >> 16);
            buf[pos++] = (byte) (n[0] >> 24);
        }
    }

    private List<ShapeId> getNeighbors(Shape shape) {
        List<ShapeId> neighbors = new ArrayList<>();
        // Member targets
        for (MemberShape member : shape.members()) {
            addNonPrelude(neighbors, member.getTarget());
        }
        // Shape-type-specific references
        if (shape instanceof OperationShape) {
            OperationShape op = (OperationShape) shape;
            op.getInput().ifPresent(id -> addNonPrelude(neighbors, id));
            op.getOutput().ifPresent(id -> addNonPrelude(neighbors, id));
            op.getErrors().forEach(id -> addNonPrelude(neighbors, id));
        } else if (shape instanceof ResourceShape) {
            ResourceShape res = (ResourceShape) shape;
            res.getPut().ifPresent(id -> addNonPrelude(neighbors, id));
            res.getCreate().ifPresent(id -> addNonPrelude(neighbors, id));
            res.getRead().ifPresent(id -> addNonPrelude(neighbors, id));
            res.getUpdate().ifPresent(id -> addNonPrelude(neighbors, id));
            res.getDelete().ifPresent(id -> addNonPrelude(neighbors, id));
            res.getList().ifPresent(id -> addNonPrelude(neighbors, id));
            res.getOperations().forEach(id -> addNonPrelude(neighbors, id));
            res.getCollectionOperations().forEach(id -> addNonPrelude(neighbors, id));
            res.getResources().forEach(id -> addNonPrelude(neighbors, id));
            res.getIdentifiers().values().forEach(id -> addNonPrelude(neighbors, id));
            res.getProperties().values().forEach(id -> addNonPrelude(neighbors, id));
        } else if (shape instanceof ServiceShape) {
            ServiceShape svc = (ServiceShape) shape;
            svc.getOperations().forEach(id -> addNonPrelude(neighbors, id));
            svc.getResources().forEach(id -> addNonPrelude(neighbors, id));
            svc.getErrors().forEach(id -> addNonPrelude(neighbors, id));
        }
        return neighbors;
    }

    private static void addNonPrelude(List<ShapeId> list, ShapeId id) {
        if (!id.getNamespace().equals("smithy.api")) {
            list.add(id);
        }
    }

    private void writeMetadataSection() {
        Map<String, Node> metadata = model.getMetadata();
        List<Map.Entry<String, Node>> filtered = new ArrayList<>();
        for (Map.Entry<String, Node> entry : metadata.entrySet()) {
            if (metadataFilter.test(entry.getKey())) {
                filtered.add(entry);
            }
        }
        if (filtered.isEmpty()) {
            return;
        }
        writeVarUInt(filtered.size());
        for (Map.Entry<String, Node> entry : filtered) {
            writeVarUInt(symRef(entry.getKey()));
            writeDynamicValue(entry.getValue(), true);
        }
    }

    private void writeShapesSection(List<byte[]> shapeBytes) {
        writeVarUInt(shapes.size());
        for (byte[] data : shapeBytes) {
            ensure(data.length);
            System.arraycopy(data, 0, buf, pos, data.length);
            pos += data.length;
        }
    }

    // --- Shape writing ---

    private void writeShape(Shape shape) {
        writeVarUInt(symRef(shape.getId().toString()));

        int contentStart = pos;
        ensure(1);
        buf[pos++] = SmfConstants.shapeTypeToByte(shape.getType());
        writeTraits(shape.getAllTraits());
        writeShapePayload(shape);
        int contentLen = pos - contentStart;

        // Now we need to insert byteLength between shapeId and content.
        // Shift content right to make room for the VarUInt length.
        int lengthSize = LEB128.varUIntSize(contentLen);
        ensure(lengthSize);
        System.arraycopy(buf, contentStart, buf, contentStart + lengthSize, contentLen);
        LEB128.writeVarUInt(buf, contentStart, contentLen);
        pos = contentStart + lengthSize + contentLen;
    }

    private void writeShapePayload(Shape shape) {
        switch (shape.getType()) {
            case BLOB:
            case BOOLEAN:
            case STRING:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_DECIMAL:
            case BIG_INTEGER:
            case TIMESTAMP:
            case DOCUMENT:
                writeMixins(shape);
                break;
            case ENUM:
            case INT_ENUM:
            case STRUCTURE:
            case UNION:
                writeNamedMembers(shape);
                writeMixins(shape);
                break;
            case LIST:
                writeCollectionPayload((CollectionShape) shape);
                writeMixins(shape);
                break;
            case MAP:
                writeMapPayload((MapShape) shape);
                writeMixins(shape);
                break;
            case OPERATION:
                writeOperationPayload((OperationShape) shape);
                writeMixins(shape);
                break;
            case RESOURCE:
                writeResourcePayload((ResourceShape) shape);
                writeMixins(shape);
                break;
            case SERVICE:
                writeServicePayload((ServiceShape) shape);
                writeMixins(shape);
                break;
            default:
                break;
        }
    }

    private void writeTraits(Map<ShapeId, Trait> traits) {
        List<Map.Entry<ShapeId, Trait>> filtered = new ArrayList<>();
        for (Map.Entry<ShapeId, Trait> entry : traits.entrySet()) {
            if (traitFilter.test(entry.getValue())) {
                filtered.add(entry);
            }
        }
        if (!splitColdTraits) {
            writeVarUInt(filtered.size());
            for (Map.Entry<ShapeId, Trait> entry : filtered) {
                writeVarUInt(symRef(entry.getKey().toString()));
                writeVarUInt(getTraitValueRef(entry.getValue()));
            }
            return;
        }

        List<Map.Entry<ShapeId, Trait>> hotTraits = new ArrayList<>();
        List<Map.Entry<ShapeId, Trait>> coldTraits = new ArrayList<>();
        for (Map.Entry<ShapeId, Trait> entry : filtered) {
            if (isColdTrait(entry.getValue())) {
                coldTraits.add(entry);
            } else {
                hotTraits.add(entry);
            }
        }

        writeVarUInt(hotTraits.size());
        for (Map.Entry<ShapeId, Trait> entry : hotTraits) {
            writeVarUInt(symRef(entry.getKey().toString()));
            writeVarUInt(getTraitValueRef(entry.getValue()));
        }
        writeVarUInt(coldTraits.size());
        for (Map.Entry<ShapeId, Trait> entry : coldTraits) {
            writeVarUInt(symRef(entry.getKey().toString()));
            writeVarUInt(getTraitValueRef(entry.getValue()));
        }
    }

    private void writeNamedMembers(Shape shape) {
        List<MemberShape> members = new ArrayList<>(shape.members());
        writeVarUInt(members.size());
        for (MemberShape member : members) {
            writeVarUInt(symRef(member.getMemberName()));
            writeVarUInt(symRef(member.getTarget().toString()));
            writeTraits(member.getAllTraits());
        }
    }

    private void writeCollectionPayload(CollectionShape shape) {
        MemberShape member = shape.getMember();
        writeVarUInt(symRef(member.getTarget().toString()));
        writeTraits(member.getAllTraits());
    }

    private void writeMapPayload(MapShape shape) {
        writeVarUInt(symRef(shape.getKey().getTarget().toString()));
        writeTraits(shape.getKey().getAllTraits());
        writeVarUInt(symRef(shape.getValue().getTarget().toString()));
        writeTraits(shape.getValue().getAllTraits());
    }

    private void writeOperationPayload(OperationShape shape) {
        int flags = 0;
        if (shape.getInput().isPresent()) {
            flags |= SmfConstants.OP_HAS_INPUT;
        }
        if (shape.getOutput().isPresent()) {
            flags |= SmfConstants.OP_HAS_OUTPUT;
        }
        if (!shape.getErrors().isEmpty()) {
            flags |= SmfConstants.OP_HAS_ERRORS;
        }
        ensure(1);
        buf[pos++] = (byte) flags;
        shape.getInput().ifPresent(id -> writeVarUInt(symRef(id.toString())));
        shape.getOutput().ifPresent(id -> writeVarUInt(symRef(id.toString())));
        if (!shape.getErrors().isEmpty()) {
            writeVarUInt(shape.getErrors().size());
            for (ShapeId id : shape.getErrors()) {
                writeVarUInt(symRef(id.toString()));
            }
        }
    }

    private void writeResourcePayload(ResourceShape shape) {
        int flags = 0;
        if (shape.getPut().isPresent()) {
            flags |= SmfConstants.RES_HAS_PUT;
        }
        if (shape.getCreate().isPresent()) {
            flags |= SmfConstants.RES_HAS_CREATE;
        }
        if (shape.getRead().isPresent()) {
            flags |= SmfConstants.RES_HAS_READ;
        }
        if (shape.getUpdate().isPresent()) {
            flags |= SmfConstants.RES_HAS_UPDATE;
        }
        if (shape.getDelete().isPresent()) {
            flags |= SmfConstants.RES_HAS_DELETE;
        }
        if (shape.getList().isPresent()) {
            flags |= SmfConstants.RES_HAS_LIST;
        }
        ensure(1);
        buf[pos++] = (byte) flags;
        shape.getPut().ifPresent(id -> writeVarUInt(symRef(id.toString())));
        shape.getCreate().ifPresent(id -> writeVarUInt(symRef(id.toString())));
        shape.getRead().ifPresent(id -> writeVarUInt(symRef(id.toString())));
        shape.getUpdate().ifPresent(id -> writeVarUInt(symRef(id.toString())));
        shape.getDelete().ifPresent(id -> writeVarUInt(symRef(id.toString())));
        shape.getList().ifPresent(id -> writeVarUInt(symRef(id.toString())));

        Map<String, ShapeId> identifiers = shape.getIdentifiers();
        writeVarUInt(identifiers.size());
        for (Map.Entry<String, ShapeId> entry : identifiers.entrySet()) {
            writeVarUInt(symRef(entry.getKey()));
            writeVarUInt(symRef(entry.getValue().toString()));
        }

        Map<String, ShapeId> properties = shape.getProperties();
        writeVarUInt(properties.size());
        for (Map.Entry<String, ShapeId> entry : properties.entrySet()) {
            writeVarUInt(symRef(entry.getKey()));
            writeVarUInt(symRef(entry.getValue().toString()));
        }

        Set<ShapeId> ops = shape.getOperations();
        writeVarUInt(ops.size());
        for (ShapeId id : ops) {
            writeVarUInt(symRef(id.toString()));
        }

        Set<ShapeId> collOps = shape.getCollectionOperations();
        writeVarUInt(collOps.size());
        for (ShapeId id : collOps) {
            writeVarUInt(symRef(id.toString()));
        }

        Set<ShapeId> resources = shape.getResources();
        writeVarUInt(resources.size());
        for (ShapeId id : resources) {
            writeVarUInt(symRef(id.toString()));
        }
    }

    private void writeServicePayload(ServiceShape shape) {
        writeString(shape.getVersion());

        Set<ShapeId> ops = shape.getOperations();
        writeVarUInt(ops.size());
        for (ShapeId id : ops) {
            writeVarUInt(symRef(id.toString()));
        }

        Set<ShapeId> resources = shape.getResources();
        writeVarUInt(resources.size());
        for (ShapeId id : resources) {
            writeVarUInt(symRef(id.toString()));
        }

        List<ShapeId> errors = shape.getErrors();
        writeVarUInt(errors.size());
        for (ShapeId id : errors) {
            writeVarUInt(symRef(id.toString()));
        }

        Map<ShapeId, String> renames = shape.getRename();
        writeVarUInt(renames.size());
        for (Map.Entry<ShapeId, String> entry : renames.entrySet()) {
            writeVarUInt(symRef(entry.getKey().toString()));
            writeString(entry.getValue());
        }
    }

    private void writeMixins(Shape shape) {
        Set<ShapeId> mixins = shape.getMixins();
        writeVarUInt(mixins.size());
        for (ShapeId id : mixins) {
            writeVarUInt(symRef(id.toString()));
        }
    }

    // --- Dynamic value writing ---

    private void writeDynamicValue(Node node) {
        writeDynamicValue(node, false);
    }

    private void writeDynamicValue(Node node, boolean useSymrefs) {
        ensure(1);
        if (node.isNullNode()) {
            buf[pos++] = SmfConstants.VALUE_NULL;
        } else if (node.isBooleanNode()) {
            buf[pos++] = node.expectBooleanNode().getValue()
                    ? SmfConstants.VALUE_TRUE
                    : SmfConstants.VALUE_FALSE;
        } else if (node.isNumberNode()) {
            writeNumberValue(node.expectNumberNode());
        } else if (node.isStringNode()) {
            String value = node.expectStringNode().getValue();
            // Encode as a symref only when the value is already a structural
            // symbol (member name, shape id, target). This captures
            // enumValue/ec2QueryName values equal to member names without
            // adding any new symbols.
            if (useSymrefs && symbolIndex.containsKey(value)) {
                buf[pos++] = SmfConstants.VALUE_STRING_SYMREF;
                writeVarUInt(symRef(value));
            } else {
                writeStringValue(node.expectStringNode());
            }
        } else if (node.isArrayNode()) {
            ArrayNode arr = node.expectArrayNode();
            buf[pos++] = SmfConstants.VALUE_LIST;
            writeVarUInt(arr.size());
            for (Node element : arr.getElements()) {
                writeDynamicValue(element, useSymrefs);
            }
        } else if (node.isObjectNode()) {
            ObjectNode obj = node.expectObjectNode();
            if (obj.isEmpty()) {
                buf[pos++] = SmfConstants.VALUE_EMPTY_OBJECT;
            } else if (useSymrefs && allKeysInterned(obj)) {
                buf[pos++] = SmfConstants.VALUE_OBJECT_SYMREF_KEYS;
                writeVarUInt(obj.size());
                for (Map.Entry<StringNode, Node> entry : obj.getMembers().entrySet()) {
                    writeVarUInt(symRef(entry.getKey().getValue()));
                    writeDynamicValue(entry.getValue(), useSymrefs);
                }
            } else {
                buf[pos++] = SmfConstants.VALUE_OBJECT;
                writeVarUInt(obj.size());
                for (Map.Entry<StringNode, Node> entry : obj.getMembers().entrySet()) {
                    writeString(entry.getKey().getValue());
                    writeDynamicValue(entry.getValue(), useSymrefs);
                }
            }
        }
    }

    private void writeNumberValue(NumberNode node) {
        Number num = node.getValue();
        if (num instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) num;
            if (bd.scale() == 0 && bd.unscaledValue().bitLength() <= 63) {
                buf[pos++] = SmfConstants.VALUE_INTEGER;
                writeVarInt(bd.longValueExact());
            } else if (bd.scale() == 0) {
                buf[pos++] = SmfConstants.VALUE_BIG_INTEGER;
                writeString(bd.unscaledValue().toString());
            } else {
                buf[pos++] = SmfConstants.VALUE_BIG_DECIMAL;
                writeString(bd.toPlainString());
            }
        } else if (num instanceof BigInteger) {
            BigInteger bi = (BigInteger) num;
            if (bi.bitLength() <= 63) {
                buf[pos++] = SmfConstants.VALUE_INTEGER;
                writeVarInt(bi.longValueExact());
            } else {
                buf[pos++] = SmfConstants.VALUE_BIG_INTEGER;
                writeString(bi.toString());
            }
        } else if (num instanceof Double || num instanceof Float) {
            buf[pos++] = SmfConstants.VALUE_DOUBLE;
            ensure(8);
            long bits = Double.doubleToRawLongBits(num.doubleValue());
            buf[pos++] = (byte) bits;
            buf[pos++] = (byte) (bits >> 8);
            buf[pos++] = (byte) (bits >> 16);
            buf[pos++] = (byte) (bits >> 24);
            buf[pos++] = (byte) (bits >> 32);
            buf[pos++] = (byte) (bits >> 40);
            buf[pos++] = (byte) (bits >> 48);
            buf[pos++] = (byte) (bits >> 56);
        } else {
            buf[pos++] = SmfConstants.VALUE_INTEGER;
            writeVarInt(num.longValue());
        }
    }

    private void writeStringValue(StringNode node) {
        ensure(1);
        buf[pos++] = SmfConstants.VALUE_STRING;
        writeString(node.getValue());
    }

    private boolean allKeysInterned(ObjectNode obj) {
        for (StringNode key : obj.getMembers().keySet()) {
            if (!symbolIndex.containsKey(key.getValue())) {
                return false;
            }
        }
        return true;
    }

    // --- Buffer primitives ---

    private void writeVarUInt(int value) {
        ensure(5);
        pos = LEB128.writeVarUInt(buf, pos, value);
    }

    private void writeVarInt(long value) {
        ensure(10);
        pos = LEB128.writeVarInt(buf, pos, value);
    }

    private void writeString(String s) {
        if (isAscii(s)) {
            writeVarUInt(s.length());
            ensure(s.length());
            for (int i = 0; i < s.length(); i++) {
                buf[pos++] = (byte) s.charAt(i);
            }
            return;
        }

        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarUInt(bytes.length);
        ensure(bytes.length);
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        pos += bytes.length;
    }

    private boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    private void ensure(int needed) {
        if (pos + needed > buf.length) {
            int newLen = Math.max(buf.length * 2, pos + needed);
            byte[] newBuf = new byte[newLen];
            System.arraycopy(buf, 0, newBuf, 0, pos);
            buf = newBuf;
        }
    }

    /**
     * Builder used to create {@link SmfWriter}.
     */
    public static final class Builder implements SmithyBuilder<SmfWriter> {
        private Predicate<String> metadataFilter = FunctionalUtils.alwaysTrue();
        private Predicate<Shape> shapeFilter = FunctionalUtils.alwaysTrue();
        private Predicate<Trait> traitFilter = FunctionalUtils.alwaysTrue();
        private Predicate<Trait> coldTraitFilter = FunctionalUtils.alwaysFalse();
        private boolean includePrelude = false;
        private boolean splitColdTraits = false;

        private Builder() {}

        /**
         * Predicate that determines if a metadata key is serialized.
         *
         * @param metadataFilter Predicate that accepts a metadata key.
         * @return Returns the builder.
         */
        public Builder metadataFilter(Predicate<String> metadataFilter) {
            this.metadataFilter = Objects.requireNonNull(metadataFilter);
            return this;
        }

        /**
         * Predicate that determines if a shape is serialized.
         *
         * @param shapeFilter Predicate that accepts a shape.
         * @return Returns the builder.
         */
        public Builder shapeFilter(Predicate<Shape> shapeFilter) {
            this.shapeFilter = Objects.requireNonNull(shapeFilter);
            return this;
        }

        /**
         * Sets a predicate that filters trait instances from serialized shapes.
         *
         * <p>Note that this does not filter out trait definitions. It only filters
         * out instances of traits from being serialized on shapes.
         *
         * @param traitFilter Predicate that filters trait instances.
         * @return Returns the builder.
         */
        public Builder traitFilter(Predicate<Trait> traitFilter) {
            this.traitFilter = Objects.requireNonNull(traitFilter);
            return this;
        }

        /**
         * Sets a predicate that classifies serialized traits as cold traits.
         *
         * <p>Cold traits are written into a separate trait-value table when
         * {@link #splitColdTraits(boolean)} is enabled.
         *
         * @param coldTraitFilter Predicate that identifies cold traits.
         * @return Returns the builder.
         */
        public Builder coldTraitFilter(Predicate<Trait> coldTraitFilter) {
            this.coldTraitFilter = Objects.requireNonNull(coldTraitFilter);
            return this;
        }

        /**
         * Enables or disables including the prelude in the serialized model.
         *
         * <p>By default, the prelude is not included.
         *
         * @param includePrelude Whether the prelude should be included.
         * @return Returns the builder.
         */
        public Builder includePrelude(boolean includePrelude) {
            this.includePrelude = includePrelude;
            return this;
        }

        /**
         * Enables writing hot and cold trait values into separate tables.
         *
         * @param splitColdTraits Whether cold traits should be separated.
         * @return Returns the builder.
         */
        public Builder splitColdTraits(boolean splitColdTraits) {
            this.splitColdTraits = splitColdTraits;
            return this;
        }

        @Override
        public SmfWriter build() {
            return new SmfWriter(this);
        }
    }
}
