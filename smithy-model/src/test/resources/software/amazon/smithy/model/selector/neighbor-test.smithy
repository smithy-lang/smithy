$version: "2"

namespace smithy.example

@trait
@deprecated
structure myTrait {}

@myTrait
string MyString

@trait(selector: "service")
@protocolDefinition
structure myProtocol {}

@myProtocol
service MyService1 {
    version: "2020-01-01"
}

service MyService2 {
    version: "2020-01-01",
    operations: [Operation],
    resources: [MyResource]
}

operation Operation {
    input: Input,
    output: Output,
    errors: [Error]
}

structure Input {
  foo: smithy.api#String,
}

structure Output {
  foo: smithy.api#String,
}

@error("client")
structure Error {
  foo: smithy.api#String,
}

resource MyResource {
    read: GetMyResource
    delete: DeleteMyResource
}

@readonly
operation GetMyResource {}

@idempotent
operation DeleteMyResource {}
