$version: "2.0"

namespace com.example

@mixin
structure Common {
    @required
    description: String
}

structure Thing with [Common] {}
