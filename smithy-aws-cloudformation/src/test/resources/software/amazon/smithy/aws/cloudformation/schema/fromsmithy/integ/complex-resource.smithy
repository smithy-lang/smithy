namespace smithy.example

use aws.cloudformation#cfnAdditionalIdentifier
use aws.cloudformation#cfnExcludeProperty
use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02"
    resources: [
        Foo
    ]
}

/// Definition of Example::TestService::Foo Resource Type
@cfnResource(
    additionalSchemas: [FooProperties]
)
resource Foo {
    identifiers: { fooId: String }
    create: CreateFoo
    read: GetFoo
    update: UpdateFoo
}

@http(method: "POST", uri: "/foos", code: 200)
operation CreateFoo {
    input: CreateFooRequest
    output: CreateFooResponse
}

structure CreateFooRequest {
    @cfnMutability("full")
    tags: TagList

    @cfnMutability("write")
    secret: String

    fooAlias: String

    mutableProperty: ComplexProperty

    createProperty: ComplexProperty

    writeProperty: ComplexProperty

    createWriteProperty: ArbitraryMap
}

structure CreateFooResponse {
    fooId: String
}

@readonly
@http(method: "GET", uri: "/foos/{fooId}", code: 200)
operation GetFoo {
    input: GetFooRequest
    output: GetFooResponse
}

structure GetFooRequest {
    @httpLabel
    @required
    fooId: String

    @httpQuery("fooAlias")
    @cfnAdditionalIdentifier
    fooAlias: String
}

structure GetFooResponse {
    fooId: String

    @httpResponseCode
    @cfnExcludeProperty
    responseCode: Integer

    @cfnMutability("read")
    updatedAt: Timestamp

    mutableProperty: ComplexProperty

    createProperty: ComplexProperty

    readProperty: ComplexProperty
}

@idempotent
@http(method: "PUT", uri: "/foos/{fooId}", code: 200)
operation UpdateFoo {
    input: UpdateFooRequest
}

structure UpdateFooRequest {
    @httpLabel
    @required
    fooId: String

    fooAlias: String

    writeProperty: ComplexProperty

    mutableProperty: ComplexProperty
}

structure FooProperties {
    addedProperty: String

    @cfnMutability("full")
    barProperty: String

    @cfnMutability("create-and-read")
    immutableSetting: Boolean

    @cfnMutability("read")
    createdAt: Timestamp

    @cfnMutability("write")
    password: String
}

structure ComplexProperty {
    anotherProperty: String
}

list TagList {
    member: String
}

map ArbitraryMap {
    key: String
    value: String
}
