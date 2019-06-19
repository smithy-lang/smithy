namespace com.foo

trait foo {
  shape: String,
  selector: "*"
}

// A null trait value cannot be coerced to a string.
@foo
string MyString
