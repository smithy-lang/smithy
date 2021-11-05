namespace example.smithy

use aws.protocols#httpChecksum
use aws.protocols#restJson1

@restJson1
service MyService {
    version: "2020-07-02",
    operations: [ValidEnums,],
}

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["crc32c"]
)
@http(method: "GET", uri: "/unsupported")
@readonly
@suppress(["UnstableTrait"])
operation ValidEnums {
    input: ValidEnumsInput,
    output: ValidEnumsOutput,

}

structure ValidEnumsInput {
    @httpQuery("requestAlgorithm")
    requestAlgorithm: RequestAlgorithm,

    @httpQuery("validationMode")
    validationMode: ValidationMode,
}

structure ValidEnumsOutput {}

@enum([
    {
        value: "crc32c",
        name: "CRC32C"
    },
    {
        value: "crc32",
        name: "CRC32"
    },
    {
        value: "sha1",
        name: "SHA1"
    },
    {
        value: "sha256",
        name: "SHA256"
    }
])
string RequestAlgorithm

@enum([
    {
        value: "ENABLED",
        name: "ENABLED"
    }
])
string ValidationMode
