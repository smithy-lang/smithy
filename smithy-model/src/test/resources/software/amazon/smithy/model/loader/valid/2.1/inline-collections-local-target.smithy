$version: "2.1"

namespace smithy.example

// Unqualified inline targets resolve like every other member target: imports,
// then the current namespace, then the prelude. Here `Widget` is a sibling in the
// current namespace (declared after its use, so this also covers forward
// references), and `String` is shadowed by a local shape, so it must resolve to
// smithy.example#String rather than the prelude smithy.api#String.
structure MyStructure {
    widgets: [Widget]
    strings: [String]
}

structure Widget {}

string String
