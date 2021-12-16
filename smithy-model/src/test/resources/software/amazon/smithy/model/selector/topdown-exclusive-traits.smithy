$version: "2"

namespace smithy.example

@trait
structure a {}

@trait
structure b {}

@a
service Service1 {
    version: "2020-08-22",
    operations: [O1, O2],
    resources: [R1]
}

@b
operation O1 {}

operation O2 {}

resource R1 {
    resources: [R2],
    operations: [O3]
}

@b
operation O3 {}

@b
resource R2 {
    operations: [O4]
}

operation O4 {}
