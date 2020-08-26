namespace smithy.example.other

operation Hello2 {
    input: Hello2Input,
}

structure Hello2Input {
    @required
    x: X,

    @required
    s: String, // this should resolve to smithy.api#String
}

string X

@readonly
operation GetHello2 {
    input: Hello2Input,
}
