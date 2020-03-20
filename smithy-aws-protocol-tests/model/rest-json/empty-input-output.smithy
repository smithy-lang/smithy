// This file defines test cases that test the basics of empty input and
// output shape serialization.
//
// TODO: does an operation with no input always send {}? What about no output?

$version: "0.5.0"

namespace aws.protocols.tests.restjson

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input or output.
/// While this should be rare, code generators must support this.
@http(uri: "/NoInputAndNoOutput", method: "POST")
operation NoInputAndNoOutput {}

apply NoInputAndNoOutput @httpRequestTests([
    {
        id: "RestJsonNoInputAndNoOutput",
        documentation: "No input serializes no payload",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/NoInputAndNoOutput"
    }
])

apply NoInputAndNoOutput @httpResponseTests([
   {
       id: "RestJsonNoInputAndNoOutput",
       documentation: "No output serializes no payload",
       protocol: "aws.rest-json-1.1",
       code: 200
   }
])

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input and the
/// output is empty. While this should be rare, code generators must support
/// this.
@http(uri: "/NoInputAndOutputOutput", method: "POST")
operation NoInputAndOutput {
    output: NoInputAndOutputOutput
}

apply NoInputAndOutput @httpRequestTests([
    {
        id: "RestJsonNoInputAndOutput",
        documentation: "No input serializes no payload",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/NoInputAndOutputOutput"
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "RestJsonNoInputAndOutput",
        documentation: "Empty output serializes no payload",
        protocol: "aws.rest-json-1.1",
        code: 200
    }
])

structure NoInputAndOutputOutput {}

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has an empty input
/// and empty output structure that reuses the same shape. While this should
/// be rare, code generators must support this.
@http(uri: "/EmptyInputAndEmptyOutput", method: "POST")
operation EmptyInputAndEmptyOutput {
    input: EmptyInputAndEmptyOutputInput,
    output: EmptyInputAndEmptyOutputOutput
}

apply EmptyInputAndEmptyOutput @httpRequestTests([
    {
        id: "RestJsonEmptyInputAndEmptyOutput",
        documentation: "Empty input serializes no payload",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/EmptyInputAndEmptyOutput",
        body: "{}",
        bodyMediaType: "application/json"
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "RestJsonEmptyInputAndEmptyOutput",
        documentation: "Empty output serializes no payload",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: "{}",
        bodyMediaType: "application/json"
    },
])

structure EmptyInputAndEmptyOutputInput {}
structure EmptyInputAndEmptyOutputOutput {}
