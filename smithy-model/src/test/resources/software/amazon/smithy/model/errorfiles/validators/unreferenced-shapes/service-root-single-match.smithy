$version: "2.0"

metadata validators = [
    {
        name: "UnreferencedShape"
        configuration: {
            rootShapeSelector: "service"
        }
    }
]

namespace smithy.example

service MyService {
    operations: [
        MyOperation
    ]
}

operation MyOperation {
    input := {
        foo: MyAbc
    }
    output := {
        bar: MyDef
    }
}

structure MyAbc {
    MyString: MyString
}

string MyString

string MyDef

string Baz

string Bam
