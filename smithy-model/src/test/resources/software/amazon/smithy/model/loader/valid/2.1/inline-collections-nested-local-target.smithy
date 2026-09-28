$version: "2.1"

namespace smithy.example

// Nested inline collections and map key/value targets resolve unqualified names
// against the current namespace too. `MyKey` and `MyValue` are local siblings, and
// the nested `[Widget]` element resolves to the local Widget.
structure MyStructure {
    entries: {MyKey: MyValue}
    nested: {String: [Widget]}
}

string MyKey

structure MyValue {}

structure Widget {}
