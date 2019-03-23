namespace foo.alpha

@hello
string MyString1

@foo.alpha#hello
string MyString2

@foo.beta#hello
string MyString3

@foo.gamma#hello
string MyString4

trait hello {
  selector: "*"
}


namespace foo.beta

@hello
string MyString1

@foo.alpha#hello
string MyString2

@foo.beta#hello
string MyString3

@foo.gamma#hello
string MyString4

trait hello {
  selector: "*"
}


namespace foo.gamma

@hello
string MyString1

@foo.alpha#hello
string MyString2

@foo.beta#hello
string MyString3

@foo.gamma#hello
string MyString4

trait hello {
  selector: "*"
}
