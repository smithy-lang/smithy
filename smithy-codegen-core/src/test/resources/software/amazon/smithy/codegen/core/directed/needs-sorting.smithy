$version: "1.0"

namespace smithy.example

service Foo {
    operations: [FooOperation]
}

operation FooOperation {
    input: FooOperationInput,
    output: FooOperationOutput
}

@input
structure FooOperationInput {
    recursive: RecursiveA
}

@output
structure FooOperationOutput {
    a: A
}

structure C {
    d: D
}

structure B {
    c: C
}

structure D {
    e: String
}

structure A {
    b: B
}

structure RecursiveA {
    a: A,
    b: RecursiveB
}

structure RecursiveB {
    a: RecursiveA,
    b: B
}
