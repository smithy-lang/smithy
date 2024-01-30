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

service MyService1 {
    operations: [
        MyOperation1
    ]
}

service MyService2 {
    operations: [
        MyOperation2
    ]
}

operation MyOperation1 {
    input := {
        foo: MyAbc
    }
}

operation MyOperation2 {
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
