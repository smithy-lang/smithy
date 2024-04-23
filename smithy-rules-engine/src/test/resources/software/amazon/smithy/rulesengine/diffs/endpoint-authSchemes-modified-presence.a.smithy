$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.rules#endpointRuleSet"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use smithy.rules#endpointRuleSet

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {
                    authSchemes: [
                        {
                            name: "example1"
                        }
                        {
                            name: "example2"
                        }
                    ]
                }
            }
        }
    ]
)
service Service1 {}

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {
                    authSchemes: [
                        {
                            name: "example1"
                        }
                    ]
                }
            }
        }
    ]
)
service Service2 {}
