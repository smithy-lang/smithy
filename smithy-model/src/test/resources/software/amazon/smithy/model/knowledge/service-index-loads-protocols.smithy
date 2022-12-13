$version: "2"

namespace smithy.example

@fooJson
@fooXml
service TestService {
    version: "2020-01-29"
}

@trait(selector: "service")
@protocolDefinition
structure fooJson {}

@trait(selector: "service")
@protocolDefinition
structure fooXml {}
