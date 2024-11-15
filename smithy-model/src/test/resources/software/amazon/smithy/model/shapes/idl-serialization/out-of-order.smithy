$version: "2.0"

namespace com.example

string MyString

structure Hello {
    bar: String
    @required
    @length(
        min: 1
    )
    baz: MyString
}

service Foo {
    version: "2006-03-01"
}
