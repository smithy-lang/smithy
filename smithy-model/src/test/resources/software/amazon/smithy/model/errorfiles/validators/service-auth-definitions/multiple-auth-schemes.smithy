$version: "2"

namespace smithy.example

// Service shape with multiple auth schemes and no auth trait should cause warning
@httpBasicAuth
@httpDigestAuth
@httpBearerAuth
service FooService {
    version: "2023-08-15"
    operations: [GetFoo]
}

operation GetFoo {
    output: GetFooOutput
}

structure GetFooOutput {}
