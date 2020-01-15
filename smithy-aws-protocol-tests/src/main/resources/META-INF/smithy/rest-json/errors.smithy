// This file defines test cases that test error serialization.

$version: "0.5.0"

namespace aws.protocols.tests.restjson

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This operation has four possible return values:
///
/// 1. A successful response in the form of GreetingWithErrorsOutput
/// 2. An InvalidGreeting error.
/// 3. A BadRequest error.
/// 4. A FooError.
///
/// Implementations must be able to successfully take a response and
/// properly (de)serialize successful and error responses based on the
/// the presence of the
@idempotent
@http(uri: "/GreetingWithErrors", method: "PUT")
operation GreetingWithErrors {
    output: GreetingWithErrorsOutput,
    errors: [InvalidGreeting, ComplexError, FooError]
}

apply GreetingWithErrors @httpResponseTests([
    {
        id: "RestJsonGreetingWithErrors",
        description: "Ensures that operations with errors successfully know how to deserialize the successful response",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "greeting": "Hello"
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json",
            "X-Greeting": "Hello",
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
        id: "RestJsonInvalidGreetingError",
        description: "Parses simple JSON errors",
        protocol: "aws.rest-json-1.1",
        params: {
            Message: "Hi"
        },
        code: 400,
        headers: {
            "Content-Type": "application/json",
            "X-Amzn-Errortype": "InvalidGreeting",
        },
        body: """
              {
                  "Message": "Hi"
              }""",
        bodyMediaType: "application/json",
    },

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
        id: "RestJsonComplexErrorWithNoMessage",
        description: "Serializes a complex error with no message member",
        protocol: "aws.rest-json-1.1",
        params: {
            Header: "Header",
            TopLevel: "Top level",
            Nested: {
                Foo: "bar"
            }
        },
        code: 403,
        headers: {
            "Content-Type": "application/json",
            "X-Header": "Header",
            "X-Amzn-Errortype": "ComplexError",
        },
        body: """
              {
                  "TopLevel": "Top level",
                  "Nested": {
                      "Fooooo": "bar"
                  }
              }""",
        bodyMediaType: "application/json",
    },
    {
        id: "RestJsonEmptyComplexErrorWithNoMessage",
        protocol: "aws.rest-json-1.1",
        params: {},
        code: 403,
        headers: {
            "Content-Type": "application/json"
        },
        body: "{}",
        bodyMediaType: "application/json",
    },
])

structure ComplexNestedErrorData {
    @jsonName("Fooooo")
    Foo: String,
}

/// This error has test cases that test some of the dark corners of Amazon service
/// framework history. It should only be implemented by clients.
@error("server")
@httpError(500)
@tags(["client-only"])
structure FooError {}

apply FooError @httpResponseTests([
    {
        id: "RestJsonFooErrorUsingXAmznErrorType",
        description: "Serializes the X-Amzn-ErrorType header. For an example service, see Amazon EKS.",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "X-Amzn-Errortype": "FooError",
        },
    },
    {
        id: "RestJsonFooErrorUsingXAmznErrorTypeWithUri",
        description: """
            Some X-Amzn-Errortype headers contain URLs. Clients need to split the URL on ':' and take \
            only the first half of the string. For example, 'ValidationException:http://internal.amazon.com/coral/com.amazon.coral.validate/'
            is to be interpreted as 'ValidationException'.

            For an example service see Amazon Polly.""",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "X-Amzn-Errortype": "FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/",
        },
    },
    {
        id: "RestJsonFooErrorUsingXAmznErrorTypeWithUriAndNamespace",
        description: """
                     X-Amzn-Errortype might contain a URL and a namespace. Client should extract only the shape \
                     name. This is a pathalogical case that might not actually happen in any deployed AWS service.""",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "X-Amzn-Errortype": "aws.protocols.tests.restjson#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/",
        },
    },
    {
        id: "RestJsonFooErrorUsingCode",
        description: """
                     This example uses the 'code' property in the output rather than X-Amzn-Errortype. Some \
                     services do this though it's preferable to send the X-Amzn-Errortype. Client implementations \
                     must first check for the X-Amzn-Errortype and then check for a top-level 'code' property.

                     For example service see Amazon S3 Glacier.""",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "code": "FooError"
              }""",
        bodyMediaType: "application/json",
    },
    {
        id: "RestJsonFooErrorUsingCodeAndNamespace",
        description: """
                     Some services serialize errors using code, and it might contain a namespace. \
                     Clients should just take the last part of the string after '#'.""",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "code": "aws.protocols.tests.restjson#FooError"
              }""",
        bodyMediaType: "application/json",
    },
    {
        id: "RestJsonFooErrorUsingCodeUriAndNamespace",
        description: """
                     Some services serialize errors using code, and it might contain a namespace. It also might \
                     contain a URI. Clients should just take the last part of the string after '#' and before ":". \
                     This is a pathalogical case that might not occur in any deployed AWS service.""",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "code": "aws.protocols.tests.restjson#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"
              }""",
        bodyMediaType: "application/json",
    },
    {
        id: "RestJsonFooErrorWithDunderType",
        description: "Some services serialize errors using __type.",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "__type": "FooError"
              }""",
        bodyMediaType: "application/json",
    },
    {
        id: "RestJsonFooErrorWithDunderTypeAndNamespace",
        description: """
                     Some services serialize errors using __type, and it might contain a namespace. \
                     Clients should just take the last part of the string after '#'.""",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "__type": "aws.protocols.tests.restjson#FooError"
              }""",
        bodyMediaType: "application/json",
    },
    {
        id: "RestJsonFooErrorWithDunderTypeUriAndNamespace",
        description: """
                     Some services serialize errors using __type, and it might contain a namespace. It also might \
                     contain a URI. Clients should just take the last part of the string after '#' and before ":". \
                     This is a pathalogical case that might not occur in any deployed AWS service.""",
        protocol: "aws.rest-json-1.1",
        code: 500,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "__type": "aws.protocols.tests.restjson#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"
              }""",
        bodyMediaType: "application/json",
    }
])
