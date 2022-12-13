$version: "2"

namespace smithy.example

service HttpService {
    version: "2020-04-21",
    operations: [HasHttp1, HasHttp2, NoHttp]
}

@http(uri: "/", method: "GET")
operation HasHttp1 {}

@http(uri: "/", method: "HEAD")
operation HasHttp2 {}

operation NoHttp {}
