$version: "2"

namespace smithy.example

use smithy.contracts#contracts

service Crypto {
    operations: [Encrypt]
    resources: [Key]
}

resource Key {
    identifiers: {id: String}
}

@contracts({
    KeyEnabledOnSuccess: {
        documentation: "A successful Encrypt requires the referenced key to have been ENABLED beforehand"
        expression: "requires(@, resource(before, {service: 'smithy.example#Crypto', resource: 'smithy.example#Key', ids: {id: input.keyId}}).properties.keyState == 'ENABLED')"
    }
})
@examples([
    {
        title: "Enabled key succeeds"
        input: {keyId: "k1"}
        output: {}
        before: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {id: "k1"}, properties: {keyState: "ENABLED"}}]}}]}
    }
    {
        title: "Disabled key must not succeed"
        input: {keyId: "k1"}
        output: {}
        before: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {id: "k1"}, properties: {keyState: "DISABLED"}}]}}]}
    }
])
operation Encrypt {
    input: EncryptInput
    output: EncryptOutput
}

@input
structure EncryptInput {
    @required
    keyId: String
}

@output
structure EncryptOutput {}
