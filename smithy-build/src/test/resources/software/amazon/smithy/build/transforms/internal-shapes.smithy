namespace smithy.example

@internal
structure InternalStructure {
    foo: String
}

structure ExternalStructure {
    @internal
    internal: String

    external: String
}
