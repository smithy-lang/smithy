$version: "2"

namespace smithy.example

@idempotent(
    exists: [ConflictException]
    notFound: [NotFoundException]
)
operation PutItem {
    input: PutItemInput
    output: PutItemOutput
    errors: [
        ConflictException
        NotFoundException
    ]
}

@input
structure PutItemInput {}

@output
structure PutItemOutput {}

@error("client")
structure ConflictException {}

@error("client")
structure NotFoundException {}
