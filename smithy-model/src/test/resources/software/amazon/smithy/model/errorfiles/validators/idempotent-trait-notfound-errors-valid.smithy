$version: "2"

namespace smithy.example

@idempotent(notFound: ["smithy.example#ResourceNotFoundException"])
operation DeleteThing {
    input: DeleteThingInput
    output: DeleteThingOutput
    errors: [ResourceNotFoundException]
}

@input
structure DeleteThingInput {}

@output
structure DeleteThingOutput {}

@error("client")
structure ResourceNotFoundException {}
