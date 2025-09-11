$version: "2.0"
namespace test.service.error
use aws.protocols#restJson1

@restJson1
service TestService {
    version: "1.0.0",
    operations: [TestOp]
    errors: [ClientError]
}

@http(method: "POST", uri: "/test")
operation TestOp {
    input: TestInput,
    output: TestOutput,
    errors: [CustomError]  // Operation-specific 400 error
}

structure TestInput { value: String }
structure TestOutput { result: String }


@error("client")
@httpError(400)
structure ClientError {
    message: String
}

// Operation-specific 400 error - should have examples generated
@error("client")
@httpError(400)
structure CustomError {
    message: String,
    code: String,
    details: String
}

apply TestOp @examples([
    {
        title: "Success example"
        input: { value: "good" }
        output: { result: "success" }
    },
    {
        title: "Custom error example"
        input: { value: "bad" }
        error: {
            shapeId: CustomError
            content: {
                message: "Custom error occurred"
                code: "CUSTOM_ERROR"
                details: "This should appear in 400 response examples"
            }
        }
        allowConstraintErrors: true
    }
])
