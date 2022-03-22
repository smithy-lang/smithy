$version: "1.0"

namespace com.example

@trait
integer numberTrait

@trait
boolean boolTrait

@trait
document documentTrait

@trait
string stringTrait

/// Documentation
@sensitive
@internal()
@deprecated(
    since: "1.0"
)
@tags(["foo"])
@unstable({})
@stringTrait("foo")
@since("""
    0.9""")
@numberTrait(1)
@boolTrait(true)
@documentTrait(null)
structure TraitBearer {
    /// Documentation
    @sensitive
    @internal()
    @deprecated(
        since: "1.0"
    )
    @tags(["foo"])
    @unstable({})
    @pattern("foo")
    @since("""
        0.9""")
    @numberTrait(1)
    @boolTrait(true)
    @documentTrait(null)
    member: String
}
