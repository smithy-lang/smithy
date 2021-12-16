$version: "2.0"

namespace smithy.example

@aws.protocols#restJson1
service PrefixHeaders {
    version: "2018-01-01",
    operations: [PrefixHeadersOperation]
}

@http(method: "GET", uri: "/")
operation PrefixHeadersOperation {
    input: Input,
    output: Output
}

structure Input {
    @httpPrefixHeaders("x-custom-")
    metaData: Headers,
}

structure Output {
    @httpPrefixHeaders("x-custom-")
    metaData: Headers,
}

map Headers {
    key: String,
    value: String
}
