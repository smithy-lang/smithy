$version: "2"

namespace smithy.example

operation Operation {
    input: Input,
    output: Output
}

structure Input {
  foo: String,
}

structure Output {
  foo: String,
}
