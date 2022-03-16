namespace com.example

string String

// ----- lists ------

list A {member: smithy.api#String}
list B {member:String}

@deprecated
list C {
  member: String
}

@deprecated
@since("1.0")
list D {
  member: String,
}

@deprecated
@since("1.0")
list E {
  @internal @since("1.1")
  member: String
}

@deprecated @since("1.0")
list F {
  @internal
  @since("1.1")
  member: String
}

@deprecated @since("1.0")
list G
{
@internal
@since("1.1")
member: String
}

//---- sets -----

set H {member: smithy.api#String}
set I{member:String}

@deprecated
set J {
  member: String,
}

@deprecated
@since("1.0")
set K {
 member: String
}

@deprecated
@since("1.0")
set L {@internal @since("1.1") member: String}

@deprecated @since("1.0")
set M {@internal @since("1.1") member: String }

@deprecated @since("1.0")
set N
{
@internal
@since("1.1")
member: String
}
