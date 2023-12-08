$version: "2.0"

namespace ns.foo

service MyService {
    version: "2017-01-17"
    operations: [
        A
        B
    ]
    errors: [
        CommonError1
        CommonError2
    ]
}

@readonly
operation A {
    input: Unit
    output: Unit
}

@readonly
operation B {
    input: Input
    output: Output
    errors: [
        Error1
        Error2
    ]
}

operation C {
    input: Input
    output: Output,
    errors: [
        CommonError1
    ]
}

@error("server")
structure CommonError1 {}

@error("server")
structure CommonError2 {}

@error("client")
structure Error1 {}

@error("server")
structure Error2 {}

@error("client")
structure UnusedError {}

structure Input {}

structure Output {}
