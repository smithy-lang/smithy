$version: "2.0"

namespace ns.foo

service Service {
    version: "1"
    operations: [Operation]
    resources: [Parent]
}

resource Parent {
    resources: [Child]
}

resource Child {}

operation Operation {}
