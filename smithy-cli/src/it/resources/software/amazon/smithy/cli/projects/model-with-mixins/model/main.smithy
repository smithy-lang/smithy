$version: "2.0"

namespace com.amazon.example

@mixin
@tags(["a"])
structure A1 {
    @private
    a: String
}

@mixin
@documentation("Structure A2 docs")
structure A2 {
    @required
    a: String
}

structure Valid with [A1, A2] {
   c: Integer
}
