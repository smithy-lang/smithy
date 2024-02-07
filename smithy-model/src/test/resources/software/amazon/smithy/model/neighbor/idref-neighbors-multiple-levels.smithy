$version: "2.0"

namespace com.foo

service FooService {
    version: "2024-01-22"
    operations: [GetFoo]
}

operation GetFoo {
    input := {
        withIdRef: WithIdRef
    }
}

@trait
@idRef(failWhenMissing: true)
string ref

@ref(Referenced)
structure WithIdRef {}

structure Referenced {
    connectedThroughReferenced: ConnectedThroughReferenced
}

// Only connected through `Referenced`, which itself is only
// connected via idRef.
@ref(AnotherReferenced)
structure ConnectedThroughReferenced {}

string AnotherReferenced
