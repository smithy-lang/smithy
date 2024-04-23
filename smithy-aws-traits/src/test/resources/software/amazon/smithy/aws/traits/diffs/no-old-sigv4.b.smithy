$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.api#auth"
        namespace: "ns.foo"
    }
]

namespace ns.foo

@auth([httpBearerAuth])
@httpBearerAuth
service Service {
    operations: [
        Operation
    ]
}

@auth([httpBearerAuth])
operation Operation {}
