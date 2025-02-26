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
    name: "CustomResource"
    additionalSchemas: [ExtraBarRequest]
)
resource BarResource {
    identifiers: {
        barId: BarId
    }
    operations: [ExtraBarOperation]
}

@cfnResource(primaryIdentifier: "tadArn")
resource TadResource {
    identifiers: {
        tadId: String
    }
    properties: {
        tadArn: String
    }
    create: CreateTad
    read: GetTad
}

operation CreateTad {
    input := {}
    output := {
        @required
        tadId: String
        tadArn: String
    }
}

@readonly
operation GetTad {
    input := {
        @required
        tadId: String
    }
    output := {
        @required
        tadId: String
        tadArn: String
    }
}

operation ExtraBarOperation {
    input: ExtraBarRequest
}

structure ExtraBarRequest {
    @required
    barId: BarId
}

string FooId

string BarId
