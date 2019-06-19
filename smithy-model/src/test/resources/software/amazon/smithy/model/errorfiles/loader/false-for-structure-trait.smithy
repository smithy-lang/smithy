namespace com.foo

trait foo {
  shape: Struct,
  selector: "*"
}

structure Struct {}

// This is invalid
@foo(false)
string MyString1

// This is valid
@foo(true)
string MyString2
