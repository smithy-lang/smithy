$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnResource

@cfnResource
resource FooResource {
    identifiers: {
        fooId: FooId
    }
}

@cfnResource(
    name: "CustomResource",
    additionalSchemas: [ExtraBarRequest]
)
resource BarResource {
    identifiers: {
        barId: BarId
    },
    operations: [ExtraBarOperation],
}

operation ExtraBarOperation {
    input: ExtraBarRequest,
}

structure ExtraBarRequest {
    @required
    barId: BarId,
}

string FooId

string BarId
