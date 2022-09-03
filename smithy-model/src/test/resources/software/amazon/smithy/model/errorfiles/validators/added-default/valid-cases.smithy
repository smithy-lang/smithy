$version: "2.0"

namespace smithy.example

structure Foo {
    @addedDefault
    bar: Integer = 0
}

structure Baz {
    @addedDefault
    @required
    bar: Integer = 0
}

structure Bar {
    @addedDefault
    @required
    bar: Integer = 100
}

structure Bam {
    @addedDefault
    @required
    bar: String = ""
}
