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
  @sensitive @since("1.1")
  member: String
}

@deprecated @since("1.0")
list F {
  @sensitive
  @since("1.1")
  member: String
}

@deprecated @since("1.0")
list G
{
@sensitive
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
set L {@sensitive @since("1.1") member: String}

@deprecated @since("1.0")
set M {@sensitive @since("1.1") member: String }

@deprecated @since("1.0")
set N
{
@sensitive
@since("1.1")
member: String
}
