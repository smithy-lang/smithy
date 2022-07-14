$version: "2.0"

namespace smithy.example

@http(uri: "/", method: "POST")
operation RequiredStreamOperation {
    input: RequiredStream
}

structure RequiredStream {
    @required
    payload: StreamingBlob
}

@http(uri: "/", method: "POST")
operation DefaultStreamOperation {
    input: DefaultStream
}

structure DefaultStream {
    @default("")
    payload: StreamingBlob
}

@http(uri: "/", method: "POST")
operation NeitherRequiredNorDefaultStreamOperation {
    input: NeitherRequiredNorDefaultStream
}

structure NeitherRequiredNorDefaultStream {
    payload: StreamingBlob
}

@streaming
blob StreamingBlob
