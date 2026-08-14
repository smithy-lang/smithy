$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsQueryError

// Shapes for @awsQueryCompatible coverage: services that migrated off awsQuery and
// have to keep exposing awsQuery error codes to old callers. The service itself is
// declared per protocol, since the trait is service-level and only some protocols
// accept it; this file holds the operation they share. Its two errors cover the code
// being derived from the shape name and the code being overridden by @awsQueryError.
//
// Note @awsQueryError's httpResponseCode is NOT applied by the RPC v2 family: it
// records what awsQuery itself would have returned, and the status still comes from
// the @error trait.
operation QueryCompatErrorOp {
    input: QueryCompatErrorOpInput
    output: QueryCompatErrorOpOutput
    errors: [
        QueryCompatError
        QueryCompatCustomCodeError
    ]
}

structure QueryCompatErrorOpInput {}

structure QueryCompatErrorOpOutput {}

@error("client")
structure QueryCompatError {
    message: String
}

@awsQueryError(code: "CustomCode", httpResponseCode: 402)
@error("client")
structure QueryCompatCustomCodeError {
    message: String
}
