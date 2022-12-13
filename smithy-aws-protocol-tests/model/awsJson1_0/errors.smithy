// This file defines test cases that test error serialization.

$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This operation has three possible return values:
///
/// 1. A successful response in the form of GreetingWithErrorsOutput
/// 2. An InvalidGreeting error.
/// 3. A ComplexError error.
///
/// Implementations must be able to successfully take a response and
/// properly deserialize successful and error responses.
@idempotent
operation GreetingWithErrors {
    input: GreetingWithErrorsInput,
    output: GreetingWithErrorsOutput,
    errors: [InvalidGreeting, ComplexError, FooError]
}

@input
structure GreetingWithErrorsInput {
    greeting: String,
}

@output
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
        id: "AwsJson10InvalidGreetingError",
        documentation: "Parses simple JSON errors",
        protocol: awsJson1_0,
        params: {
            Message: "Hi"
        },
        code: 400,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "__type": "aws.protocoltests.json10#InvalidGreeting",
                  "Message": "Hi"
              }""",
        bodyMediaType: "application/json",
    },
])

/// This error is thrown when a request is invalid.
@error("client")
structure ComplexError {
    TopLevel: String,
    Nested: ComplexNestedErrorData,
}

structure ComplexNestedErrorData {
    @jsonName("Fooooo") // Even if this trait it present, it does not affect serialization for this protocol
    Foo: String,
}

apply ComplexError @httpResponseTests([
    {
        id: "AwsJson10ComplexError",
        documentation: "Parses a complex error with no message member",
        protocol: awsJson1_0,
        params: {
            TopLevel: "Top level",
            Nested: {
                Foo: "bar"
            }
        },
        code: 400,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "__type": "aws.protocoltests.json10#ComplexError",
                  "TopLevel": "Top level",
                  "Nested": {
                      "Foo": "bar"
                  }
              }""",
        bodyMediaType: "application/json",
    },
    {
        id: "AwsJson10EmptyComplexError",
        documentation: "Parses a complex error with an empty body",
        protocol: awsJson1_0,
        code: 400,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "__type": "aws.protocoltests.json10#ComplexError"
              }""",
        bodyMediaType: "application/json"
    },
])

/// This error has test cases that test some of the dark corners of Amazon service
/// framework history. It should only be implemented by clients.
@error("server")
@tags(["client-only"])
structure FooError {}

apply FooError @httpResponseTests([
    {
        id: "AwsJson10FooErrorUsingXAmznErrorType",
        documentation: "Serializes the X-Amzn-ErrorType header. For an example service, see Amazon EKS.",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "X-Amzn-Errortype": "FooError",
        },
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorUsingXAmznErrorTypeWithUri",
        documentation: """
            Some X-Amzn-Errortype headers contain URLs. Clients need to split the URL on ':' and take \
            only the first half of the string. For example, 'ValidationException:http://internal.amazon.com/coral/com.amazon.coral.validate/'
            is to be interpreted as 'ValidationException'.

            For an example service see Amazon Polly.""",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "X-Amzn-Errortype": "FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/",
        },
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorUsingXAmznErrorTypeWithUriAndNamespace",
        documentation: """
                     X-Amzn-Errortype might contain a URL and a namespace. Client should extract only the shape \
                     name. This is a pathalogical case that might not actually happen in any deployed AWS service.""",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "X-Amzn-Errortype": "aws.protocoltests.json10#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/",
        },
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorUsingCode",
        documentation: """
                     This example uses the 'code' property in the output rather than X-Amzn-Errortype. Some \
                     services do this though it's preferable to send the X-Amzn-Errortype. Client implementations \
                     must first check for the X-Amzn-Errortype and then check for a top-level 'code' property.

                     For example service see Amazon S3 Glacier.""",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "code": "FooError"
              }""",
        bodyMediaType: "application/json",
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorUsingCodeAndNamespace",
        documentation: """
                     Some services serialize errors using code, and it might contain a namespace. \
                     Clients should just take the last part of the string after '#'.""",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "code": "aws.protocoltests.json10#FooError"
              }""",
        bodyMediaType: "application/json",
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorUsingCodeUriAndNamespace",
        documentation: """
                     Some services serialize errors using code, and it might contain a namespace. It also might \
                     contain a URI. Clients should just take the last part of the string after '#' and before ":". \
                     This is a pathalogical case that might not occur in any deployed AWS service.""",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "code": "aws.protocoltests.json10#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"
              }""",
        bodyMediaType: "application/json",
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorWithDunderType",
        documentation: "Some services serialize errors using __type.",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "__type": "FooError"
              }""",
        bodyMediaType: "application/json",
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorWithDunderTypeAndNamespace",
        documentation: """
                     Some services serialize errors using __type, and it might contain a namespace. \
                     Clients should just take the last part of the string after '#'.""",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "__type": "aws.protocoltests.json10#FooError"
              }""",
        bodyMediaType: "application/json",
        appliesTo: "client",
    },
    {
        id: "AwsJson10FooErrorWithDunderTypeUriAndNamespace",
        documentation: """
                     Some services serialize errors using __type, and it might contain a namespace. It also might \
                     contain a URI. Clients should just take the last part of the string after '#' and before ":". \
                     This is a pathalogical case that might not occur in any deployed AWS service.""",
        protocol: awsJson1_0,
        code: 500,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        body: """
              {
                  "__type": "aws.protocoltests.json10#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"
              }""",
        bodyMediaType: "application/json",
        appliesTo: "client",
    }
])
