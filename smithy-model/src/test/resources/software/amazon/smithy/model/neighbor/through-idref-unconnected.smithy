$version: "2.0"

namespace com.foo

@trait
@idRef(failWhenMissing: true, selector: "*")
string unconnected

@unconnected(Referenced)
structure WithTrait {}

structure Referenced {}
