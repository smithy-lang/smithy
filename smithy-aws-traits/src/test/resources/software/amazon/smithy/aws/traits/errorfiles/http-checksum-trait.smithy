namespace smithy.example

use aws.protocols#httpChecksum

@httpChecksum()
@suppress(["UnstableTrait"])
operation NoBehavior {
    input: NoBehaviorInput,
}

structure NoBehaviorInput {}

@httpChecksum(
    requestChecksumRequired: true,
)
@suppress(["UnstableTrait"])
operation NoInput {}

@httpChecksum(
    responseAlgorithms: ["crc32c"]
)
@suppress(["UnstableTrait"])
operation NoModeForResponse {
    input: NoModeForResponseInput,
    output: NoModeForResponseOutput,

}

structure NoModeForResponseInput {}

structure NoModeForResponseOutput {}

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["crc32c"]
)
@suppress(["UnstableTrait"])
operation NoOutputForResponse {
    input: NoOutputForResponseInput,

}

structure NoOutputForResponseInput {
    requestAlgorithm: ChecksumAlgorithm,
    validationMode: ValidationMode,
}

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
string ChecksumAlgorithm

@enum([
    {
        value: "ENABLED",
        name: "ENABLED"
    }
])
string ValidationMode


