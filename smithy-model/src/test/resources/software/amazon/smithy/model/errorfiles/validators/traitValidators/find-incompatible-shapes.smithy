$version: "2.0"

namespace com.amazonaws.simple

@trait
@protocolDefinition
@traitValidators(
    "com.amazonaws.simple.myProtocol.NoDocuments": {
        selector: "~> member :test(> document)"
        message: "Document types are not supported"
    }
    "com.amazonaws.simple.myProtocol.NoEventStreams": {
        selector: "~> operation -[input, output]-> :test(> member > union [trait|streaming]))"
        message: "Event streams are not supported"
    }
    "com.amazonaws.simple.myProtocol.NoErrors": {
        selector: "-[operation]-> :not(-[error]->)"
    }
)
structure myProtocol {}

@myProtocol
service MyService {
    operations: [GetFoo]
}

operation GetFoo {
    input := {
        document: Document
    }
    output := {
        stream: Stream
    }
}

@streaming
union Stream {
    a: AEvent
}

structure AEvent {}
