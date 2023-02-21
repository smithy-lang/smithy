$version: "1.0"

namespace smithy.example

@enum([
    { name: "DIAMOND", value: "diamond"},
    { name: "CLUB", value: "club"},
    { name: "HEART", value: "heart"},
    { name: "SPADE", value: "spade"},
])
string Suit

@enum([
    { name: "RED", value: "red"}
    { name: "BLACK", value: "black"}
])
string Color
