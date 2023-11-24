$version: "2.0"

namespace com.amazon.example

@mixin
structure A1 {
    @private
    a: String
}

@mixin
structure A2 {
    @required
    a: String
}

// a member is defined in the structure and it will be overridden by
// mixins A1 and A2
structure Valid with [A2 A1] {
    a: String
    b: String
}
