$version: "1.0"

namespace ns.foo

service ShapedService {
    version: "2020-07-02",
    shapes: [
        Common1,
        Common2,
    ],
}

structure Common1 {}

string Common2
