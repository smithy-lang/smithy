$version: "2"

namespace smithy.example

use smithy.contracts#contracts

service Crypto {
    operations: [Encrypt]
    resources: [Key]
}

// A second service also holds the referenced resource. The reference has no explicit
// `service`, so the projected handle has none either. When both services appear in the
// `before` snapshot, resource() cannot choose one and the contract cannot be evaluated.
service Admin {
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
        title: "Two services in the snapshot cannot be disambiguated"
        input: {keyRef: "k1"}
        output: {}
        before: {
            "smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k1"}, properties: {keyState: "ENABLED"}}]}}]
            "smithy.example#Admin": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k1"}, properties: {keyState: "ENABLED"}}]}}]
        }
    }
])
operation Encrypt {
    input: EncryptInput
    output: EncryptOutput
}

@input
@references([
    {resource: Key, name: "key", ids: {keyId: "keyRef"}}
])
structure EncryptInput {
    @required
    keyRef: String
}

@output
structure EncryptOutput {}
