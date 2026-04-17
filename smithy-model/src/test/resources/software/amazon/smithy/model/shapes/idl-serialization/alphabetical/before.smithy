$version: "2.1"

metadata zoo = "test"
metadata foo = "hi"

namespace com.example

service Hij {
    version: "2006-03-01"
}

string Def

structure Abc {
    bar: String
    @length(
        min: 1
    )
    @required
    baz: String
}
