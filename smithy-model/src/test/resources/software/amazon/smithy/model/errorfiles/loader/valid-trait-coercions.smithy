namespace com.foo

trait a {
  shape: StringList,
  selector: "*"
}

trait b {
  shape: StringSet,
  selector: "*"
}

trait c {
  shape: EmptyStruct,
  selector: "*"
}

trait d {
  shape: StringMap,
  selector: "*"
}

trait e {
  selector: "*"
}

list StringList {
  member: String
}

set StringSet {
  member: String
}

structure EmptyStruct {}

map StringMap {
  key: String,
  value: String,
}

@a @b @c @d @e
structure Test {}
