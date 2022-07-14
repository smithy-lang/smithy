$version: "2.0"

namespace ns.foo

/// This trait isn't an annotation trait since it doesn't extend BooleanTrait
@trait
boolean FalseBooleanTrait

@FalseBooleanTrait(true)
@private
string Foo
