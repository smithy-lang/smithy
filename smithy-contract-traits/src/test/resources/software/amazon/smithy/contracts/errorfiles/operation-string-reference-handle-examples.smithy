$version: "2"

namespace smithy.example

use smithy.contracts#contracts

service Crypto {
    operations: [Encrypt]
    resources: [Key]
}

resource Key {
    identifiers: {keyId: String}
}

@contracts({
    KeyEnabledOnSuccess: {
        documentation: "A successful Encrypt requires the referenced key to have been ENABLED beforehand"
        expression: "requires(@, resource(before, input.key).properties.keyState == 'ENABLED')"
    }
})
@examples([
    {
        title: "Enabled key succeeds"
        input: {keyRef: "k1"}
        output: {}
        before: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k1"}, properties: {keyState: "ENABLED"}}]}}]}
    }
    {
        title: "Disabled key must not succeed"
        input: {keyRef: "k1"}
        output: {}
        before: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k1"}, properties: {keyState: "DISABLED"}}]}}]}
    }
])
operation Encrypt {
    input: EncryptInput
    output: EncryptOutput
}

@input
structure EncryptInput {
    @required
    keyRef: KeyReference
}

/// The reference is declared directly on the string shape, not on the structure. The
/// string value is the resource's single identifier (keyId), and `name` projects the
/// handle as `input.key`, distinct from the `keyRef` member that carries the value.
@references([{resource: Key, name: "key"}])
string KeyReference

@output
structure EncryptOutput {}
