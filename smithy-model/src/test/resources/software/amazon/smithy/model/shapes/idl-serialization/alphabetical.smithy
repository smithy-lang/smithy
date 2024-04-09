$version: "2.0"

metadata foo = "hi"
metadata zoo = "test"

namespace com.example

structure Abc {
    bar: String
    @length(
        min: 1
    )
    @required
    baz: String
}

string Def

service Hij {
    version: "2006-03-01"
}
