$version: "2.0"

namespace test.smithy.traitcodegen.enums

// ========================
//  Legacy String Enum test
// ========================
// The following trait check that the plugin can generate traits from a
// legacy string enum (i.e. a string with the @enum trait applied).
@enum([
    {
        name: "DIAMOND"
        value: "diamond"
    }
    {
        name: "CLUB"
        value: "club"
    }
    {
        name: "HEART"
        value: "heart"
    }
    {
        name: "SPADE"
        value: "spade"
    }
])
@trait
string Suit
