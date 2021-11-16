namespace smithy.example

use aws.protocols#httpChecksum

@httpChecksum()
@suppress(["UnstableTrait"])
operation NoBehavior {
    input: NoBehaviorInput,
    output: Unit
}

@input
structure NoBehaviorInput {}

@httpChecksum(
    requestChecksumRequired: true,
)
@suppress(["UnstableTrait"])
operation NoInput {
    output: Unit
}

@httpChecksum(
    responseAlgorithms: ["CRC32C"]
)
@suppress(["UnstableTrait"])
operation NoModeForResponse {
    input: NoModeForResponseInput,
    output: NoModeForResponseOutput,
}

@input
structure NoModeForResponseInput {}

@output
structure NoModeForResponseOutput {}

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["CRC32C"]
)
@suppress(["UnstableTrait"])
operation NoOutputForResponse {
    input: NoOutputForResponseInput,
    output: Unit
}

@input
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


