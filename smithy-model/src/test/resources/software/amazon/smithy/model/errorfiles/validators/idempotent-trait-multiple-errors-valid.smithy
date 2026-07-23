$version: "2"

namespace smithy.example

@idempotent(exists: ["smithy.example#ConflictException", "smithy.example#AlreadyExistsException"])
operation CreateThing {
    input: CreateThingInput
    output: CreateThingOutput
    errors: [ConflictException, AlreadyExistsException, ValidationException]
}

@input
structure CreateThingInput {}

@output
structure CreateThingOutput {}

@error("client")
structure ConflictException {}

@error("client")
structure AlreadyExistsException {}

@error("client")
structure ValidationException {}
