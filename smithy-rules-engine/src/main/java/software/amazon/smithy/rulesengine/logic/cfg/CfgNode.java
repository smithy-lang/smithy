/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

/**
 * Abstract base class for all nodes in a Control Flow Graph (CFG).
 */
public abstract class CfgNode {
    // Package-private "sealed" class.
    CfgNode() {}
}
