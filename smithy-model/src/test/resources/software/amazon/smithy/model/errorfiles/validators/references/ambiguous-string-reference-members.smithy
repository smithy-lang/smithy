$version: "2.0"

namespace ns.foo

resource Key {
    identifiers: {
        keyId: String
    }
}

// Two members target the same named string-reference shape, so both would project the
// handle `key`; the name cannot identify a single member.
@references([{resource: Key, name: "key"}])
string KeyId

structure CopyKeyInput {
    @required
    source: KeyId

    @required
    destination: KeyId
}
