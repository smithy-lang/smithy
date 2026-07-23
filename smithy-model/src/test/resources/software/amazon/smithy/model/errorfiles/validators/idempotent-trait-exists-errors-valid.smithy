$version: "2"

namespace smithy.example

@idempotent(exists: ["smithy.example#ConflictException"])
operation CreateFunction {
    input: CreateFunctionInput
    output: CreateFunctionOutput
    errors: [ConflictException, ValidationException]
}

@input
structure CreateFunctionInput {}

@output
structure CreateFunctionOutput {}

@error("client")
structure ConflictException {}

@error("client")
structure ValidationException {}
