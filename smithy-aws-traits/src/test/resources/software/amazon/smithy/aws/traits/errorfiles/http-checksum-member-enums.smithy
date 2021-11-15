namespace smithy.example

use aws.protocols#httpChecksum

@httpChecksum(
    requestAlgorithmMember: "requestAlgorithm",
    requestValidationModeMember: "validationMode",
    responseAlgorithms: ["CRC32C"]
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
    responseAlgorithms: ["CRC32C"]
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
    responseAlgorithms: ["CRC32C"]
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
    responseAlgorithms: ["CRC32C"]
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


@enum([
    {
        value: "SHA2",
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
