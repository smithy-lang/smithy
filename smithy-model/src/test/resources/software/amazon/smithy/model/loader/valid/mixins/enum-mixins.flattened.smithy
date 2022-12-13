$version: "2.0"

namespace smithy.example

/// Mixed enum
@sensitive
@private
enum MixedEnum {
    FOO
    /// Docs
    BAR
    BAZ
}

/// Mixed int enum
@sensitive
@private
intEnum MixedIntEnum {
    @enumValue(1)
    FOO

    /// Docs
    @enumValue(2)
    BAR

    @enumValue(3)
    BAZ
}
