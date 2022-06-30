$version: "2.0"

namespace com.foo

@trait(selector: "*")
list a {
  member: String,
}

@trait
structure b {}

@trait(selector: "*")
map c {
  key: String,
  value: String,
}

@trait(selector: "*")
structure d {}

@trait
structure e {}

@a
@b
@c
@d
@e()
structure Test {}
