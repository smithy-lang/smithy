$version: "2.0"

metadata suppressions = [
    {
        id: "ResourceOperationInputOutput",
        namespace: "com.example"
    }
]

namespace com.example

resource Resource1 {
    identifiers: {
        id: String
    }
    properties: {
        property: String
    }
    update: UpdateResource
}

service Service {
    resources: [Resource1]
}

operation UpdateResource {
    input: ResourceStructure_1
    output: ResourceStructure_2
}

structure ResourceStructure_1 {
    property: String

    @idempotencyToken
    token: String

    @required
    id: String

    @notProperty
    spurious: String
}

structure ResourceStructure_2 {
    @nestedProperties
    nested: String
}

