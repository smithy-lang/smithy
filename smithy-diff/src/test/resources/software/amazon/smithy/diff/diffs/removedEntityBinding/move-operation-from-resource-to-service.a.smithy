$version: "2.0"

namespace ns.foo

service Service {
    version: "1"
    resources: [Resource]
}

resource Resource {
    operations: [Operation]
}

operation Operation {}
