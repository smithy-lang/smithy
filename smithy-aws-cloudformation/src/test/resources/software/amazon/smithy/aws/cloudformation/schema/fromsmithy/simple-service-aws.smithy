$version: "1.0"

namespace smithy.example

use aws.api#service
use aws.cloudformation#cfnResource

@service(sdkId: "Some Thing", cloudFormationName: "SomeThing")
service TestService {
    version: "2020-07-02",
    resources: [
        FooResource,
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

operation CreateFooOperation {
    input: CreateFooRequest,
    output: CreateFooResponse,
}

structure CreateFooRequest {
    fooValidCreateProperty: String,

    @deprecated(message: "Use the `fooValidFullyMutableProperty` property.")
    fooDeprecatedMutableProperty: String,
    fooValidFullyMutableProperty: ComplexProperty,
}

structure CreateFooResponse {
    fooId: FooId,

    @deprecated(message: "Use the `fooValidFullyMutableProperty` property.")
    fooDeprecatedMutableProperty: String,
    fooValidFullyMutableProperty: ComplexProperty,
}

@readonly
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
