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
    responseAlgorithms: ["CRC32C"]
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
    responseAlgorithms: ["CRC32C"]
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
string ChecksumAlgorithm

@enum([
    {
        value: "ENABLED",
        name: "ENABLED"
    }
])
string ValidationMode


