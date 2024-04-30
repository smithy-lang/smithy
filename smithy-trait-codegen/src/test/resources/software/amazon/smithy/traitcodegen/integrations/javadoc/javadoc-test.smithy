$version: "2"

namespace com.example.javadoc

/// Basic class-level documentation
@trait
structure DocumentationWrapping {
    /// This is a long long docstring that should be wrapped. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
    shouldBeWrapped: String,

    /// Documentation includes preformatted text that should not be messed with. This sentence should still be partially wrapped.
    /// For example:
    ///
    /// <pre>
    /// Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
    /// </pre>
    ///
    /// <ul>
    ///     <li> Lorem ipsum dolor sit amet, consectetur adipiscing elit Lorem ipsum dolor sit amet, consectetur adipiscing elit consectetur adipiscing </li>
    ///     <li> Lorem ipsum dolor sit amet, consectetur adipiscing elit Lorem ipsum dolor sit amet, consectetur adipiscing elit consectetur adipiscing </li>
    /// </ul>
    preformattedText: String
}

@trait
@deprecated(message: "A message", since: "yesterday")
structure DeprecatedStructure {
    @deprecated(message: "A message", since: "yesterday")
    deprecatedMember: String,

    /// Has docs in addition to deprecated
    @deprecated(message: "A message", since: "yesterday")
    deprecatedWithDocs: String
}

@trait
@since("1.2")
structure SinceStructure {
    @since("1.2")
    sinceMember: String
}

@trait
@externalDocumentation(
    Example: "https://example.com",
    Example2: "https://example.com"
)
structure ExternalDocumentation {
    @externalDocumentation(Example: "https://example.com")
    memberWithExternalDocumentation: String
}


/// This structure applies all documentation traits
@trait
@unstable
@deprecated(since: "sometime")
@externalDocumentation(Example: "https://example.com")
@since("4.5")
structure Rollup {
    /// This member applies all documentation traits
    @unstable
    @deprecated(since: "sometime")
    @externalDocumentation(Example: "https://example.com")
    @since("4.5")
    rollupMember: String
}

/// This enum applies all documentation traits to its variants
@trait
enum EnumVariantsTest {
    /// Just a documented variant
    @unstable
    @deprecated(message: "Really. Dont use this.")
    @externalDocumentation(Example: "https://example.com")
    @since("4.5")
    A,
    B
}

