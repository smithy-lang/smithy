$version: "2.0"

namespace smithy.protocols

use smithy.api#cors
use smithy.api#endpoint
use smithy.api#hostLabel
use smithy.api#httpError
use smithy.api#requestCompression

/// An RPC-based protocol that serializes JSON payloads.
@trait(selector: "service")
@protocolDefinition(traits: [
    cors
    endpoint
    hostLabel
    httpError
    requestCompression
])
structure rpcv2Json {
    /// Priority ordered list of supported HTTP protocol versions.
    http: StringList

    /// Priority ordered list of supported HTTP protocol versions
    /// that are required when using event streams.
    eventStreamHttp: StringList
}
