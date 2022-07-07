$version: "1.0"

namespace smithy.example

use aws.cloudformation#cfnRootResourceId

/// The Foo resource is cool.
resource FooResource {
    identifiers: {
        fooId: String,
    },
    create: CreateFooOperation,
}

operation CreateFooOperation {
    input: CreateFooOperationInput,
    output: CreateFooOperationOutput
}

@input
structure CreateFooOperationInput {
    @cfnRootResourceId
    fooId: String,
}

@output
structure CreateFooOperationOutput {
    fooId: String,
}



