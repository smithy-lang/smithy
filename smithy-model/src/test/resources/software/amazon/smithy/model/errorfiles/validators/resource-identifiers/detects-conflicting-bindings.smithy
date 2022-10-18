$version: "2.0"

namespace smithy.example

resource Foo {
    identifiers: {
        bar: String
    }
    read: GetFoo
    put: PutFoo
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

@idempotent
operation PutFoo {
    input:= {
    @required
        @resourceIdentifier("bar")
        @required
        a: String

        @resourceIdentifier("bar")
        @required
        b: String
    }
}
