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
    responseAlgorithms: ["CRC32C"]
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
        value: "CRC32C",
        name: "CRC32C"
    },
    {
        value: "CRC32",
        name: "CRC32"
    },
    {
        value: "SHA1",
        name: "SHA1"
    },
    {
        value: "SHA256",
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
