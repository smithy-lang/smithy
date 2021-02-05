$version: "1.0"

namespace aws.protocoltests.json

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(
    sdkId: "Json Protocol",
    arnNamespace: "jsonprotocol",
    cloudFormationName: "JsonProtocol",
    cloudTrailEventSource: "jsonprotocol.amazonaws.com",
)
@sigv4(name: "foo")
@awsJson1_1
@title("Sample Json 1.1 Protocol Service")
service JsonProtocol {
    version: "2018-01-01",
    operations: [
        EmptyOperation,
        KitchenSinkOperation,
        OperationWithOptionalInputOutput,
        PutAndGetInlineDocuments,
        JsonEnums,
        NullOperation,
        GreetingWithErrors,
        JsonUnions,

        // @endpoint and @hostLabel trait tests
        EndpointOperation,
        EndpointWithHostLabelOperation,
    ],
}

structure EmptyStruct {}

structure SimpleStruct {
    Value: String,
}

structure StructWithLocationName {
    @jsonName("RenamedMember")
    Value: String,
}

list ListOfListOfStrings {
    member: ListOfStrings,
}

list ListOfMapsOfStrings {
    member: MapOfStrings,
}

list ListOfStrings {
    member: String,
}

list ListOfStructs {
    member: SimpleStruct,
}

map MapOfListsOfStrings {
    key: String,
    value: ListOfStrings,
}

map MapOfMapOfStrings {
    key: String,
    value: MapOfStrings,
}

map MapOfStrings {
    key: String,
    value: String,
}

map MapOfStructs {
    key: String,
    value: SimpleStruct,
}

@mediaType("application/json")
string JsonValue
