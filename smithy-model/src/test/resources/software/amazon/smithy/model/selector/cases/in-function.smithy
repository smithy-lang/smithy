$version: "2.0"

metadata selectorTests = [
    // Find numbers that are used in input but are not used in output.
    {
        selector: """
            service
            $output(~> operation -[output]-> ~> number)
            ~> operation -[input]-> ~> number
            :not(:in(${output}))"""
        matches: [
            smithy.api#Integer
            smithy.api#Double
        ]
    }
    // This is not how you should write this expression, but it does test that :in can be used with variables.
    // This should instead be written as:
    //     operation ~> number
    {
        selector: """
            $usedNumbers(operation ~> number)
            operation ~> *
            :in(${usedNumbers})"""
        matches: [
            smithy.api#Integer
            smithy.api#Float
            smithy.api#Double
            smithy.api#Short
            smithy.api#Long
            smithy.api#Byte
        ]
    }
    // This is also not how you'd write this, but it is valid.
    // This should be written more directly as:
    //     member [id|namespace = smithy.example] > number
    {
        selector: ":in(number :test(< member [id|namespace = smithy.example]))"
        matches: [
            smithy.api#Integer
            smithy.api#Float
            smithy.api#Double
            smithy.api#Short
            smithy.api#Long
            smithy.api#Byte
        ]
    }
]

namespace smithy.example

service MyService {
    operations: [A, B]
}

operation A {
    input:= {
        a: Integer
        b: Float
        c: Double
    }
    output:= {
        a: Short
        b: Long
        c: Float
    }
}

operation B {
    input:= {
        a: Byte
    }
    output:= {
        b: Byte
    }
}
