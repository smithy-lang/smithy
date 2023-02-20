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

    /// The list of serialization formats supported by the service.
    /// Must contain at least one entry. Can currently only contain "cbor".
    @required
    @length(min: 1)
    format: FormatList
}

@private
list FormatList {
    @pattern("^[a-z0-9\\-]+$")
    member: String
}

@private
list StringList {
    member: String
}
