$version: "2.0"

namespace smithy.example

string MyString

structure MyFooIsBroken {
// The parser will keep trying to parse here, assuming integer is a key and needs to be followed by ":".
integer MyInteger

// When the above fails, error recovery kicks in, looking for the next token at the start of the line.
@unknown
integer MyInteger2
