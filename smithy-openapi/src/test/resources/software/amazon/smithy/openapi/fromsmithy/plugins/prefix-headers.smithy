namespace smithy.example

@protocols(aws.rest-json: {})
service PrefixHeaders {
    version: "2018-01-01",
    operations: [PrefixHeadersOperation]
}

@http(method: GET, uri: "/")
operation PrefixHeadersOperation(Input) -> Output

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
    value: StringList,
}

list StringList {
    member: String
}
