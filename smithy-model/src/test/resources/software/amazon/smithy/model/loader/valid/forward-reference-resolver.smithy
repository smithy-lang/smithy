$version: "2.0"

namespace smithy.example

structure Foo {
  baz: String,
  bar: Integer,
}

integer Integer

list MyList {
  member: String,
}

string MyString

resource MyResource {
  identifiers: {
    a: MyString,      // Not a forward reference
    b: AnotherString, // Forward reference
    c: String,        // Prelude reference
  }
}

string AnotherString
