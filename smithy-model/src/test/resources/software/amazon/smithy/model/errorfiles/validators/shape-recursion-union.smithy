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

// An optional member doesn't make this structure terminate, because the
// required member still targets the union, so the union is impossible.
union RecursiveThroughOptional {
    a: HasOptionalMember
}

structure HasOptionalMember {
    @required
    union: RecursiveThroughOptional

    optional: String
}

// This union is impossible because one required member of the structure
// leads back to the union, even though the other required member terminates.
union RecursiveThroughRequired {
    a: HasTerminatingRequiredMember
}

structure HasTerminatingRequiredMember {
    @required
    union: RecursiveThroughRequired

    @required
    str: String
}

// This union is fine because every required member of the structure terminates
// and the recursive member is optional: {"a": {"str": "hi"}}
union RecursiveThroughOptionalOnly {
    a: HasOptionalRecursiveMember
}

structure HasOptionalRecursiveMember {
    union: RecursiveThroughOptionalOnly

    @required
    str: String
}

// This union is impossible because a structure needs all of its required
// members, and only one of the two required members can be built.
union RecursiveThroughSecondRequired {
    a: HasTwoRequiredTargets
}

structure HasTwoRequiredTargets {
    @required
    empty: EmptyStructure

    @required
    union: RecursiveThroughSecondRequired
}

structure EmptyStructure {}

// This union is fine because its only member targets a structure whose two
// required members target the same buildable shape: {"a": {"x": {}, "y": {}}}
union BuildableThroughSameTargetTwice {
    a: HasSameRequiredTargetTwice
}

structure HasSameRequiredTargetTwice {
    @required
    x: EmptyStructure

    @required
    y: EmptyStructure
}
