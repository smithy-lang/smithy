$version: "2.0"

namespace smithy.example

use aws.protocols#awsJson1_1
use aws.protocols#restXml

@awsJson1_1
@restXml
service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#BadGreeting": "ThisDoesNotWork"
        "smithy.example#ServerError": "ServerDown"
        "smithy.example#Language": "LanguageSettings"
    }
}

operation SayHello {
    input: SayHelloInput,
    output: SayHelloOutput,
    errors: [BadGreeting, ServerError]
}

@input
structure SayHelloInput {
    language: Language
}

@output
structure SayHelloOutput {}

@error("client")
structure BadGreeting {}

@error("server")
structure ServerError {}

structure Language {}
