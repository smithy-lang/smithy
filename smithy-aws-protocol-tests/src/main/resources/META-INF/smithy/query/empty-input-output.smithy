// This file defines test cases that test the basics of empty input and
// output shape serialization.

$version: "0.5.0"

namespace aws.protocols.tests.query

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input or output.
///
/// While this should be rare, code generators must support this.
operation NoInputAndNoOutput()

apply NoInputAndNoOutput @httpRequestTests([
    {
        id: "QueryNoInputAndNoOutput",
        description: "No input serializes no additional query params",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
            Action=NoInputAndNoOutput
            &Version=2020-01-08""",
        bodyMediaType: "application/x-www-form-urlencoded"
    }
])

apply NoInputAndNoOutput @httpResponseTests([
   {
       id: "QueryNoInputAndNoOutput",
       description: "No output serializes no payload",
       protocol: "aws.query",
       code: 200,
       body: ""
   }
])

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input and the
/// output is empty.
///
/// While this should be rare, code generators must support this.
operation NoInputAndOutput() -> NoInputAndOutputOutput

apply NoInputAndOutput @httpRequestTests([
    {
        id: "QueryNoInputAndOutput",
        description: "No input serializes no payload",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=NoInputAndOutput
              &Version=2020-01-08""",
        bodyMediaType: "application/x-www-form-urlencoded"
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "QueryNoInputAndOutput",
        description: "Empty output serializes no payload",
        protocol: "aws.query",
        code: 200,
        body: ""
    }
])

structure NoInputAndOutputOutput {}

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has an empty input
/// and empty output structure that reuses the same shape.
///
/// While this should be rare, code generators must support this.
operation EmptyInputAndEmptyOutput(EmptyInputAndEmptyOutputInput) -> EmptyInputAndEmptyOutputOutput

apply EmptyInputAndEmptyOutput @httpRequestTests([
    {
        id: "QueryEmptyInputAndEmptyOutput",
        description: "Empty input serializes no extra query params",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=EmptyInputAndEmptyOutput
              &Version=2020-01-08""",
        bodyMediaType: "application/x-www-form-urlencoded"
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "QueryEmptyInputAndEmptyOutput",
        description: "Empty output serializes no payload",
        protocol: "aws.query",
        code: 200,
        body: ""
    },
])

structure EmptyInputAndEmptyOutputInput {}
structure EmptyInputAndEmptyOutputOutput {}
