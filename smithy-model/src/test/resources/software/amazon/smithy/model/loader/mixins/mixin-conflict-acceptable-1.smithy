$version: "1.1"

namespace smithy.example

@mixin
structure A with B {
    a: String
}

@mixin
structure B {
    b: String
}
