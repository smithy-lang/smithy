$version: "2.0"

metadata a = [ // a
]
metadata b = [
] // b
metadata c = [
    // c
    100
]
metadata d = { // d
}
metadata e = {} // e
metadata f = {
    f // f
    : 100
}
metadata g = {
    g: // g
    100
}
metadata h = {
    h: 100 // h
}
metadata i = {
    i: 100 // i1
    // i2
}
metadata j = {
    j1: 100 // j1a
    // j1b
    j2: 100 // j2a
    // j2b
}
metadata k = {
    k1: // k1a
    100 // k1b
    k2: 100 // k2a
    // k2b
}

// na
namespace smithy.example // nb
// nc

use smithy.api#Integer // i
// l1
use smithy.api#Long // l2
// l3
use smithy.api#String // s

/// Docs 1
/// Docs 2
@required // trailing trait comment
@length( // Leading
    min
    // Pre-colon
    : // Post colon 1
    // Post-colon 2
    1 // post value
    // Pre-close
) // trailing
string MyString // Trailing comment
/// Docs for next

integer MyInteger

@tags(// a
    [ // b
    "foo" // c
    ] // d
) // e
integer MyInteger2

// Hello 1
// Hello 2
structure MyStruct // a
{ // b
    // c
    a: String // d
    // e
    // f
    b: String // g
    // h
} // i
// j

structure MyStructure2 { //k
    // l
    i: Integer
    l: Long // m
} // n
// o
