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

@trait
structure annotationTrait {}

// This is used to test that trait locations are properly set for a variety of
// trait types. The locations for annotation traits in particular used to be
// fraught. To ensure there's no regressions, do not remove any traits from
// here. If a trait absolutely must be removed, replace it with a custom
// trait that looks exactly the same, like those above. At minimum there
// should be one trait for each Node type, including a case for every syntactic
// variation.

/// Documentation trait using the doc comment syntax
@annotationTrait     // Annotation trait without parens
@internal()          // Annotation trait with parens
@deprecated(         // Structured trait with no braces
    since: "1.0"
)
@tags(["foo"])       // List trait
@unstable({})        // Structured trait with braces
@stringTrait("foo")  // String trait using normal string syntax
@since("""
    0.9""")          // String trait using block syntax
@numberTrait(1)      // Number trait
@boolTrait(true)     // Boolean trait
@documentTrait(null) // Null value trait
structure TraitBearer {
    /// Documentation trait using the doc comment syntax
    @annotationTrait     // Annotation trait without parens
    @internal()          // Annotation trait with parens
    @deprecated(         // Structured trait with no braces
        since: "1.0"
    )
    @tags(["foo"])       // List trait
    @unstable({})        // Structured trait with braces
    @stringTrait("foo")  // String trait using normal string syntax
    @since("""
        0.9""")          // String trait using block syntax
    @numberTrait(1)      // Number trait
    @boolTrait(true)     // Boolean trait
    @documentTrait(null) // Null value trait
    member: String
}
