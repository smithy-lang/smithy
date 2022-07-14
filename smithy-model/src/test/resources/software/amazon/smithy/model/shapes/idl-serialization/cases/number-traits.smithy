$version: "2.0"

namespace ns.foo

@trait
bigDecimal BigDecimalTrait

@trait
bigInteger BigIntegerTrait

@trait
byte ByteTrait

@trait
double DoubleTrait

@trait
float FloatTrait

@trait
integer IntegerTrait

@trait
long LongTrait

@trait
short ShortTrait

@BigDecimalTrait(3.14)
@BigIntegerTrait(123)
@ByteTrait(123)
@DoubleTrait(1.234)
@FloatTrait(-1.234)
@IntegerTrait(-123)
@LongTrait(321)
@ShortTrait(12321)
string Foo
