$version: "2.0"

namespace smithy.example

string MyString

structure Foo {
    foo: MyString,
    bar: Bar,
}

structure Bar {
    baz: Integer,
    bam: BamList,
}

list BamList {
    member: MyString
}

structure Recursive {
    a: RecursiveList,
    b: Recursive,
}

list RecursiveList {
    member: Recursive,
}
