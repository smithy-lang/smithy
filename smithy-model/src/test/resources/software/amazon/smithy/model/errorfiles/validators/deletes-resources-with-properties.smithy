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
        resource: Thing
        properties: { foo: { path: "foo" } }
    }
])
operation DeleteThing {
    input: DeleteThingInput
    output: DeleteThingOutput
}

@input
structure DeleteThingInput {
    thingId: String
    foo: String
}

structure DeleteThingOutput {}

@deletesResources([
    {
        resource: Thing
        propertiesFrom: "spec"
    }
])
operation DeleteThingFrom {
    input: DeleteThingFromInput
}

@input
structure DeleteThingFromInput {
    thingId: String
    spec: ThingSpec
}

structure ThingSpec {
    foo: String
}
