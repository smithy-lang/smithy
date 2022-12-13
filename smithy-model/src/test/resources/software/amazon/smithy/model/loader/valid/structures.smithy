namespace com.foo

structure A {}
structure B {
}

structure C
{}

structure D
{
}

structure E     {      }

@deprecated
structure F {}

@deprecated
@documentation("foo")
structure G {}

structure H {
  foo: E
}

structure I {
  foo: E,
  baz: H
}

structure J {
  @deprecated
  foo: E,
  baz: H
}

structure K {
  @deprecated @since("2.0")
  foo: E,
  @deprecated
  baz: H
}

structure L {
  @deprecated @since("2.0") foo: E,
  @deprecated baz: H
}

@documentation("abc")
structure M {
  @deprecated
  @since("2.0")
  foo:E,
  @deprecated
  baz:H
}
