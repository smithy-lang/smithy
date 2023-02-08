$version: "2.0"

namespace smithy.protocols

use smithy.api#httpError
use smithy.api#cors
use smithy.api#endpoint
use smithy.api#hostLabel

/// An RPC protocol with support for multiple wire formats.
@trait(selector: "service")
@protocolDefinition(traits: [
    httpError
    cors
    endpoint
    hostLabel
])
structure rpcv2 {
    /// Priority ordered list of supported HTTP protocol versions.
    http: StringList
    /// Priority ordered list of supported HTTP protocol versions
    /// that are required when using event streams.
    eventStreamHttp: StringList

    /// Priority ordered list of supported wire formats.
    @required
    format: StringList
}

/// A list of String shapes.
list StringList {
    member: String
}
