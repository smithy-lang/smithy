/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Timing and size breakdown for {@link SmfReader#readSelectiveWithProfile(byte[], SelectiveLoadRequest)}.
 */
@SmithyUnstableApi
public final class SmfSelectiveLoadProfile {

    private final boolean crcVerified;
    private final int requestedOperationCount;
    private final int sharedSymbolCount;
    private final int localSymbolCount;
    private final int symbolCount;
    private final int traitValueCount;
    private final int traitValueBytesScanned;
    private final int traitValueDataBytes;
    private final int decodedTraitValueCount;
    private final int indexEntryCount;
    private final int neighborCount;
    private final int closureShapeCount;
    private final int initialShapeLoadCount;
    private final int traitDefinitionShapeCount;
    private final int finalIndexedShapeCount;
    private final int initialShapeBytesLoaded;
    private final int traitDefinitionShapeBytesLoaded;
    private final long totalNanos;
    private final long crcNanos;
    private final long headerNanos;
    private final long symbolTableNanos;
    private final long traitValueScanNanos;
    private final long rootResolutionNanos;
    private final long indexSetupNanos;
    private final long closureComputationNanos;
    private final long offsetCollectionNanos;
    private final long metadataSkipNanos;
    private final long shapeLoadNanos;
    private final long traitDefinitionLoadNanos;

    SmfSelectiveLoadProfile(
            boolean crcVerified,
            int requestedOperationCount,
            int sharedSymbolCount,
            int localSymbolCount,
            int symbolCount,
            int traitValueCount,
            int traitValueBytesScanned,
            int traitValueDataBytes,
            int decodedTraitValueCount,
            int indexEntryCount,
            int neighborCount,
            int closureShapeCount,
            int initialShapeLoadCount,
            int traitDefinitionShapeCount,
            int finalIndexedShapeCount,
            int initialShapeBytesLoaded,
            int traitDefinitionShapeBytesLoaded,
            long totalNanos,
            long crcNanos,
            long headerNanos,
            long symbolTableNanos,
            long traitValueScanNanos,
            long rootResolutionNanos,
            long indexSetupNanos,
            long closureComputationNanos,
            long offsetCollectionNanos,
            long metadataSkipNanos,
            long shapeLoadNanos,
            long traitDefinitionLoadNanos
    ) {
        this.crcVerified = crcVerified;
        this.requestedOperationCount = requestedOperationCount;
        this.sharedSymbolCount = sharedSymbolCount;
        this.localSymbolCount = localSymbolCount;
        this.symbolCount = symbolCount;
        this.traitValueCount = traitValueCount;
        this.traitValueBytesScanned = traitValueBytesScanned;
        this.traitValueDataBytes = traitValueDataBytes;
        this.decodedTraitValueCount = decodedTraitValueCount;
        this.indexEntryCount = indexEntryCount;
        this.neighborCount = neighborCount;
        this.closureShapeCount = closureShapeCount;
        this.initialShapeLoadCount = initialShapeLoadCount;
        this.traitDefinitionShapeCount = traitDefinitionShapeCount;
        this.finalIndexedShapeCount = finalIndexedShapeCount;
        this.initialShapeBytesLoaded = initialShapeBytesLoaded;
        this.traitDefinitionShapeBytesLoaded = traitDefinitionShapeBytesLoaded;
        this.totalNanos = totalNanos;
        this.crcNanos = crcNanos;
        this.headerNanos = headerNanos;
        this.symbolTableNanos = symbolTableNanos;
        this.traitValueScanNanos = traitValueScanNanos;
        this.rootResolutionNanos = rootResolutionNanos;
        this.indexSetupNanos = indexSetupNanos;
        this.closureComputationNanos = closureComputationNanos;
        this.offsetCollectionNanos = offsetCollectionNanos;
        this.metadataSkipNanos = metadataSkipNanos;
        this.shapeLoadNanos = shapeLoadNanos;
        this.traitDefinitionLoadNanos = traitDefinitionLoadNanos;
    }

    public boolean isCrcVerified() {
        return crcVerified;
    }

    public int getRequestedOperationCount() {
        return requestedOperationCount;
    }

    public int getSharedSymbolCount() {
        return sharedSymbolCount;
    }

    public int getLocalSymbolCount() {
        return localSymbolCount;
    }

    public int getSymbolCount() {
        return symbolCount;
    }

    public int getTraitValueCount() {
        return traitValueCount;
    }

    public int getTraitValueBytesScanned() {
        return traitValueBytesScanned;
    }

    public int getTraitValueDataBytes() {
        return traitValueDataBytes;
    }

    public int getDecodedTraitValueCount() {
        return decodedTraitValueCount;
    }

    public int getIndexEntryCount() {
        return indexEntryCount;
    }

    public int getNeighborCount() {
        return neighborCount;
    }

    public int getClosureShapeCount() {
        return closureShapeCount;
    }

    public int getInitialShapeLoadCount() {
        return initialShapeLoadCount;
    }

    public int getTraitDefinitionShapeCount() {
        return traitDefinitionShapeCount;
    }

    public int getFinalIndexedShapeCount() {
        return finalIndexedShapeCount;
    }

    public int getInitialShapeBytesLoaded() {
        return initialShapeBytesLoaded;
    }

    public int getTraitDefinitionShapeBytesLoaded() {
        return traitDefinitionShapeBytesLoaded;
    }

    public long getTotalNanos() {
        return totalNanos;
    }

    public long getCrcNanos() {
        return crcNanos;
    }

    public long getHeaderNanos() {
        return headerNanos;
    }

    public long getSymbolTableNanos() {
        return symbolTableNanos;
    }

    public long getTraitValueScanNanos() {
        return traitValueScanNanos;
    }

    public long getRootResolutionNanos() {
        return rootResolutionNanos;
    }

    public long getIndexSetupNanos() {
        return indexSetupNanos;
    }

    public long getClosureComputationNanos() {
        return closureComputationNanos;
    }

    public long getOffsetCollectionNanos() {
        return offsetCollectionNanos;
    }

    public long getMetadataSkipNanos() {
        return metadataSkipNanos;
    }

    public long getShapeLoadNanos() {
        return shapeLoadNanos;
    }

    public long getTraitDefinitionLoadNanos() {
        return traitDefinitionLoadNanos;
    }
}
