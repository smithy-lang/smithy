$version: "2"

namespace smithy.example

@httpBasicAuth
@httpBearerAuth
service MyService1 {
    version: "2020-04-21",
    operations: [HasDigestAuth, HasBasicAuth, NoAuth]
}

@auth([httpDigestAuth])
@http(uri: "/", method: "GET")
operation HasDigestAuth {}

@auth([httpBasicAuth])
operation HasBasicAuth {}

@http(uri: "/", method: "POST")
operation NoAuth {}

@httpBearerAuth
service MyService2 {
    version: "2020-04-21",
    operations: [HasDigestAuth, HasBasicAuth, NoAuth]
}
