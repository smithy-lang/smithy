// Member conflicts with an inherited mixin member: `com.amazon.example#Valid$a
$version: "2.0"

namespace com.amazon.example

@mixin
structure A1 {
    a: String
}

@mixin
structure A2 {
    a: Integer
}

structure Valid with [A1 A2] {
    c: Integer
}
