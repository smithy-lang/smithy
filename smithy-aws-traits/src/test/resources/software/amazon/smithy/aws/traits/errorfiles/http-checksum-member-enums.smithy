namespace smithy.example

use aws.protocols#httpChecksum

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["crc32c"]
)
@suppress(["UnstableTrait"])
operation ValidEnums {
    input: ValidEnumsInput,
    output: ValidEnumsOutput,

}

structure ValidEnumsInput {
    requestAlgorithm: ChecksumAlgorithm,
    validationMode: ValidationMode,
}

structure ValidEnumsOutput {}

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["crc32c"]
)
@suppress(["UnstableTrait"])
operation InvalidEnums {
    input: InvalidEnumsInput,
    output: InvalidEnumsOutput,
}

structure InvalidEnumsInput {
    requestAlgorithm: BadChecksumAlgorithm,
    validationMode: BadValidationMode,
}

structure InvalidEnumsOutput {}

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["crc32c"]
)
@suppress(["UnstableTrait"])
operation NoEnums {
    input: NoEnumsInput,
    output: NoEnumsOutput,
}

structure NoEnumsInput {
    requestAlgorithm: String,
    validationMode: String,
}

structure NoEnumsOutput {}

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["crc32c"]
)
@suppress(["UnstableTrait"])
operation NoMember {
    input: NoMemberInput,
    output: NoMemberOutput,
}

structure NoMemberInput {}

structure NoMemberOutput {}

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


@enum([
    {
        value: "sha2",
        name: "SHA2"
    }
])
string BadChecksumAlgorithm

@enum([
    {
        value: "DISABLED",
        name: "DISABLED"
    }
])
string BadValidationMode

map StringMap {
    key: String,
    value: String
}
