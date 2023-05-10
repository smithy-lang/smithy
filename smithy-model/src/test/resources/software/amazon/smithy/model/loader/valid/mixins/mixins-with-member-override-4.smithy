$version: "2.0"

namespace com.amazon.example

@mixin
structure A1 {
    @private
    @tags(["a"])
    a: String
}

@mixin
structure A2 {
    @required
    @tags(["b"])
    a: String
}

structure Valid with [A1 A2] {
    a: String
    c: Integer
}
