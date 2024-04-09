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

// Member `a` is defined in the structure with a @tags trait that MUST
// not be overridden by inherited mixins.
structure Valid with [A1 A2] {
    @tags(["c"])
    a: String
    c: Integer
}
