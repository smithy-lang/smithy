// Member conflicts with an inherited mixin member: `com.amazon.example#A2$a`
$version: "2.0"

namespace com.amazon.example

@mixin
structure A1 {
    a: Integer
}

@mixin
structure A2 {
    a: String
}

structure Valid with [A1 A2] {
    a: Integer
    c: Integer
}
