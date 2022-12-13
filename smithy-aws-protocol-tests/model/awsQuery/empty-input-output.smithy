// This file defines test cases that test the basics of empty input and
// output shape serialization.

$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input or output.
///
/// While this should be rare, code generators must support this.
operation NoInputAndNoOutput {}

apply NoInputAndNoOutput @httpRequestTests([
    {
        id: "QueryNoInputAndNoOutput",
        documentation: "No input serializes no additional query params",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=NoInputAndNoOutput&Version=2020-01-08",
        bodyMediaType: "application/x-www-form-urlencoded"
    }
])

apply NoInputAndNoOutput @httpResponseTests([
   {
       id: "QueryNoInputAndNoOutput",
       documentation: "Empty output. Note that no assertion is made on the output body itself.",
       protocol: awsQuery,
       code: 200,
   }
])

/// The example tests how requests and responses are serialized when there's
/// no request payload or response members.
///
/// While this should be rare, code generators must support this.
operation NoInputAndOutput {
    input: NoInputAndOutputInput,
    output: NoInputAndOutputOutput
}

apply NoInputAndOutput @httpRequestTests([
    {
        id: "QueryNoInputAndOutput",
        documentation: "No input serializes no payload",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=NoInputAndOutput&Version=2020-01-08",
        bodyMediaType: "application/x-www-form-urlencoded"
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "QueryNoInputAndOutput",
        documentation: "Empty output",
        protocol: awsQuery,
        code: 200,
    }
])

@input
structure NoInputAndOutputInput {}

@output
structure NoInputAndOutputOutput {}

/// The example tests how requests and responses are serialized when there's
/// no request or response members.
///
/// While this should be rare, code generators must support this.
operation EmptyInputAndEmptyOutput {
    input: EmptyInputAndEmptyOutputInput,
    output: EmptyInputAndEmptyOutputOutput
}

apply EmptyInputAndEmptyOutput @httpRequestTests([
    {
        id: "QueryEmptyInputAndEmptyOutput",
        documentation: "Empty input serializes no extra query params",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=EmptyInputAndEmptyOutput&Version=2020-01-08",
        bodyMediaType: "application/x-www-form-urlencoded"
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "QueryEmptyInputAndEmptyOutput",
        documentation: "Empty output",
        protocol: awsQuery,
        code: 200,
    },
])

structure EmptyInputAndEmptyOutputInput {}
structure EmptyInputAndEmptyOutputOutput {}
