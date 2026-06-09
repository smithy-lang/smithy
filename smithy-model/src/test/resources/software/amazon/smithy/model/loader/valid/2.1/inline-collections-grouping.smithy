$version: "2.1"

namespace smithy.example

// Multiple members using the same inline collection type share a single
// synthetic shape. Both `names` and `tags` target the same _SyntheticListOfString.
structure First {
    names: [String]
}

structure Second {
    tags: [String]
}
