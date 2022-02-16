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
    @enumValue(int: 1)
    FOO

    /// Docs
    @enumValue(int: 2)
    BAR

    @enumValue(int: 3)
    BAZ
}
