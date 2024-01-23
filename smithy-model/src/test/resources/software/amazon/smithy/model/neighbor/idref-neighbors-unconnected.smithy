$version: "2.0"

namespace com.foo

service FooService {
    version: "2024-01-22"
    operations: [GetFoo]
}

operation GetFoo {
    input := {
        withReferencedByUnconnected: WithReferencedByUnconnected
    }
}

@trait
@idRef(failWhenMissing: true)
string ref

@ref(Referenced)
structure WithTrait {}

structure Referenced {}

@ref(ReferencedByUnconnected)
structure WithReferencedByUnconnected {}

string ReferencedByUnconnected

structure Unconnected {
    ref: ReferencedByUnconnected
}
