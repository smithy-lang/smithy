$version: "2.0"

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

@input
structure ValidEnumsInput {
    requestAlgorithm: ChecksumAlgorithm,
    validationMode: ValidationMode,
}

@output
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

@input
structure InvalidEnumsInput {
    requestAlgorithm: BadChecksumAlgorithm,
    validationMode: BadValidationMode,
}

@output
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

@input
structure NoEnumsInput {
    requestAlgorithm: String,
    validationMode: String,
}

@output
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

@input
structure NoMemberInput {}

@output
structure NoMemberOutput {}

enum ChecksumAlgorithm {
    CRC32C
    CRC32
    SHA1
    SHA256
}

enum ValidationMode {
    ENABLED
}


enum BadChecksumAlgorithm {
    SHA2
}

enum BadValidationMode {
    DISABLED
}

map StringMap {
    key: String,
    value: String
}
