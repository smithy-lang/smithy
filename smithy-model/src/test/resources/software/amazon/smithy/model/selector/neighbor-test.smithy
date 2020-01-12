namespace smithy.example

operation Operation {
    input: Input,
    output: Output,
    errors: [Error]
}

structure Input {
  foo: smithy.api#String,
}

structure Output {
  foo: smithy.api#String,
}

@error("client")
structure Error {
  foo: smithy.api#String,
}
