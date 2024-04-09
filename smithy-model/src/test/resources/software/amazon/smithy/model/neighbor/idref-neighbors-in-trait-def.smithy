$version: "2.0"

namespace com.foo

service FooService {
    version: "2024-01-22"
    operations: [GetFoo]
}

operation GetFoo {
    input := {
        withRefStructTrait: WithRefStructTrait
    }
}

@trait
@idRef(failWhenMissing: true)
string ref

@trait
structure refStruct {
    @ref(ReferencedInTraitDef)
    other: String

    @idRef(failWhenMissing: true)
    ref: String
}

string ReferencedInTraitDef

@refStruct(other: "foo", ref: OtherReferenced)
structure WithRefStructTrait {}

string OtherReferenced
