namespace example.weather

structure Foo {
    /// Docs 1
    optional: String

    /// Docs 2
    requiredRelative: String!

    /// Docs
    requiredAbsolute: smithy.api#String!
}

structure FooNoDocs {
    optional: String
    requiredRelative: String!
    requiredAbsolute: smithy.api#String!
}

structure FooNoDocsWithCommas {
    optional: String,
    requiredRelative: String!,
    requiredAbsolute: smithy.api#String!,
}

structure RequiredConflictsAreIgnored {
    @required
    required: String!,
}
