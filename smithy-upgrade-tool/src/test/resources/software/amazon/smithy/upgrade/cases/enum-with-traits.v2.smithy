$version: "2.0"

namespace com.example

@internal
enum TraitAfterEnum {
    @enumValue("foo")
    FOO
    @enumValue("bar")
    BAR
}

@internal
enum TraitBeforeEnum {
    @enumValue("foo")
    FOO
    @enumValue("bar")
    BAR
}

@internal()
enum AnnotationTraitWithParens {
    @enumValue("foo")
    FOO
    @enumValue("bar")
    BAR
}
