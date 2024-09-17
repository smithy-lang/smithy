$version: "2.0"

namespace ns.foo

use aws.protocols#httpChecksum

service Service {
    operations: [
        PutSomething
    ]
}

@httpChecksum(
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["CRC32C", "CRC32", "SHA1"]
)
operation PutSomething {
    input: PutSomethingInput
}

structure PutSomethingInput {
    @httpHeader("x-amz-request-algorithm")
    checksumAlgorithm: ChecksumAlgorithm

    @httpHeader("x-amz-response-validation-mode")
    validationMode: ValidationMode

    @httpPayload
    content: Blob
}

enum ChecksumAlgorithm {
    CRC32C
    CRC32
    SHA1
    SHA256
}

enum ValidationMode {
    ENABLED
}
