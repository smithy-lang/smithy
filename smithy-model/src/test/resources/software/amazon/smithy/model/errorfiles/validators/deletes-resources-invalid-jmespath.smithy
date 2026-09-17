$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Thing {
    identifiers: { thingId: String }
}

@deletesResources([
    {
        resource: "com.example#Thing"
        identifiers: { thingId: { path: "[invalid..path" } }
    }
])
operation DeleteThing {
    input: DeleteThingInput
    output: DeleteThingOutput
}

@input
structure DeleteThingInput {
    thingId: String
}

structure DeleteThingOutput {}
