$version: "1.0"

namespace smithy.example

// It's impossible to provide a value for this union.
union RecursiveUnion {
    a: RecursiveUnion,
    b: RecursiveUnion,
}

union NotFullyRecursiveUnion {
    a: RecursiveUnion,
    b: RecursiveUnion,
    c: String
}
