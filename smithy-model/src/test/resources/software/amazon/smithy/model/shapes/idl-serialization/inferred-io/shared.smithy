$version: "2.1"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace com.example

operation NotShared {
    input: NotSharedInput
    output: NotSharedOutput
}

operation SharedCustomA {
    input := {}
    output := {}
}

operation SharedCustomB {
    input := {}
    output := {}
}

@input
structure NotSharedInput {}

@output
structure NotSharedOutput {}
