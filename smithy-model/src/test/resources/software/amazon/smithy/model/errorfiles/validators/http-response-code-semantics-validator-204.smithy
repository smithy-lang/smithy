$version: "2.0"

namespace smithy.example

@http(uri: "/invalid204", method: "GET", code: 204)
@readonly
operation InvalidStatusCode204 {
    output: InvalidStatusCode204Output,
}

@output
structure InvalidStatusCode204Output {
    bad: String
}


@http(uri: "/invalid205", method: "GET", code: 205)
@readonly
operation InvalidStatusCode205 {
    output: InvalidStatusCode205Output,
}

@output
structure InvalidStatusCode205Output {
    bad: String
}
