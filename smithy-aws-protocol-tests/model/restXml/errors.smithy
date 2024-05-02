// This file defines test cases that test error serialization.

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This operation has three possible return values:
///
/// 1. A successful response in the form of GreetingWithErrorsOutput
/// 2. An InvalidGreeting error.
/// 3. A BadRequest error.
///
/// Implementations must be able to successfully take a response and
/// properly (de)serialize successful and error responses based on the
/// the presence of the
@idempotent
@http(uri: "/GreetingWithErrors", method: "PUT")
operation GreetingWithErrors {
    output: GreetingWithErrorsOutput,
    errors: [InvalidGreeting, ComplexError]
}

apply GreetingWithErrors @httpResponseTests([
    {
        id: "GreetingWithErrors",
        documentation: "Ensures that operations with errors successfully know how to deserialize the successful response",
        protocol: restXml,
        code: 200,
        body: "",
        headers: {
            "X-Greeting": "Hello"
        },
        params: {
            greeting: "Hello"
        }
    }
])

structure GreetingWithErrorsOutput {
    @httpHeader("X-Greeting")
    greeting: String,
}

/// This error is thrown when an invalid greeting value is provided.
@error("client")
@httpError(400)
structure InvalidGreeting {
    Message: String,
}

apply InvalidGreeting @httpResponseTests([
    {
        id: "InvalidGreetingError",
        documentation: "Parses simple XML errors",
        protocol: restXml,
        params: {
            Message: "Hi"
        },
        code: 400,
        headers: {
            "Content-Type": "application/xml"
        },
        body: """
              <ErrorResponse>
                 <Error>
                    <Type>Sender</Type>
                    <Code>InvalidGreeting</Code>
                    <Message>Hi</Message>
                    <AnotherSetting>setting</AnotherSetting>
                 </Error>
                 <RequestId>foo-id</RequestId>
              </ErrorResponse>
              """,
        bodyMediaType: "application/xml",
    }
])

/// This error is thrown when a request is invalid.
@error("client")
@httpError(403)
structure ComplexError {
    // Errors support HTTP bindings!
    @httpHeader("X-Header")
    Header: String,

    TopLevel: String,

    Nested: ComplexNestedErrorData,
}

apply ComplexError @httpResponseTests([
    {
        id: "ComplexError",
        protocol: restXml,
        params: {
            Header: "Header",
            TopLevel: "Top level",
            Nested: {
                Foo: "bar"
            }
        },
        code: 403,
        headers: {
            "Content-Type": "application/xml",
            "X-Header": "Header",
        },
        body: """
              <ErrorResponse>
                 <Error>
                    <Type>Sender</Type>
                    <Code>ComplexError</Code>
                    <Message>Hi</Message>
                    <TopLevel>Top level</TopLevel>
                    <Nested>
                        <Foo>bar</Foo>
                    </Nested>
                 </Error>
                 <RequestId>foo-id</RequestId>
              </ErrorResponse>
              """,
        bodyMediaType: "application/xml",
    }
])

structure ComplexNestedErrorData {
    Foo: String,
}
