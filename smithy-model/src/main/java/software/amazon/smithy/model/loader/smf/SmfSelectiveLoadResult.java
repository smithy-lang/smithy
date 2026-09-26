/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Result of a selective SMF load with profiling metadata.
 */
@SmithyUnstableApi
public final class SmfSelectiveLoadResult {

    private final Model model;
    private final SmfSelectiveLoadProfile profile;

    SmfSelectiveLoadResult(Model model, SmfSelectiveLoadProfile profile) {
        this.model = model;
        this.profile = profile;
    }

    public Model getModel() {
        return model;
    }

    public SmfSelectiveLoadProfile getProfile() {
        return profile;
    }
}
