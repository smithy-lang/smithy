$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnResource
use aws.iam#iamAction

service TestService {
    version: "2020-07-02",
    resources: [
        FooResource,
        BarResource
        BazResource
    ],
}

/// The Foo resource is cool.
@externalDocumentation(
    "Documentation Url": "https://docs.example.com",
    "Source Url": "https://source.example.com",
    "Main": "https://docs2.example.com",
    "Code": "https://source2.example.com",
)
@cfnResource
resource FooResource {
    identifiers: {
        fooId: FooId,
    },
    create: CreateFooOperation,
    read: GetFooOperation,
    update: UpdateFooOperation,
}

@aws.iam#requiredActions(["otherservice:DescribeDependencyComponent"])
operation CreateFooOperation {
    input: CreateFooRequest,
    output: CreateFooResponse,
}

structure CreateFooRequest {
    fooValidCreateProperty: String,

    @required
    fooRequiredProperty: String,

    @deprecated(message: "Use the `fooValidFullyMutableProperty` property.")
    fooDeprecatedMutableProperty: String,
    fooValidFullyMutableProperty: ComplexProperty,
}

structure CreateFooResponse {
    fooId: FooId,

    fooRequiredProperty: String,

    @deprecated(message: "Use the `fooValidFullyMutableProperty` property.")
    fooDeprecatedMutableProperty: String,
    fooValidFullyMutableProperty: ComplexProperty,
}

@readonly
@iamAction(requiredActions: ["otherservice:DescribeThing"])
operation GetFooOperation {
    input: GetFooRequest,
    output: GetFooResponse,
}

structure GetFooRequest {
    @required
    fooId: FooId,
}

structure GetFooResponse {
    fooId: FooId,

    fooRequiredProperty: String,
    fooValidReadProperty: String,

    @deprecated(message: "Use the `fooValidFullyMutableProperty` property.")
    fooDeprecatedMutableProperty: String,

    fooValidFullyMutableProperty: ComplexProperty,
}

operation UpdateFooOperation {
    input: UpdateFooRequest,
    output: UpdateFooResponse,
}

structure UpdateFooRequest {
    @required
    fooId: FooId,

    fooRequiredProperty: String,
    fooValidWriteProperty: String,

    @deprecated(message: "Use the `fooValidFullyMutableProperty` property.")
    fooDeprecatedMutableProperty: String,

    fooValidFullyMutableProperty: ComplexProperty,
}

structure UpdateFooResponse {
    fooId: FooId,

    fooValidReadProperty: String,

    @deprecated(message: "Use the `fooValidFullyMutableProperty` property.")
    fooDeprecatedMutableProperty: String,

    fooValidFullyMutableProperty: ComplexProperty,
}

string FooId

structure ComplexProperty {
    property: String,
    another: String,
}

/// The Bar resource is cooler.
@externalDocumentation(
    "Documentation Url": "https://docs.example.com",
    "Source Url": "https://source.example.com",
    "Main": "https://docs2.example.com",
    "Code": "https://source2.example.com",
)
@cfnResource
resource BarResource {
    identifiers: {
        barId: String
    }
    put: CreateBar
}

@idempotent
operation CreateBar {
    input := for BarResource {
        @required
        $barId
    }
}

/// The Baz resource is irreplaceable.
@externalDocumentation(
    "Documentation Url": "https://docs.example.com",
    "Source Url": "https://source.example.com",
    "Main": "https://docs2.example.com",
    "Code": "https://source2.example.com",
)
@cfnResource
@noReplace
resource BazResource {
    identifiers: {
        bazId: String
    }
    put: CreateBaz
}

@idempotent
operation CreateBaz {
    input := for BazResource {
        @required
        $bazId
    }
}
