/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

/**
 * Consumer that receives every node in a {@link Bdd}.
 */
public interface BddNodeConsumer {
    /**
     * Receives a BDD node.
     *
     * @param var  Variable.
     * @param high High reference.
     * @param low  Low reference.
     */
    void accept(int var, int high, int low);
}
