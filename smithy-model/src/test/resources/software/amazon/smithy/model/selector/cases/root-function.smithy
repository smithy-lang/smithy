$version: "2.0"

metadata selectorTests = [
    // Find shapes used in service operation inputs but not operation outputs.
    {
        selector: """
            number
            :in(:root(service ~> operation -[input]-> * ~> number))
            :not(:in(:root(service ~> operation -[output]-> * ~> number)))"""
        matches: [
            smithy.api#Integer
        ]
    }
    // This is similar to the above :root example, but also returns smithy.api#Double because each capture
    // of service operations is isolated to a single service and not global across all services.
    // * When MyService1 is evaluated, Double is only used in input and not output.
    // * The usage of Double in the output of MyService2 is not taken into account when evaluating MyService1.
    {
        selector: """
            service
            $outputs(~> operation -[output]-> ~> number)
            ~> operation -[input]-> ~> number
            :not(:in(${outputs}))"""
        matches: [
            smithy.api#Integer
            smithy.api#Double
        ]
    }
]

namespace smithy.example

service MyService1 {
    operations: [A]
}

operation A {
    input:= {
        a: Integer
        b: Float
        c: Double
    }
    output:= {
        d: Float
        e: Short
        f: Long
    }
}

service MyService2 {
    operations: [B]
}

operation B {
    input:= {
        a: Double
    }
    output:= {
        a: Double
    }
}
