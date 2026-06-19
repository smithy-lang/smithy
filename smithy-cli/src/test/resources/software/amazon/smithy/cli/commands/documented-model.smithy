$version: "2.0"

namespace smithy.example

@documentation("Service-level documentation.")
service DocService {
    version: "2024-01-01"
    operations: [DocOp]
}

@documentation("Operation-level documentation that should be stripped by --no-docs.")
operation DocOp {
    input: DocInput
    output: DocOutput
}

structure DocInput {
    @required
    @documentation("Member documentation that should be stripped by --no-docs.")
    name: String
}

structure DocOutput {}
