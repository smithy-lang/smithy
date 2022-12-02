$version: "2.0"

namespace smithy.example

resource Foo {
    identifiers: {
        bar: String
    }
    read: GetFoo
    put: PutFoo
    delete: DeleteFoo
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
        @resourceIdentifier("bar")
        @required
        a: String

        @resourceIdentifier("bar")
        @required
        b: String
    }
}

@idempotent
operation DeleteFoo {
    input:= {
        @required
        bar: String

        @resourceIdentifier("bar")
        @required
        a: String

        @resourceIdentifier("bar")
        @required
        b: String
    }
}
