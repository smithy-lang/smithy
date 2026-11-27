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

// The reference omits `ids`, so each resource identifier binds to the member of the same
// name (keyId). The handle's ids must be {keyId: input.keyId}, not empty: an empty ids
// would match the first instance in the world regardless of the input.
@contracts({
    KeyEnabledOnSuccess: {
        documentation: "A successful Encrypt requires the referenced key to have been ENABLED beforehand"
        expression: "requires(@, resource(before, input.key).properties.keyState == 'ENABLED')"
    }
})
@examples([
    {
        title: "Referenced key present and enabled"
        input: {keyId: "k1"}
        output: {}
        before: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k1"}, properties: {keyState: "ENABLED"}}]}}]}
    }
    {
        title: "Referenced key absent must fail"
        input: {keyId: "k1"}
        output: {}
        before: {"smithy.example#Crypto": [{resources: {"smithy.example#Key": [{identifiers: {keyId: "k2"}, properties: {keyState: "ENABLED"}}]}}]}
    }
])
operation Encrypt {
    input: EncryptInput
    output: EncryptOutput
}

@input
@references([
    {resource: Key, name: "key"}
])
structure EncryptInput {
    @required
    keyId: String
}

@output
structure EncryptOutput {}
