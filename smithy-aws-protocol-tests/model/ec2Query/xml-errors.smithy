// This file defines test cases that serialize aws.ec2 errors.
//
// EC2 protocol errors look like this:
//
// <Response>
//     <Errors>
//         <Error>
//             <Code>InvalidInstanceID.NotFound</Code>
//             <Message>The instance ID 'i-1a2b3c4d' does not exist</Message>
//         </Error>
//     </Errors>
//     <RequestID>ea966190-f9aa-478e-9ede-example</RequestID>
// </Response>
//
// These errors differ from query errors in that they have an <Errors>
// wrapper, they always have a wrapping element named <Response>, and
// they do not send a <Type> element in the error data.
//
// See: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/errors-overview.html#api-error-response

$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
use smithy.test#httpResponseTests

/// This operation has three possible return values:
///
/// 1. A successful response in the form of GreetingWithErrorsOutput
/// 2. An InvalidGreeting error.
/// 3. A BadRequest error.
operation GreetingWithErrors {
    output: GreetingWithErrorsOutput,
    errors: [InvalidGreeting, ComplexError]
}

apply GreetingWithErrors @httpResponseTests([
    {
        id: "Ec2GreetingWithErrors",
        documentation: "Ensures that operations with errors successfully know how to deserialize the successful response",
        protocol: ec2Query,
        code: 200,
        headers: {
            "Content-Type": "text/xml;charset=UTF-8"
        },
        body: """
              <GreetingWithErrorsResponse xmlns="https://example.com/">
                  <greeting>Hello</greeting>
                  <requestId>requestid</requestId>
              </GreetingWithErrorsResponse>
              """,
        bodyMediaType: "application/xml",
        params: {
            greeting: "Hello"
        }
    }
])

structure GreetingWithErrorsOutput {
    greeting: String,
}

/// This error is thrown when an invalid greeting value is provided.
@error("client")
structure InvalidGreeting {
    Message: String,
}

apply InvalidGreeting @httpResponseTests([
    {
        id: "Ec2InvalidGreetingError",
        documentation: "Parses simple XML errors",
        protocol: ec2Query,
        code: 400,
        headers: {
            "Content-Type": "text/xml;charset=UTF-8"
        },
        body: """
              <Response>
                  <Errors>
                      <Error>
                          <Code>InvalidGreeting</Code>
                          <Message>Hi</Message>
                      </Error>
                  </Errors>
                  <RequestID>foo-id</RequestID>
              </Response>
              """,
        bodyMediaType: "application/xml",
        params: {
            Message: "Hi"
        },
    }
])

/// This error is thrown when a request is invalid.
@error("client")
structure ComplexError {
    TopLevel: String,

    Nested: ComplexNestedErrorData,
}

apply ComplexError @httpResponseTests([
    {
        id: "Ec2ComplexError",
        protocol: ec2Query,
        params: {
            TopLevel: "Top level",
            Nested: {
                Foo: "bar"
            }
        },
        code: 400,
        headers: {
            "Content-Type": "text/xml;charset=UTF-8"
        },
        body: """
              <Response>
                  <Errors>
                      <Error>
                          <Code>ComplexError</Code>
                          <Message>Hi</Message>
                          <TopLevel>Top level</TopLevel>
                          <Nested>
                              <Foo>bar</Foo>
                          </Nested>
                      </Error>
                  </Errors>
                  <RequestID>foo-id</RequestID>
              </Response>
              """,
        bodyMediaType: "application/xml",
    }
])

structure ComplexNestedErrorData {
    Foo: String,
}
