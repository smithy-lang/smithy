$version: "2.0"

namespace smithy.example

// This is impossible because there is no non-recursive branch along
// the recursive path back to itself.
union RecursiveUnionA {
    b: RecursiveUnionB
}

union RecursiveUnionB {
    a: RecursiveUnionA
}

// These unions are fine because there is a non-recursive branch along the
// recursive path back to itself:
// 1. {"e": {"str": "hi"}}
// 2. {"d": {"c": {"e": {"str": "hi"}}}}
union RecursiveUnionC {
    d: RecursiveUnionD,
    e: OkUnion
}

union RecursiveUnionD {
    c: RecursiveUnionC
}

union OkUnion {
    str: String
}

// This is fine too because on the of the recursive branches contains a list
// which gives the type a size.
union RecursiveUnionE {
     f: RecursiveUnionF
}

union RecursiveUnionF {
    e: EList
}

list EList {
    member: RecursiveUnionE
}

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

// This structure is invalid and the union is invalid
structure Start {
    @required
    foo: RecursiveNext
}

union RecursiveNext {
    a: Start,
    b: Start
}
