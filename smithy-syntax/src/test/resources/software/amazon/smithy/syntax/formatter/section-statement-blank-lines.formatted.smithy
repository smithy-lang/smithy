// Blank lines are added before and after control, metadata, and use statements if the statement contains any
// newline.
$version: "2.0"
$foo: 100
$baz: { abc: 123 }

$bar: {
    aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"
    aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2"
}

metadata a = "a"
metadata b = "b"

// A comment makes a blank line here, and beneath too.
metadata c = "c"

metadata d = "d"

metadata e = {
    aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"
    aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2"
}

metadata f = 100

namespace smithy.example

use smithy.api#Boolean

// This comment separates this use statement from it's leading and trailing lines.
use smithy.api#Integer

// This comment separates this use statement from it's leading and trailing lines.
// It does not add an trailing line though since it's the last statement in the section.
use smithy.api#String

structure Shape {
    a: String
    b: Boolean
    c: Integer
}
