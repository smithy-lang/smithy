/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates models implementing the {@code Rpcv2JsonTrait} against its constraints by:
 *
 * - Ensuring that every entry in {@code eventStreamHttp} also appears in the {@code http} property
 *   of a protocol trait.
 */
@SmithyInternalApi
public final class Rpcv2JsonTraitValidator extends Rpcv2ProtocolTraitValidator<Rpcv2JsonTrait> {

    public Rpcv2JsonTraitValidator() {
        super(Rpcv2JsonTrait.class);
    }
}
