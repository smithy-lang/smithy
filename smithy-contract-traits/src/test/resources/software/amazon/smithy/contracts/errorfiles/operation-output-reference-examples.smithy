$version: "2"

namespace smithy.example

use smithy.contracts#contracts

service Crypto {
    operations: [GenerateKey]
    resources: [Key]
}

resource Key {
    identifiers: {keyId: String}
}

@contracts({
    ResultKeyEnabled: {
        documentation: "On a successful GenerateKey the returned key is ENABLED in the resulting world"
        expression: "requires(@, resource(after, output.key).properties.keyState == 'ENABLED')"
    }
})
@examples([
    {
        title: "Generated key is enabled"
        input: {}
        output: {keyRef: "k1"}
        after: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k1"}, properties: {keyState: "ENABLED"}}]}}]}
    }
    {
        title: "Generated key must not be disabled"
        input: {}
        output: {keyRef: "k1"}
        after: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k1"}, properties: {keyState: "DISABLED"}}]}}]}
    }
])
operation GenerateKey {
    input: GenerateKeyInput
    output: GenerateKeyOutput
}

@input
structure GenerateKeyInput {}

// The reference is on the OUTPUT structure, so the handle projects as output.key. The
// contract can only read it because the call node projects handles for output as well as
// input; otherwise output.key would be null and both examples would fail.
@output
@references([
    {resource: Key, name: "key", ids: {keyId: "keyRef"}}
])
structure GenerateKeyOutput {
    @required
    keyRef: String
}
