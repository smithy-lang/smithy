$version: "2.0"

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
    input: WithoutTraitsRequest
    output: WithoutTraitsResponse
}

operation WithTraits {
    input: WithTraitsRequest
    output: WithTraitsResponse
}

structure MultipleBindRequest {}

structure MultipleBindResponse {}

structure WithoutTraitsRequest {}

structure WithoutTraitsResponse {}

@input
structure WithTraitsRequest {
    a: String
}

@output
structure WithTraitsResponse {}
