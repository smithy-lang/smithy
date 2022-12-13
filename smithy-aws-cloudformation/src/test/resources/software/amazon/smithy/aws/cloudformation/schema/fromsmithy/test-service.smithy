$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnAdditionalIdentifier
use aws.cloudformation#cfnResource
use aws.cloudformation#cfnExcludeProperty
use aws.cloudformation#cfnMutability

service TestService {
    version: "2020-07-02",
    resources: [
        FooResource,
        BarResource,
    ],
}

/// The Foo resource is cool.
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
    fooValidCreateProperty: FooMap,

    fooValidCreateReadProperty: String,

    fooValidFullyMutableProperty: ComplexProperty,

    conditionalProperty: ConditionalProperty,
}

structure CreateFooResponse {
    fooId: FooId,
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

    fooValidCreateReadProperty: String,

    fooValidFullyMutableProperty: ComplexProperty,

    conditionalProperty: ConditionalProperty,
}

operation UpdateFooOperation {
    input: UpdateFooRequest,
    output: UpdateFooResponse,
}

structure UpdateFooRequest {
    @required
    fooId: FooId,

    @cfnMutability("write")
    fooValidWriteProperty: String,

    fooValidFullyMutableProperty: ComplexProperty,

    conditionalProperty: ConditionalProperty,
}

structure UpdateFooResponse {
    fooId: FooId,

    fooValidReadProperty: String,

    fooValidFullyMutableProperty: ComplexProperty,

    conditionalProperty: ConditionalProperty,
}

/// A Bar resource, not that kind of bar though.
@cfnResource(name: "Bar", additionalSchemas: [ExtraBarRequest])
resource BarResource {
    identifiers: {
        barId: BarId,
    },
    put: PutBarOperation,
    read: GetBarOperation,
    operations: [ExtraBarOperation],
    resources: [BazResource],
}

@idempotent
@aws.iam#requiredActions(["otherservice:DescribeDependencyComponent"])
operation PutBarOperation {
    input: PutBarRequest,
}

structure PutBarRequest {
    @required
    barId: BarId,

    barImplicitFullProperty: String,
}

@readonly
operation GetBarOperation {
    input: GetBarRequest,
    output: GetBarResponse,
}

structure GetBarRequest {
    @required
    barId: BarId,

    @cfnAdditionalIdentifier
    arn: String,
}

structure GetBarResponse {
    barId: BarId,
    barImplicitReadProperty: String,
    barImplicitFullProperty: String,

    @cfnMutability("full")
    barExplicitMutableProperty: String,
}

operation ExtraBarOperation {
    input: ExtraBarRequest,
}

structure ExtraBarRequest {
    @required
    barId: BarId,

    barValidAdditionalProperty: String,

    @cfnExcludeProperty
    barValidExcludedProperty: String,
}

/// This is an herb.
@cfnResource("name": "Basil")
resource BazResource {
    identifiers: {
        barId: BarId,
        bazId: BazId,
    },
    create: CreateBazOperation,
    read: GetBazOperation,
    update: UpdateBazOperation,
}

operation CreateBazOperation {
    input: CreateBazRequest,
    output: CreateBazResponse,
}

structure CreateBazRequest {
    @required
    barId: BarId,

    bazExplicitMutableProperty: String,
    bazImplicitCreateProperty: String,
    bazImplicitFullyMutableProperty: String,
    bazImplicitWriteProperty: String,
}

structure CreateBazResponse {
    barId: BarId,
    bazId: BazId,
}

@readonly
operation GetBazOperation {
    input: GetBazRequest,
    output: GetBazResponse,
}

structure GetBazRequest {
    @required
    barId: BarId,

    @required
    bazId: BazId,
}

structure GetBazResponse {
    barId: BarId,
    bazId: BazId,

    @cfnMutability("full")
    bazExplicitMutableProperty: String,
    bazImplicitCreateProperty: String,
    bazImplicitReadProperty: String,
    bazImplicitFullyMutableProperty: String,
}

operation UpdateBazOperation {
    input: UpdateBazRequest,
    output: UpdateBazResponse,
}

structure UpdateBazRequest {
    @required
    barId: BarId,

    @required
    bazId: BazId,

    bazImplicitWriteProperty: String,
    bazImplicitFullyMutableProperty: String,
}

structure UpdateBazResponse {
    barId: BarId,
    bazId: BazId,
    bazImplicitWriteProperty: String,
    bazImplicitFullyMutableProperty: String,
}

string FooId

string BarId

string BazId

structure ComplexProperty {
    property: String,
    another: String,
}

union ConditionalProperty {
    optionOne: String,
    optionTwo: String,
}

map FooMap {
    key: String,
    value: String
}
