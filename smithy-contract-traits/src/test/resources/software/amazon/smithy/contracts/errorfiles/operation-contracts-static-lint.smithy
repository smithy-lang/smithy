$version: "2"

namespace smithy.example

use smithy.contracts#contracts

// Exercises the static linting in ContractsTraitValidator: an expression that does not
// return a boolean, and an expression with a malformed function call. Both are caught
// against the operation call's static type, before any example is evaluated.
@contracts({
    NotBoolean: {
        documentation: "The expression returns a string, not a boolean"
        expression: "input.name"
    }
    BadFunctionCall: {
        documentation: "resource is called with the wrong number of arguments"
        expression: "resource(before)"
    }
    UnknownMember: {
        documentation: "the expression references a member the input does not have"
        expression: "input.nope == 'x'"
    }
})
operation DoThing {
    input: DoThingInput
    output: DoThingOutput
}

@input
structure DoThingInput {
    @required
    name: String
}

@output
structure DoThingOutput {}
