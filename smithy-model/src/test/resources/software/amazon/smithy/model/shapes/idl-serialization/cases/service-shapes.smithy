$version: "1.1"

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

operation EmptyOperation {}

operation MyOperation {
    input: InputOutput
    output: InputOutput
    errors: [
        Error
    ]
}

@readonly
operation ReadonlyResourceOperation {
    input: ResourceOperationInput
}

@idempotent
operation ResourceOperation {
    input: ResourceOperationInput
}

@error("client")
structure Error {}

structure InputOutput {}

structure ResourceOperationInput {
    id: String
}
