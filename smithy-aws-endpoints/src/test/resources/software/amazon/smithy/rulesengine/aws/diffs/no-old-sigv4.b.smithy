$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Add.smithy.rules#endpointRuleSet"
        namespace: "ns.foo"
    }
    {
        id: "ModifiedTrait.Remove.smithy.rules#endpointRuleSet"
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
                            name: "example"
                        }
                    ]
                }
            }
        }
    ]
)
@auth([httpBearerAuth])
@httpBearerAuth
service Service1 {}

@auth([httpBearerAuth])
@httpBearerAuth
service Service2 {}

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
                            name: "example"
                        }
                    ]
                }
            }
        }
    ]
)
@auth([httpBearerAuth])
@httpBearerAuth
service Service3 {}

@auth([httpBearerAuth])
@httpBearerAuth
service Service4 {}
