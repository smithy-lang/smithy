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
    create: ResourceOperation
    read: ReadonlyResourceOperation
    update: ResourceOperation
    delete: ResourceOperation
    list: ReadonlyResourceOperation
    operations: [
        ResourceOperation
    ]
    collectionOperations: [
        ResourceOperation
    ]
    resources: [
        SubResource
    ]
}

resource SubResource {
    identifiers: {
        id: String
    }
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
        id: String
    }
    output: Unit
}

@idempotent
operation ResourceOperation {
    input := {
        id: String
    }
    output: Unit
}

@error("client")
structure Error {}
