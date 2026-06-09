$version: "2.1"

namespace smithy.example

// Inline collections are capped at 3 levels of nesting.
// [[[String]]] is valid (depth 3), but [[[[String]]]] (depth 4) is an error.
structure MyStructure {
    deep: [[[[String]]]]
}
