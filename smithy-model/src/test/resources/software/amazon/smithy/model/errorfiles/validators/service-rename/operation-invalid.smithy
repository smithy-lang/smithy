namespace smithy.example

service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#SayHello": "SayHello2"
    }
}

operation SayHello {}
