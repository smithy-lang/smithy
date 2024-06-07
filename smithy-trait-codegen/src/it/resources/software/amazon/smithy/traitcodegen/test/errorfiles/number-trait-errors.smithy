$version: "2.0"

namespace test.smithy.traitcodegen.numbers

// Should fail to use string input for number traits
@IntegerTrait("bad")
@FloatTrait("bad")
@LongTrait("bad")
@ShortTrait("bad")
@DoubleTrait("bad")
structure structWithInvalidStringInput {}
