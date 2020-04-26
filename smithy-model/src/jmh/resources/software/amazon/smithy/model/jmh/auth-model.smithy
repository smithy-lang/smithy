namespace smithy.example

@httpBasicAuth
@httpBearerAuth
service MyService1 {
    version: "2020-04-21",
    operations: [HasDigestAuth, HasDigestAndBasicAuth, HasBasicAuth, NoAuth]
}

@auth([httpDigestAuth])
@http(uri: "/", method: "GET")
operation HasDigestAuth {}

@auth([httpDigestAuth, httpBasicAuth])
@http(uri: "/", method: "HEAD")
operation HasDigestAndBasicAuth {}

@auth([httpBasicAuth])
operation HasBasicAuth {}

@http(uri: "/", method: "POST")
operation NoAuth {
    input: NoAuthInput,
}

structure NoAuthInput {
    @httpPayload
    foo: Blob,

    baz: String,
}

@httpBearerAuth
@doNotUseMe1("hi")
@doNotUseMe2("hi")
service MyService2 {
    version: "2020-04-21",
    operations: [HasDigestAuth, HasDigestAndBasicAuth, HasBasicAuth, NoAuth]
}

@trait
@deprecated
string doNotUseMe1

@trait
@deprecated
string doNotUseMe2
