$version: "2.0"

namespace ns.foo

service Service {
    version: "1"
    resources: [Parent]
}

resource Parent {
    resources: [Child]
}

resource Child {}
