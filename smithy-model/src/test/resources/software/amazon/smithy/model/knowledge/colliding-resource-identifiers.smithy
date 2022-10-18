$version: "2.0"

namespace smithy.example

resource Foo {
    identifiers: {
        bar: String
    }
    read: GetFoo
}

@readonly
operation GetFoo {
    input:= {
        @required
        bar: String

        @resourceIdentifier("bar")
        @required
        bam: String
    }
}
