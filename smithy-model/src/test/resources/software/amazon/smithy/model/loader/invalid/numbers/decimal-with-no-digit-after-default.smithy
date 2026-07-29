// Invalid number '1.': '.' must be followed by a digit
$version: "2.1"

namespace smithy.example

structure Invalid {
    value: Integer = 1.
}
