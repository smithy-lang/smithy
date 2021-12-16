// This file defines test cases that test the basics of empty input and
// output shape serialization.

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input or output.
/// While this should be rare, code generators must support this.
@http(uri: "/NoInputAndNoOutput", method: "POST")
operation NoInputAndNoOutput {}

apply NoInputAndNoOutput @httpRequestTests([
    {
        id: "NoInputAndNoOutput",
        documentation: "No input serializes no payload",
        protocol: restXml,
        method: "POST",
        uri: "/NoInputAndNoOutput",
        body: ""
    }
])

apply NoInputAndNoOutput @httpResponseTests([
   {
       id: "NoInputAndNoOutput",
       documentation: "No output serializes no payload",
       protocol: restXml,
       code: 200,
       body: ""
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
        id: "NoInputAndOutput",
        documentation: "No input serializes no payload",
        protocol: restXml,
        method: "POST",
        uri: "/NoInputAndOutputOutput",
        body: ""
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "NoInputAndOutput",
        documentation: "Empty output serializes no payload",
        protocol: restXml,
        code: 200,
        body: ""
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
        id: "EmptyInputAndEmptyOutput",
        documentation: "Empty input serializes no payload",
        protocol: restXml,
        method: "POST",
        uri: "/EmptyInputAndEmptyOutput",
        body: ""
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "EmptyInputAndEmptyOutput",
        documentation: "Empty output serializes no payload",
        protocol: restXml,
        code: 200,
        body: ""
    },
])

structure EmptyInputAndEmptyOutputInput {}
structure EmptyInputAndEmptyOutputOutput {}
