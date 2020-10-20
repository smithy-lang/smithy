namespace smithy.example

structure Foo {
    // This is fine.
    @sensitive
    bar: String,

    // This should warn.
    @sensitive
    baz: Baz,

    // This is fine.
    bam: SensitiveString,

    // This is fine.
    boo: Boo,

    // This should warn for the redundant member trait.
    @sensitive
    qux: Boo
}

structure Baz {}

@sensitive
structure Boo {}

@sensitive
string SensitiveString
