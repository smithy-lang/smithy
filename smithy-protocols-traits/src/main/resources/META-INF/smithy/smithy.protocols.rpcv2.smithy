$version: "2.0"

namespace smithy.protocols

@trait(selector: "service")
@protocolDefinition(traits: ["smithy.api#httpError"])
@documentation("An RPC-based protocol with binary encoding support.")
structure smithyRpcV2 {
    http: StringList,
    eventStreamHttp: StringList,
    @required
    format: StringList
}

@documentation("A list of String shapes.")
list StringList {
    member: String
}
