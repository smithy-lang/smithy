$version: "2.0"

namespace ns.foo

service EmptyService {
    version: "2020-02-18"
}

service MyService {
    version: "2020-02-18"
    operations: [
        MyOperation
    ]
    resources: [
        MyResource
    ]
}

resource EmptyResource {
}

resource MyResource {
    identifiers: {
        id: String
    }
    put: ResourceOperation
    create: EmptyOperation
    read: ReadonlyResourceOperation
    update: ResourceOperation
    delete: ResourceOperation
    list: CollectionResourceOperation
    operations: [
        CollectionResourceOperation
    ]
    collectionOperations: [
        CollectionResourceOperation
    ]
    resources: [
        SubResource
    ]
    properties: {
        value: String
        other: String
    }
}

resource SubResource {
    identifiers: {
        id: String
    }
}

@readonly
operation CollectionResourceOperation {
    input := {}
    output := {}
    errors: [
        Error
    ]
}

operation EmptyOperation {
    input: Unit
    output: Unit
}

operation MyOperation {
    input := {}
    output := {}
    errors: [
        Error
    ]
}

@readonly
operation ReadonlyResourceOperation {
    input := {
        @required
        id: String
    }
    output: Unit
}

@idempotent
operation ResourceOperation {
    input := {
        @required
        id: String
    }
    output := {
        value: String
        other: String
    }
}

@error("client")
structure Error {}
