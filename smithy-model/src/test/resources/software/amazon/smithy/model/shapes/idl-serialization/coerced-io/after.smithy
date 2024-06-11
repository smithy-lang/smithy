$version: "2.0"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace com.example

service InlineService {
    operations: [
        MultipleBind1
        MultipleBind2
        WithoutTraits
        WithTraits
    ]
}

operation MultipleBind1 {
    input: MultipleBindRequest
    output: MultipleBindResponse
}

operation MultipleBind2 {
    input: MultipleBindRequest
    output: MultipleBindResponse
}

operation WithoutTraits {
    input := {}
    output := {}
}

operation WithTraits {
    input := {
        a: String
    }
    output := {}
}

structure MultipleBindRequest {}

structure MultipleBindResponse {}
