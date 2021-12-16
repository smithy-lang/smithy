$version: "2.0"

namespace smithy.api

@deprecated
boolean PrimitiveBoolean

@deprecated
byte PrimitiveByte

@deprecated
short PrimitiveShort

@deprecated
integer PrimitiveInteger

@deprecated
long PrimitiveLong

@deprecated
float PrimitiveFloat

@deprecated
double PrimitiveDouble

/// Indicates that a shape is boxed.
///
/// When a boxed shape is the target of a member, the member
/// may or may not contain a value, and the member has no default value.
@trait(selector: """
    :test(boolean, byte, short, integer, long, float, double,
          member > :test(boolean, byte, short, integer, long, float, double))""")
@tags(["diff.error.const"])
structure box {}
