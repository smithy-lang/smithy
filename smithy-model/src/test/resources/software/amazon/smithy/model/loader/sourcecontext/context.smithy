namespace example.smithy

structure Foo {
  bar: String,
}

/// Docs
@deprecated
structure Baz {
  /// Hello!
  @recommended
  bam: String,
}

apply Foo @documentation("applied")
