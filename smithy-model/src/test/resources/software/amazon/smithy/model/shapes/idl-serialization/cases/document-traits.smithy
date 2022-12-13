$version: "2.0"

namespace ns.foo

@trait
document DocumentTrait

@DocumentTrait(false)
string Boolean

@DocumentTrait([
    "foo"
])
string List

@DocumentTrait(
    foo: "bar"
)
string Map

@DocumentTrait(null)
string Null

@DocumentTrait(123)
string Number

@DocumentTrait("foo")
string String
