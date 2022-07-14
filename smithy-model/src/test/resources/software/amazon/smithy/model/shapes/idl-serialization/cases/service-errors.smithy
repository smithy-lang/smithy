$version: "2.0"

namespace ns.foo

service EmptyService {
    version: "2020-02-18"
    errors: [
        Common1
        Common2
    ]
}

@error("client")
structure Common1 {}

@error("server")
structure Common2 {}
