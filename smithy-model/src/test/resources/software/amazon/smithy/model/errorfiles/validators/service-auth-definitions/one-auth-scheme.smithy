$version: "2"

namespace smithy.example

// Service shape with one auth schemes and no auth trait should NOT cause warning
@httpBasicAuth
service FooService {
    version: "2023-08-15"
    operations: [GetFoo]
}

operation GetFoo {
    output: GetFooOutput
}

structure GetFooOutput {}
