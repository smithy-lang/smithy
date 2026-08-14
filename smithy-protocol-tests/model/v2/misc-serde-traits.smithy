$version: "2.0"

namespace smithy.protocoltests.corpus

// MiscSerdeTraitProtocolTestService tests serialization traits that affect the
// request or response envelope rather than the shape of the body, and that
// every protocol supports.
@mixin
service MiscSerdeTraitProtocolTestService {
    operations: [
        EndpointHostPrefix
        EndpointHostLabel
        IdempotencyTokenOp
        RequestCompressionOp
        MediaTypeOp
    ]
}

@endpoint(hostPrefix: "data.")
operation EndpointHostPrefix {
    input: EndpointHostPrefixInput
    output: EndpointHostPrefixOutput
}

structure EndpointHostPrefixInput {}

structure EndpointHostPrefixOutput {}

@endpoint(hostPrefix: "data.{label}.")
operation EndpointHostLabel {
    input: EndpointHostLabelInput
    output: EndpointHostLabelOutput
}

structure EndpointHostLabelInput {
    @required
    @hostLabel
    label: String
}

structure EndpointHostLabelOutput {}

operation IdempotencyTokenOp {
    input: IdempotencyTokenOpInput
    output: IdempotencyTokenOpOutput
}

structure IdempotencyTokenOpInput {
    @idempotencyToken
    token: String
}

structure IdempotencyTokenOpOutput {}

@requestCompression(
    encodings: ["gzip"]
)
operation RequestCompressionOp {
    input: RequestCompressionOpInput
    output: RequestCompressionOpOutput
}

structure RequestCompressionOpInput {
    data: String
}

structure RequestCompressionOpOutput {}

operation MediaTypeOp {
    input: MediaTypeOpInputOutput
    output: MediaTypeOpInputOutput
}

structure MediaTypeOpInputOutput {
    mediaTypeMember: MediaTypeJsonString
}

@mediaType("application/json")
string MediaTypeJsonString
