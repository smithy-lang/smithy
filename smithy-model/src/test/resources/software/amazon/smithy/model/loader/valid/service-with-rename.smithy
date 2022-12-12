namespace smithy.example

service MyService {
    version: "2017-02-11"
    operations: [GetSomething]
    rename: {
        "foo.example#Widget": "FooWidget"
    }
}

operation GetSomething {
    output: GetSomethingOutput
}

structure GetSomethingOutput {
    widget1: Widget
    fooWidget: foo.example#Widget
}

structure Widget {

}

