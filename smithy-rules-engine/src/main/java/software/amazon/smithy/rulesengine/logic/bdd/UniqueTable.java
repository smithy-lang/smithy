/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.HashMap;
import java.util.Map;

/**
 * A specialized hash table for BDD node deduplication using triple (var, high, low) keys.
 */
final class UniqueTable {
    private final Map<TripleKey, Integer> table;
    private final TripleKey mutableKey = new TripleKey(0, 0, 0);

    public UniqueTable() {
        this.table = new HashMap<>();
    }

    public UniqueTable(int initialCapacity) {
        this.table = new HashMap<>(initialCapacity);
    }

    public Integer get(int var, int high, int low) {
        mutableKey.update(var, high, low);
        return table.get(mutableKey);
    }

    public void put(int var, int high, int low, int nodeIndex) {
        table.put(new TripleKey(var, high, low), nodeIndex);
    }

    public void clear() {
        table.clear();
    }

    public int size() {
        return table.size();
    }

    private static final class TripleKey {
        private int a, b, c, hash;

        private TripleKey(int a, int b, int c) {
            update(a, b, c);
        }

        TripleKey update(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
            int i = (a * 31 + b) * 31 + c;
            this.hash = (i ^ (i >>> 16));
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof TripleKey)) {
                return false;
            }
            TripleKey k = (TripleKey) o;
            return a == k.a && b == k.b && c == k.c;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
