$version: "2.1"

namespace smithy.example

// Inline collections can be used in union member target positions.
union MyUnion {
    names: [String]
    tags: {String: Integer}
}
