$version: "2.x"

namespace com.example

@internal
enum TraitAfterEnum {
    FOO = "foo"
    BAR = "bar"
}

@internal
enum TraitBeforeEnum {
    FOO = "foo"
    BAR = "bar"
}

@internal()
enum AnnotationTraitWithParens {
    FOO = "foo"
    BAR = "bar"
}
