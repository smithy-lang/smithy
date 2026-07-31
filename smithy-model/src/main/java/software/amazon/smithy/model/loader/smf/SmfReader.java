/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Reads SMF binary format and produces a Smithy Model.
 */
@SmithyUnstableApi
public final class SmfReader {

    private static final int MAX_NESTING_DEPTH = 256;
    private static final int CRC_SIZE = 4;

    private final byte[] buf;
    private final int limit;
    private int pos;
    private String[] symbols;
    private ShapeId[] shapeIds;
    // Lazy symbol support: store (offset << 32 | length) for local symbols
    private long[] symbolOffsets;
    private int sharedSize;
    private Node[] traitValues;
    private final long[] varIntOut = new long[2];
    private final Map<ShapeId, List<ShapeId>> pendingMixins = new LinkedHashMap<>();
    private final TraitFactory traitFactory;
    private final boolean verifyCrc;

    private SmfReader(byte[] buf, TraitFactory traitFactory, boolean verifyCrc) {
        this.buf = buf;
        this.limit = buf.length - CRC_SIZE;
        this.pos = 0;
        this.traitFactory = traitFactory;
        this.verifyCrc = verifyCrc;
    }

    /**
     * Reads SMF bytes and returns a Model.
     */
    public static Model read(byte[] data) {
        return read(data, true);
    }

    /**
     * Reads SMF bytes and returns a Model, optionally skipping CRC verification.
     */
    public static Model read(byte[] data, boolean verifyCrc) {
        return new SmfReader(data, TraitFactory.createServiceFactory(), verifyCrc).deserialize();
    }

    /**
     * Reads SMF bytes and returns a Model using the given TraitFactory.
     */
    public static Model read(byte[] data, TraitFactory traitFactory) {
        return new SmfReader(data, traitFactory, true).deserialize();
    }

    /**
     * Callback interface for streaming SMF loading without building an
     * intermediate Model.
     */
    public interface LoadHandler {
        void modelVersion();

        void metadata(String key, Node value);

        void defineShape(AbstractShapeBuilder<?, ?> builder, List<MemberShape.Builder> members);
    }

    /**
     * Reads SMF bytes and emits loading events to the handler without
     * building an intermediate Model. This is the fast path for integration
     * with the ModelAssembler pipeline.
     */
    public static void readInto(byte[] data, LoadHandler handler) {
        readInto(data, handler, true);
    }

    /**
     * Reads SMF bytes and emits loading events, optionally skipping CRC verification.
     */
    public static void readInto(byte[] data, LoadHandler handler, boolean verifyCrc) {
        new SmfReader(data, TraitFactory.createServiceFactory(), verifyCrc).deserializeStreaming(handler);
    }

    /**
     * Reads SMF bytes and returns a Model containing the service shape (with
     * only its common errors expanded) and the specified operations with their
     * full transitive closure.
     *
     * <p>This is the primary entry point for dynamic client selective loading.
     *
     * @param data SMF bytes.
     * @param request The selective load request specifying service, operations, and options.
     * @return A Model containing the service, requested operations, and their closures.
     */
    public static Model readSelective(byte[] data, SelectiveLoadRequest request) {
        ClassLoader cl = request.getClassLoader();
        TraitFactory tf = cl != null
                ? TraitFactory.createServiceFactory(cl)
                : TraitFactory.createServiceFactory();
        return new SmfReader(data, tf, request.getVerifyCrc())
                .deserializeSelectiveForService(request);
    }

    private Model deserialize() {
        if (verifyCrc) {
            verifyCrc();
        }
        readHeader();
        readSymbolTable();
        readTraitValueTable();
        skipShapeIndex();
        Model.Builder builder = Model.builder();
        readMetadata(builder);
        readShapes(builder);

        // Build an intermediate model without mixins
        Model withoutMixins = builder.build();

        // Second pass: rebuild shapes that have mixins, now that all shapes exist
        if (!pendingMixins.isEmpty()) {
            Model.Builder finalBuilder = withoutMixins.toBuilder();
            for (Map.Entry<ShapeId, List<ShapeId>> entry : pendingMixins.entrySet()) {
                Shape shape = withoutMixins.expectShape(entry.getKey());
                @SuppressWarnings("unchecked")
                AbstractShapeBuilder<?, Shape> shapeBuilder =
                        Shape.shapeToBuilder(shape);
                for (ShapeId mixinId : entry.getValue()) {
                    Shape mixinShape = withoutMixins.getShape(mixinId).orElse(null);
                    if (mixinShape != null) {
                        shapeBuilder.addMixin(mixinShape);
                    }
                }
                finalBuilder.addShape(shapeBuilder.build());
            }
            return finalBuilder.build();
        }

        return withoutMixins;
    }

    private void deserializeStreaming(LoadHandler handler) {
        if (verifyCrc) {
            verifyCrc();
        }
        readHeader();
        readSymbolTable();
        readTraitValueTable();
        skipShapeIndex();

        handler.modelVersion();

        // Metadata
        int flags = buf[SmfConstants.OFFSET_FLAGS] & 0xFF;
        if ((flags & SmfConstants.FLAG_HAS_METADATA) != 0) {
            int metaCount = readVarUInt();
            for (int i = 0; i < metaCount; i++) {
                String key = symbolAt(readVarUInt());
                Node value = readDynamicValue();
                handler.metadata(key, value);
            }
        }

        // Shapes
        int shapeCount = readVarUInt();
        for (int i = 0; i < shapeCount; i++) {
            readShapeStreaming(handler);
        }
    }

    private void readShapeStreaming(LoadHandler handler) {
        ShapeId id = shapeIdAt(readVarUInt());
        int byteLength = readVarUInt();
        int endPos = pos + byteLength;

        int shapeType = buf[pos++] & 0xFF;

        // Read traits and attach directly to the shape builder
        int traitCount = readVarUInt();
        List<Trait> shapeTraits = new ArrayList<>(traitCount);
        for (int t = 0; t < traitCount; t++) {
            ShapeId traitId = shapeIdAt(readVarUInt());
            Node traitValue = traitValueAt(readVarUInt());
            Trait trait = traitFactory.createTrait(traitId, id, traitValue)
                    .orElseGet(() -> new DynamicTrait(traitId, traitValue));
            shapeTraits.add(trait);
        }

        // Build shape and emit DefineShape with traits already attached
        List<MemberShape.Builder> members = new ArrayList<>();
        AbstractShapeBuilder<?, ?> builder = buildShapeStreaming(id, shapeType, members);
        if (builder != null) {
            for (Trait trait : shapeTraits) {
                builder.addTrait(trait);
            }
            handler.defineShape(builder, members);
        }

        pos = endPos;
    }

    private AbstractShapeBuilder<?, ?> buildShapeStreaming(
            ShapeId id,
            int shapeType,
            List<MemberShape.Builder> members
    ) {
        switch (shapeType) {
            case SmfConstants.SHAPE_BLOB:
                return simpleBuilder(BlobShape.builder(), id);
            case SmfConstants.SHAPE_BOOLEAN:
                return simpleBuilder(BooleanShape.builder(), id);
            case SmfConstants.SHAPE_STRING:
                return simpleBuilder(StringShape.builder(), id);
            case SmfConstants.SHAPE_BYTE:
                return simpleBuilder(ByteShape.builder(), id);
            case SmfConstants.SHAPE_SHORT:
                return simpleBuilder(ShortShape.builder(), id);
            case SmfConstants.SHAPE_INTEGER:
                return simpleBuilder(IntegerShape.builder(), id);
            case SmfConstants.SHAPE_LONG:
                return simpleBuilder(LongShape.builder(), id);
            case SmfConstants.SHAPE_FLOAT:
                return simpleBuilder(FloatShape.builder(), id);
            case SmfConstants.SHAPE_DOUBLE:
                return simpleBuilder(DoubleShape.builder(), id);
            case SmfConstants.SHAPE_BIG_DECIMAL:
                return simpleBuilder(BigDecimalShape.builder(), id);
            case SmfConstants.SHAPE_BIG_INTEGER:
                return simpleBuilder(BigIntegerShape.builder(), id);
            case SmfConstants.SHAPE_TIMESTAMP:
                return simpleBuilder(TimestampShape.builder(), id);
            case SmfConstants.SHAPE_DOCUMENT:
                return simpleBuilder(DocumentShape.builder(), id);
            case SmfConstants.SHAPE_ENUM:
                return namedMemberBuilder(EnumShape.builder(), id, members);
            case SmfConstants.SHAPE_INT_ENUM:
                return namedMemberBuilder(IntEnumShape.builder(), id, members);
            case SmfConstants.SHAPE_LIST:
                return collectionBuilder(ListShape.builder(), id, members);
            case SmfConstants.SHAPE_MAP:
                return mapBuilder(id, members);
            case SmfConstants.SHAPE_STRUCTURE:
                return namedMemberBuilder(StructureShape.builder(), id, members);
            case SmfConstants.SHAPE_UNION:
                return namedMemberBuilder(UnionShape.builder(), id, members);
            case SmfConstants.SHAPE_OPERATION:
                return operationBuilder(id);
            case SmfConstants.SHAPE_RESOURCE:
                return resourceBuilder(id);
            case SmfConstants.SHAPE_SERVICE:
                return serviceBuilder(id);
            default:
                throw new SmfFormatException("Unknown shape type: 0x" + Integer.toHexString(shapeType));
        }
    }

    private AbstractShapeBuilder<?, ?> simpleBuilder(AbstractShapeBuilder<?, ?> builder, ShapeId id) {
        builder.id(id);
        skipMixins();
        return builder;
    }

    private AbstractShapeBuilder<?, ?> namedMemberBuilder(
            AbstractShapeBuilder<?, ?> builder,
            ShapeId id,
            List<MemberShape.Builder> members
    ) {
        builder.id(id);
        int memberCount = readVarUInt();
        for (int i = 0; i < memberCount; i++) {
            String memberName = symbolAt(readVarUInt());
            ShapeId target = shapeIdAt(readVarUInt());
            MemberShape.Builder mb = MemberShape.builder()
                    .id(id.withMember(memberName))
                    .target(target);
            int memberTraitCount = readVarUInt();
            for (int t = 0; t < memberTraitCount; t++) {
                ShapeId traitId = shapeIdAt(readVarUInt());
                Node traitValue = traitValueAt(readVarUInt());
                Trait trait = traitFactory.createTrait(traitId, id.withMember(memberName), traitValue)
                        .orElseGet(() -> new DynamicTrait(traitId, traitValue));
                mb.addTrait(trait);
            }
            members.add(mb);
        }
        skipMixins();
        return builder;
    }

    private AbstractShapeBuilder<?, ?> collectionBuilder(
            CollectionShape.Builder<?, ?> builder,
            ShapeId id,
            List<MemberShape.Builder> members
    ) {
        builder.id(id);
        ShapeId memberTarget = shapeIdAt(readVarUInt());
        MemberShape.Builder mb = MemberShape.builder()
                .id(id.withMember("member"))
                .target(memberTarget);
        int memberTraitCount = readVarUInt();
        for (int t = 0; t < memberTraitCount; t++) {
            ShapeId traitId = shapeIdAt(readVarUInt());
            Node traitValue = traitValueAt(readVarUInt());
            Trait trait = traitFactory.createTrait(traitId, id.withMember("member"), traitValue)
                    .orElseGet(() -> new DynamicTrait(traitId, traitValue));
            mb.addTrait(trait);
        }
        members.add(mb);
        skipMixins();
        return builder;
    }

    private AbstractShapeBuilder<?, ?> mapBuilder(ShapeId id, List<MemberShape.Builder> members) {
        MapShape.Builder builder = MapShape.builder().id(id);
        MemberShape.Builder keyMb = MemberShape.builder().id(id.withMember("key")).target(shapeIdAt(readVarUInt()));
        int keyTraitCount = readVarUInt();
        for (int t = 0; t < keyTraitCount; t++) {
            ShapeId traitId = shapeIdAt(readVarUInt());
            Node traitValue = traitValueAt(readVarUInt());
            keyMb.addTrait(traitFactory.createTrait(traitId, id.withMember("key"), traitValue)
                    .orElseGet(() -> new DynamicTrait(traitId, traitValue)));
        }
        members.add(keyMb);

        MemberShape.Builder valMb = MemberShape.builder().id(id.withMember("value")).target(shapeIdAt(readVarUInt()));
        int valTraitCount = readVarUInt();
        for (int t = 0; t < valTraitCount; t++) {
            ShapeId traitId = shapeIdAt(readVarUInt());
            Node traitValue = traitValueAt(readVarUInt());
            valMb.addTrait(traitFactory.createTrait(traitId, id.withMember("value"), traitValue)
                    .orElseGet(() -> new DynamicTrait(traitId, traitValue)));
        }
        members.add(valMb);
        skipMixins();
        return builder;
    }

    private AbstractShapeBuilder<?, ?> operationBuilder(ShapeId id) {
        OperationShape.Builder builder = OperationShape.builder().id(id);
        int flags = buf[pos++] & 0xFF;
        if ((flags & SmfConstants.OP_HAS_INPUT) != 0) {
            builder.input(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.OP_HAS_OUTPUT) != 0) {
            builder.output(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.OP_HAS_ERRORS) != 0) {
            int errorCount = readVarUInt();
            for (int i = 0; i < errorCount; i++) {
                builder.addError(shapeIdAt(readVarUInt()));
            }
        }
        skipMixins();
        return builder;
    }

    private AbstractShapeBuilder<?, ?> resourceBuilder(ShapeId id) {
        ResourceShape.Builder builder = ResourceShape.builder().id(id);
        int flags = buf[pos++] & 0xFF;
        if ((flags & SmfConstants.RES_HAS_PUT) != 0) {
            builder.put(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_CREATE) != 0) {
            builder.create(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_READ) != 0) {
            builder.read(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_UPDATE) != 0) {
            builder.update(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_DELETE) != 0) {
            builder.delete(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_LIST) != 0) {
            builder.list(shapeIdAt(readVarUInt()));
        }
        int idCount = readVarUInt();
        for (int i = 0; i < idCount; i++) {
            builder.addIdentifier(symbolAt(readVarUInt()), shapeIdAt(readVarUInt()));
        }
        int propCount = readVarUInt();
        for (int i = 0; i < propCount; i++) {
            builder.addProperty(symbolAt(readVarUInt()), shapeIdAt(readVarUInt()));
        }
        int opCount = readVarUInt();
        for (int i = 0; i < opCount; i++) {
            builder.addOperation(shapeIdAt(readVarUInt()));
        }
        int collOpCount = readVarUInt();
        for (int i = 0; i < collOpCount; i++) {
            builder.addCollectionOperation(shapeIdAt(readVarUInt()));
        }
        int resCount = readVarUInt();
        for (int i = 0; i < resCount; i++) {
            builder.addResource(shapeIdAt(readVarUInt()));
        }
        skipMixins();
        return builder;
    }

    private AbstractShapeBuilder<?, ?> serviceBuilder(ShapeId id) {
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id);
        builder.version(readString());
        int opCount = readVarUInt();
        for (int i = 0; i < opCount; i++) {
            builder.addOperation(shapeIdAt(readVarUInt()));
        }
        int resCount = readVarUInt();
        for (int i = 0; i < resCount; i++) {
            builder.addResource(shapeIdAt(readVarUInt()));
        }
        int errCount = readVarUInt();
        for (int i = 0; i < errCount; i++) {
            builder.addError(shapeIdAt(readVarUInt()));
        }
        int renameCount = readVarUInt();
        for (int i = 0; i < renameCount; i++) {
            builder.putRename(shapeIdAt(readVarUInt()), readString());
        }
        skipMixins();
        return builder;
    }

    private void skipMixins() {
        int count = readVarUInt();
        for (int i = 0; i < count; i++) {
            readVarUInt();
        }
    }

    private Model deserializeSelectiveForService(SelectiveLoadRequest request) {
        if (verifyCrc) {
            verifyCrc();
        }
        readHeader();
        readSymbolTable();
        readTraitValueTableLazy();

        // Resolve root symrefs (only these need ShapeId.from)
        int serviceSym = findSymRef(request.getService());
        int[] opSyms = new int[request.getOperations().size()];
        int opIdx = 0;
        for (ShapeId opId : request.getOperations()) {
            opSyms[opIdx++] = findSymRef(opId);
        }

        // Read fixed-size index: entryCount + totalNeighbors, then table + neighbors
        int entryCount = readVarUInt();
        int totalNeighborCount = readVarUInt();
        int tableStart = pos;
        int neighborsArrayStart = tableStart + entryCount * SmfConstants.INDEX_ENTRY_SIZE;
        int indexEnd = neighborsArrayStart + totalNeighborCount * 4;

        // Compute closure using binary search on the sorted fixed-size table.
        // No need to build any intermediate arrays or hashmaps.
        Set<Integer> closureSyms = new LinkedHashSet<>();
        Queue<Integer> queue = new ArrayDeque<>();

        closureSyms.add(serviceSym);

        // Service neighbors: skip operations and resources
        int svcIdx = binarySearchIndex(tableStart, entryCount, serviceSym);
        if (svcIdx >= 0) {
            int entryPos = tableStart + svcIdx * SmfConstants.INDEX_ENTRY_SIZE;
            int nStart = readInt32LE(entryPos + 9);
            int nCount = readInt16LE(entryPos + 13);
            for (int n = 0; n < nCount; n++) {
                int neighborSym = readInt32LE(neighborsArrayStart + (nStart + n) * 4);
                int neighborIdx = binarySearchIndex(tableStart, entryCount, neighborSym);
                if (neighborIdx < 0) {
                    continue;
                }
                byte neighborType = buf[tableStart + neighborIdx * SmfConstants.INDEX_ENTRY_SIZE + 4];
                if (neighborType == SmfConstants.SHAPE_OPERATION
                        || neighborType == SmfConstants.SHAPE_RESOURCE) {
                    continue;
                }
                if (closureSyms.add(neighborSym)) {
                    queue.add(neighborSym);
                }
            }
        }

        // Add requested operations
        for (int opSym : opSyms) {
            if (closureSyms.add(opSym)) {
                queue.add(opSym);
            }
        }

        // Expand transitive closure via binary search
        while (!queue.isEmpty()) {
            int sym = queue.poll();
            int idx = binarySearchIndex(tableStart, entryCount, sym);
            if (idx < 0) {
                continue;
            }
            int entryPos = tableStart + idx * SmfConstants.INDEX_ENTRY_SIZE;
            int nStart = readInt32LE(entryPos + 9);
            int nCount = readInt16LE(entryPos + 13);
            for (int n = 0; n < nCount; n++) {
                int neighborSym = readInt32LE(neighborsArrayStart + (nStart + n) * 4);
                if (closureSyms.add(neighborSym)) {
                    queue.add(neighborSym);
                }
            }
        }

        // Collect offsets for shapes in closure
        List<int[]> toLoad = new ArrayList<>(); // [offset, symref]
        for (int sym : closureSyms) {
            int idx = binarySearchIndex(tableStart, entryCount, sym);
            if (idx >= 0) {
                int entryPos = tableStart + idx * SmfConstants.INDEX_ENTRY_SIZE;
                toLoad.add(new int[] {readInt32LE(entryPos + 5), sym});
            }
        }
        toLoad.sort((a, b) -> Integer.compare(a[0], b[0]));

        // Skip metadata to find shapes section start
        pos = indexEnd;
        int flags = buf[SmfConstants.OFFSET_FLAGS] & 0xFF;
        if ((flags & SmfConstants.FLAG_HAS_METADATA) != 0) {
            skipMetadata();
        }

        // Skip shapeCount VarUInt to get dataStart
        readVarUInt(); // shapeCount (not needed — we know which shapes to load)
        int dataStart = pos;

        // Direct seeks: jump to each shape by offset, no scanning
        Model.Builder builder = Model.builder();
        for (int[] entry : toLoad) {
            pos = dataStart + entry[0];
            readVarUInt(); // shapeId symref (skip — we already have it from index)
            int byteLength = readVarUInt();
            int endPos = pos + byteLength;
            int shapeType = buf[pos++] & 0xFF;
            ShapeId shapeId = shapeIdAt(entry[1]);
            List<Trait> traits = readTraits(shapeId);
            Shape shape = buildShape(shapeId, shapeType, traits);
            if (shape != null) {
                builder.addShape(shape);
            }
            pos = endPos;
        }

        // Second pass: load trait definition shapes referenced by loaded shapes.
        // Iterate until no new trait definitions are discovered.
        Set<Integer> loadedSyms = new LinkedHashSet<>(closureSyms);
        List<int[]> traitShapesToLoad = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            traitShapesToLoad.clear();
            for (Shape shape : builder.getCurrentShapes().values()) {
                for (ShapeId traitId : shape.getAllTraits().keySet()) {
                    int traitSym = findSymRef(traitId);
                    if (traitSym > 0 && loadedSyms.add(traitSym)) {
                        int idx = binarySearchIndex(tableStart, entryCount, traitSym);
                        if (idx >= 0) {
                            int entryPos = tableStart + idx * SmfConstants.INDEX_ENTRY_SIZE;
                            traitShapesToLoad.add(new int[] {readInt32LE(entryPos + 5), traitSym});
                            changed = true;
                        }
                    }
                }
                for (MemberShape member : shape.members()) {
                    for (ShapeId traitId : member.getAllTraits().keySet()) {
                        int traitSym = findSymRef(traitId);
                        if (traitSym > 0 && loadedSyms.add(traitSym)) {
                            int idx = binarySearchIndex(tableStart, entryCount, traitSym);
                            if (idx >= 0) {
                                int entryPos = tableStart + idx * SmfConstants.INDEX_ENTRY_SIZE;
                                traitShapesToLoad.add(new int[] {readInt32LE(entryPos + 5), traitSym});
                                changed = true;
                            }
                        }
                    }
                }
            }
            traitShapesToLoad.sort((a, b) -> Integer.compare(a[0], b[0]));
            for (int[] entry : traitShapesToLoad) {
                pos = dataStart + entry[0];
                readVarUInt();
                int byteLength = readVarUInt();
                int endPos = pos + byteLength;
                int shapeType = buf[pos++] & 0xFF;
                ShapeId shapeId = shapeIdAt(entry[1]);
                List<Trait> traits = readTraits(shapeId);
                Shape shape = buildShape(shapeId, shapeType, traits);
                if (shape != null) {
                    builder.addShape(shape);
                }
                pos = endPos;
            }
        }

        return builder.build();
    }

    /**
     * Find the symref for a ShapeId by scanning symbols.
     */
    private int findSymRef(ShapeId id) {
        String target = id.toString();
        for (int i = 1; i < symbols.length; i++) {
            if (target.equals(symbolAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private int readInt32LE(int p) {
        return (buf[p] & 0xFF) | ((buf[p + 1] & 0xFF) << 8)
                | ((buf[p + 2] & 0xFF) << 16)
                | ((buf[p + 3] & 0xFF) << 24);
    }

    private int readInt16LE(int p) {
        return (buf[p] & 0xFF) | ((buf[p + 1] & 0xFF) << 8);
    }

    /**
     * Binary search the fixed-size index table for a symref.
     * Returns the entry index or -1 if not found.
     */
    private int binarySearchIndex(int tableStart, int entryCount, int symref) {
        int lo = 0;
        int hi = entryCount - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midSym = readInt32LE(tableStart + mid * SmfConstants.INDEX_ENTRY_SIZE);
            if (midSym < symref) {
                lo = mid + 1;
            } else if (midSym > symref) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    private void skipMetadata() {
        int count = readVarUInt();
        for (int i = 0; i < count; i++) {
            readVarUInt(); // key symref
            skipDynamicValue();
        }
    }

    private void skipDynamicValue() {
        require(1);
        int tag = buf[pos++] & 0xFF;
        switch (tag) {
            case SmfConstants.VALUE_NULL:
            case SmfConstants.VALUE_FALSE:
            case SmfConstants.VALUE_TRUE:
            case SmfConstants.VALUE_EMPTY_OBJECT:
                break;
            case SmfConstants.VALUE_INTEGER:
                readVarIntLong();
                break;
            case SmfConstants.VALUE_DOUBLE:
                pos += 8;
                break;
            case SmfConstants.VALUE_STRING:
            case SmfConstants.VALUE_BIG_INTEGER:
            case SmfConstants.VALUE_BIG_DECIMAL: {
                int len = readVarUInt();
                pos += len;
                break;
            }
            case SmfConstants.VALUE_LIST: {
                int cnt = readVarUInt();
                for (int i = 0; i < cnt; i++) {
                    skipDynamicValue();
                }
                break;
            }
            case SmfConstants.VALUE_OBJECT: {
                int cnt = readVarUInt();
                for (int i = 0; i < cnt; i++) {
                    int keyLen = readVarUInt();
                    pos += keyLen;
                    skipDynamicValue();
                }
                break;
            }
            default:
                throw new SmfFormatException("Unknown value tag in skip: 0x"
                        + Integer.toHexString(tag));
        }
    }

    private void verifyCrc() {
        if (buf.length < SmfConstants.HEADER_SIZE + CRC_SIZE) {
            throw new SmfFormatException("File too short for SMF header and CRC");
        }
        int expected = (buf[limit] & 0xFF)
                | ((buf[limit + 1] & 0xFF) << 8)
                | ((buf[limit + 2] & 0xFF) << 16)
                | ((buf[limit + 3] & 0xFF) << 24);
        int actual = Crc32C.compute(buf, 0, limit);
        if (actual != expected) {
            throw new SmfFormatException("CRC-32C mismatch: file is corrupted");
        }
    }

    private void readHeader() {
        if (buf.length < SmfConstants.HEADER_SIZE) {
            throw new SmfFormatException("File too short for SMF header");
        }
        int magic = ((buf[0] & 0xFF) << 24) | ((buf[1] & 0xFF) << 16)
                | ((buf[2] & 0xFF) << 8)
                | (buf[3] & 0xFF);
        if (magic != SmfConstants.MAGIC) {
            throw new SmfFormatException("Invalid SMF magic number");
        }
        if (buf[4] != SmfConstants.FORMAT_VERSION) {
            throw new SmfFormatException("Unsupported SMF format version: " + buf[4]);
        }
        // buf[5] = smithy major, buf[6] = smithy minor (available if needed)
        // buf[7] = flags
        pos = SmfConstants.HEADER_SIZE;
    }

    private void readSymbolTable() {
        int sharedTableId = readVarUInt();
        sharedSize = 0;
        if (sharedTableId != 0) {
            int sharedTableVersion = readVarUInt();
            if (sharedTableVersion > SmfSharedSymbols.VERSION) {
                throw new SmfFormatException(
                        "Unsupported shared symbol table version: " + sharedTableVersion
                                + " (reader supports up to version " + SmfSharedSymbols.VERSION + ")");
            }
            sharedSize = SmfSharedSymbols.size();
        }

        int localCount = readCount();
        int totalSize = 1 + sharedSize + localCount;
        symbols = new String[totalSize];
        shapeIds = new ShapeId[totalSize];
        symbolOffsets = new long[totalSize];

        // ID 0 = reserved
        symbols[0] = null;

        // Shared symbols (already materialized constants)
        for (int i = 0; i < sharedSize; i++) {
            symbols[i + 1] = SmfSharedSymbols.SYMBOLS[i];
        }

        // Local symbols: record byte offsets, defer String creation
        int localStart = 1 + sharedSize;
        for (int i = 0; i < localCount; i++) {
            int len = readVarUInt();
            symbolOffsets[localStart + i] = ((long) pos << 32) | (len & 0xFFFFFFFFL);
            pos += len;
        }

        // Pre-parse shared ShapeIds (they're already known strings)
        for (int i = 1; i <= sharedSize; i++) {
            String s = symbols[i];
            if (s.indexOf('#') > 0) {
                shapeIds[i] = ShapeId.from(s);
            }
        }
    }

    private int[] traitValueOffsets;

    private void readTraitValueTable() {
        int count = readVarUInt();
        traitValues = new Node[count];
        for (int i = 0; i < count; i++) {
            traitValues[i] = readDynamicValue();
        }
    }

    private void readTraitValueTableLazy() {
        int count = readVarUInt();
        traitValues = new Node[count];
        traitValueOffsets = new int[count];
        for (int i = 0; i < count; i++) {
            traitValueOffsets[i] = pos;
            skipDynamicValue();
        }
    }

    private Node traitValueAt(int ref) {
        Node v = traitValues[ref];
        if (v == null) {
            int savedPos = pos;
            pos = traitValueOffsets[ref];
            v = readDynamicValue();
            traitValues[ref] = v;
            pos = savedPos;
        }
        return v;
    }

    private void skipTraitValueTable() {
        int count = readVarUInt();
        for (int i = 0; i < count; i++) {
            skipDynamicValue();
        }
    }

    private void skipShapeIndex() {
        int flags = buf[SmfConstants.OFFSET_FLAGS] & 0xFF;
        if ((flags & SmfConstants.FLAG_HAS_SHAPE_INDEX) == 0) {
            return;
        }
        int entryCount = readVarUInt();
        int neighborCount = readVarUInt();
        pos += entryCount * SmfConstants.INDEX_ENTRY_SIZE + neighborCount * 4;
    }

    private void readMetadata(Model.Builder builder) {
        int flags = buf[SmfConstants.OFFSET_FLAGS] & 0xFF;
        if ((flags & SmfConstants.FLAG_HAS_METADATA) == 0) {
            return;
        }
        int count = readVarUInt();
        for (int i = 0; i < count; i++) {
            String key = symbolAt(readVarUInt());
            Node value = readDynamicValue();
            builder.putMetadataProperty(key, value);
        }
    }

    private void readShapes(Model.Builder builder) {
        int count = readCount();
        for (int i = 0; i < count; i++) {
            readShape(builder);
        }
    }

    private void readShape(Model.Builder builder) {
        ShapeId id = shapeIdAt(readVarUInt());
        int byteLength = readVarUInt();
        int endPos = pos + byteLength;

        int shapeType = buf[pos++] & 0xFF;
        List<Trait> traits = readTraits(id);
        Shape shape = buildShape(id, shapeType, traits);
        if (shape != null) {
            builder.addShape(shape);
        }

        // Ensure we consumed exactly byteLength bytes
        pos = endPos;
    }

    private List<Trait> readTraits(ShapeId target) {
        int count = readCount();
        List<Trait> traits = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ShapeId traitId = shapeIdAt(readVarUInt());
            int valueRef = readVarUInt();
            Node value = traitValueAt(valueRef);
            Trait trait = traitFactory.createTrait(traitId, target, value)
                    .orElseGet(() -> new DynamicTrait(traitId, value));
            traits.add(trait);
        }
        return traits;
    }

    private Shape buildShape(ShapeId id, int shapeType, List<Trait> traits) {
        switch (shapeType) {
            case SmfConstants.SHAPE_BLOB:
                return simpleShape(BlobShape.builder(), id, traits);
            case SmfConstants.SHAPE_BOOLEAN:
                return simpleShape(BooleanShape.builder(), id, traits);
            case SmfConstants.SHAPE_STRING:
                return simpleShape(StringShape.builder(), id, traits);
            case SmfConstants.SHAPE_BYTE:
                return simpleShape(ByteShape.builder(), id, traits);
            case SmfConstants.SHAPE_SHORT:
                return simpleShape(ShortShape.builder(), id, traits);
            case SmfConstants.SHAPE_INTEGER:
                return simpleShape(IntegerShape.builder(), id, traits);
            case SmfConstants.SHAPE_LONG:
                return simpleShape(LongShape.builder(), id, traits);
            case SmfConstants.SHAPE_FLOAT:
                return simpleShape(FloatShape.builder(), id, traits);
            case SmfConstants.SHAPE_DOUBLE:
                return simpleShape(DoubleShape.builder(), id, traits);
            case SmfConstants.SHAPE_BIG_DECIMAL:
                return simpleShape(BigDecimalShape.builder(), id, traits);
            case SmfConstants.SHAPE_BIG_INTEGER:
                return simpleShape(BigIntegerShape.builder(), id, traits);
            case SmfConstants.SHAPE_TIMESTAMP:
                return simpleShape(TimestampShape.builder(), id, traits);
            case SmfConstants.SHAPE_DOCUMENT:
                return simpleShape(DocumentShape.builder(), id, traits);
            case SmfConstants.SHAPE_ENUM:
                return namedMemberShape(EnumShape.builder(), id, traits);
            case SmfConstants.SHAPE_INT_ENUM:
                return namedMemberShape(IntEnumShape.builder(), id, traits);
            case SmfConstants.SHAPE_LIST:
                return collectionShape(ListShape.builder(), id, traits);
            case SmfConstants.SHAPE_MAP:
                return mapShape(id, traits);
            case SmfConstants.SHAPE_STRUCTURE:
                return namedMemberShape(StructureShape.builder(), id, traits);
            case SmfConstants.SHAPE_UNION:
                return namedMemberShape(UnionShape.builder(), id, traits);
            case SmfConstants.SHAPE_OPERATION:
                return operationShape(id, traits);
            case SmfConstants.SHAPE_RESOURCE:
                return resourceShape(id, traits);
            case SmfConstants.SHAPE_SERVICE:
                return serviceShape(id, traits);
            default:
                throw new SmfFormatException("Unknown shape type: 0x"
                        + Integer.toHexString(shapeType));
        }
    }

    // --- Shape builders ---

    private Shape simpleShape(
            AbstractShapeBuilder<?, ?> builder,
            ShapeId id,
            List<Trait> traits
    ) {
        builder.id(id);
        traits.forEach(builder::addTrait);
        readMixins(builder);
        return builder.build();
    }

    private Shape namedMemberShape(
            AbstractShapeBuilder<?, ?> builder,
            ShapeId id,
            List<Trait> traits
    ) {
        builder.id(id);
        traits.forEach(builder::addTrait);
        int memberCount = readVarUInt();
        for (int i = 0; i < memberCount; i++) {
            String memberName = symbolAt(readVarUInt());
            ShapeId target = shapeIdAt(readVarUInt());
            MemberShape.Builder mb = MemberShape.builder()
                    .id(id.withMember(memberName))
                    .target(target);
            List<Trait> memberTraits = readTraits(id.withMember(memberName));
            memberTraits.forEach(mb::addTrait);
            builder.addMember(mb.build());
        }
        readMixins(builder);
        return builder.build();
    }

    private Shape collectionShape(CollectionShape.Builder<?, ?> builder, ShapeId id, List<Trait> traits) {
        builder.id(id);
        traits.forEach(builder::addTrait);
        ShapeId memberTarget = shapeIdAt(readVarUInt());
        MemberShape.Builder mb = MemberShape.builder()
                .id(id.withMember("member"))
                .target(memberTarget);
        List<Trait> memberTraits = readTraits(id.withMember("member"));
        memberTraits.forEach(mb::addTrait);
        builder.member(mb.build());
        readMixins(builder);
        return builder.build();
    }

    private Shape mapShape(ShapeId id, List<Trait> traits) {
        MapShape.Builder builder = MapShape.builder().id(id);
        traits.forEach(builder::addTrait);

        ShapeId keyTarget = shapeIdAt(readVarUInt());
        MemberShape.Builder keyMb = MemberShape.builder()
                .id(id.withMember("key"))
                .target(keyTarget);
        readTraits(id.withMember("key")).forEach(keyMb::addTrait);
        builder.key(keyMb.build());

        ShapeId valueTarget = shapeIdAt(readVarUInt());
        MemberShape.Builder valueMb = MemberShape.builder()
                .id(id.withMember("value"))
                .target(valueTarget);
        readTraits(id.withMember("value")).forEach(valueMb::addTrait);
        builder.value(valueMb.build());

        readMixins(builder);
        return builder.build();
    }

    private Shape operationShape(ShapeId id, List<Trait> traits) {
        OperationShape.Builder builder = OperationShape.builder().id(id);
        traits.forEach(builder::addTrait);
        int flags = buf[pos++] & 0xFF;
        if ((flags & SmfConstants.OP_HAS_INPUT) != 0) {
            builder.input(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.OP_HAS_OUTPUT) != 0) {
            builder.output(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.OP_HAS_ERRORS) != 0) {
            int errorCount = readVarUInt();
            for (int i = 0; i < errorCount; i++) {
                builder.addError(shapeIdAt(readVarUInt()));
            }
        }
        readMixins(builder);
        return builder.build();
    }

    private Shape resourceShape(ShapeId id, List<Trait> traits) {
        ResourceShape.Builder builder = ResourceShape.builder().id(id);
        traits.forEach(builder::addTrait);
        int flags = buf[pos++] & 0xFF;
        if ((flags & SmfConstants.RES_HAS_PUT) != 0) {
            builder.put(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_CREATE) != 0) {
            builder.create(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_READ) != 0) {
            builder.read(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_UPDATE) != 0) {
            builder.update(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_DELETE) != 0) {
            builder.delete(shapeIdAt(readVarUInt()));
        }
        if ((flags & SmfConstants.RES_HAS_LIST) != 0) {
            builder.list(shapeIdAt(readVarUInt()));
        }

        int idCount = readVarUInt();
        for (int i = 0; i < idCount; i++) {
            builder.addIdentifier(symbolAt(readVarUInt()), shapeIdAt(readVarUInt()));
        }

        int propCount = readVarUInt();
        for (int i = 0; i < propCount; i++) {
            builder.addProperty(symbolAt(readVarUInt()), shapeIdAt(readVarUInt()));
        }

        int opCount = readVarUInt();
        for (int i = 0; i < opCount; i++) {
            builder.addOperation(shapeIdAt(readVarUInt()));
        }

        int collOpCount = readVarUInt();
        for (int i = 0; i < collOpCount; i++) {
            builder.addCollectionOperation(shapeIdAt(readVarUInt()));
        }

        int resCount = readVarUInt();
        for (int i = 0; i < resCount; i++) {
            builder.addResource(shapeIdAt(readVarUInt()));
        }

        readMixins(builder);
        return builder.build();
    }

    private Shape serviceShape(ShapeId id, List<Trait> traits) {
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id);
        traits.forEach(builder::addTrait);
        builder.version(readString());

        int opCount = readVarUInt();
        for (int i = 0; i < opCount; i++) {
            builder.addOperation(shapeIdAt(readVarUInt()));
        }

        int resCount = readVarUInt();
        for (int i = 0; i < resCount; i++) {
            builder.addResource(shapeIdAt(readVarUInt()));
        }

        int errCount = readVarUInt();
        for (int i = 0; i < errCount; i++) {
            builder.addError(shapeIdAt(readVarUInt()));
        }

        int renameCount = readVarUInt();
        for (int i = 0; i < renameCount; i++) {
            ShapeId from = shapeIdAt(readVarUInt());
            String to = readString();
            builder.putRename(from, to);
        }

        readMixins(builder);
        return builder.build();
    }

    private void readMixins(AbstractShapeBuilder<?, ?> builder) {
        int count = readVarUInt();
        if (count > 0) {
            List<ShapeId> mixinIds = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                mixinIds.add(shapeIdAt(readVarUInt()));
            }
            pendingMixins.put(builder.getId(), mixinIds);
        }
    }

    // --- Dynamic value reading ---

    private Node readDynamicValue() {
        return readDynamicValue(0);
    }

    private Node readDynamicValue(int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new SmfFormatException("Dynamic value nesting exceeds maximum depth of " + MAX_NESTING_DEPTH);
        }
        require(1);
        int tag = buf[pos++] & 0xFF;
        switch (tag) {
            case SmfConstants.VALUE_NULL:
                return Node.nullNode();
            case SmfConstants.VALUE_FALSE:
                return Node.from(false);
            case SmfConstants.VALUE_TRUE:
                return Node.from(true);
            case SmfConstants.VALUE_INTEGER:
                return Node.from(readVarIntLong());
            case SmfConstants.VALUE_DOUBLE:
                require(8);
                long bits = (buf[pos] & 0xFFL)
                        | ((buf[pos + 1] & 0xFFL) << 8)
                        | ((buf[pos + 2] & 0xFFL) << 16)
                        | ((buf[pos + 3] & 0xFFL) << 24)
                        | ((buf[pos + 4] & 0xFFL) << 32)
                        | ((buf[pos + 5] & 0xFFL) << 40)
                        | ((buf[pos + 6] & 0xFFL) << 48)
                        | ((buf[pos + 7] & 0xFFL) << 56);
                pos += 8;
                return Node.from(Double.longBitsToDouble(bits));
            case SmfConstants.VALUE_STRING:
                return Node.from(readString());
            case SmfConstants.VALUE_LIST: {
                int count = readCount();
                List<Node> elements = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    elements.add(readDynamicValue(depth + 1));
                }
                return Node.fromNodes(elements);
            }
            case SmfConstants.VALUE_OBJECT: {
                int count = readCount();
                ObjectNode.Builder ob = ObjectNode.builder();
                for (int i = 0; i < count; i++) {
                    String key = readString();
                    Node value = readDynamicValue(depth + 1);
                    ob.withMember(key, value);
                }
                return ob.build();
            }
            case SmfConstants.VALUE_EMPTY_OBJECT:
                return Node.objectNode();
            case SmfConstants.VALUE_BIG_INTEGER: {
                String str = readString();
                return Node.from(new BigInteger(str));
            }
            case SmfConstants.VALUE_BIG_DECIMAL: {
                String str = readString();
                return Node.from(new BigDecimal(str));
            }
            default:
                throw new SmfFormatException("Unknown dynamic value tag: 0x"
                        + Integer.toHexString(tag));
        }
    }

    // --- Primitives ---

    private int readCount() {
        int count = readVarUInt();
        if (count < 0 || count > limit - pos) {
            throw new SmfFormatException(
                    "Count " + count + " exceeds remaining buffer (" + (limit - pos) + " bytes)");
        }
        return count;
    }

    private void require(int n) {
        if (pos + n > limit) {
            throw new SmfFormatException(
                    "Unexpected end of data: need " + n + " bytes at position " + pos
                            + " but only " + (limit - pos) + " remain");
        }
    }

    // WARNING: This method advances `pos` as a side effect. Never use its
    // return value in an expression that also reads `pos` (e.g., `pos += readVarUInt()`
    // is WRONG because Java evaluates the old `pos` before the call mutates it).
    private int readVarUInt() {
        require(1);
        long packed = LEB128.readVarUInt(buf, pos);
        pos = LEB128.position(packed);
        return LEB128.value(packed);
    }

    private long readVarIntLong() {
        LEB128.readVarInt(buf, pos, varIntOut);
        pos = (int) varIntOut[1];
        return varIntOut[0];
    }

    private String readString() {
        int len = readVarUInt();
        require(len);
        String s = new String(buf, pos, len, StandardCharsets.UTF_8);
        pos += len;
        return s;
    }

    private String symbolAt(int id) {
        if (id <= 0 || id >= symbols.length) {
            throw new SmfFormatException("Invalid symbol ID: " + id);
        }
        String s = symbols[id];
        if (s == null) {
            long packed = symbolOffsets[id];
            int off = (int) (packed >>> 32);
            int len = (int) packed;
            s = new String(buf, off, len, java.nio.charset.StandardCharsets.UTF_8);
            symbols[id] = s;
        }
        return s;
    }

    private ShapeId shapeIdAt(int id) {
        if (id <= 0 || id >= shapeIds.length) {
            throw new SmfFormatException("Invalid shape ID reference: " + id);
        }
        ShapeId result = shapeIds[id];
        if (result == null) {
            String s = symbolAt(id);
            if (s.indexOf('#') <= 0) {
                throw new SmfFormatException("Symbol " + id + " ('" + s
                        + "') is not a valid shape ID");
            }
            result = ShapeId.from(s);
            shapeIds[id] = result;
        }
        return result;
    }
}
