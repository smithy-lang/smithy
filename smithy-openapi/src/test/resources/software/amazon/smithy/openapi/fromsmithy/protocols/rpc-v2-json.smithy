$version: "2.0"

namespace example.smithy

use smithy.protocols#rpcv2Json

@rpcv2Json
@cors
service MyService {
    version: "2018-05-10"
    operations: [echo, beer]
}

operation echo {
    input: EchoInput
    output: EchoOutput
    errors: [DefaultClientExceptionCode, DefaultServerExceptionCode, CustomExceptionCode]
}

operation beer {
    input: BeerInput
    output: BeerOutput
}

structure EchoInput {
    rank: Integer
}

structure EchoOutput {
    value: String
}

structure BeerInput {
    key: String
}

structure BeerOutput {
    string: BigDecimal
}

@error("client")
structure DefaultClientExceptionCode {
    message: String
}

@error("server")
structure DefaultServerExceptionCode {
    message: String
}

@error("client")
@httpError(404)
structure CustomExceptionCode {
    message: String
}
