$version: "2"

namespace smithy.example

@idempotent(
    exists: [NotOnOperationException]
)
operation CreateThing {
    input: CreateThingInput
    output: CreateThingOutput
    errors: [
        ConflictException
    ]
}

@input
structure CreateThingInput {}

@output
structure CreateThingOutput {}

@error("client")
structure ConflictException {}

@error("client")
structure NotOnOperationException {}
