$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.api#auth"
        namespace: "ns.foo"
    }
    {
        id: "ModifiedTrait.Update.smithy.rules#endpointRuleSet"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use aws.auth#sigv4
use aws.auth#sigv4a
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
                        {
                            name: "sigv4"
                        }
                    ]
                }
            }
        }
    ]
)
@auth([sigv4, httpBearerAuth])
@httpBearerAuth
@sigv4(name: "service1")
@sigv4a(name: "service1")
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
                            name: "sigv4"
                        }
                        {
                            name: "example"
                        }
                    ]
                }
            }
        }
    ]
)
@auth([sigv4, httpBearerAuth])
@httpBearerAuth
@sigv4(name: "service2")
@sigv4a(name: "service2")
service Service2 {}

@auth([sigv4, httpBearerAuth])
@httpBearerAuth
@sigv4(name: "service3")
@sigv4a(name: "service3")
service Service3 {}

@auth([httpBearerAuth, sigv4])
@httpBearerAuth
@sigv4(name: "service4")
@sigv4a(name: "service4")
service Service4 {}
