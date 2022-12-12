namespace foo.alpha

@deprecated
string DeprecatedString1

@smithy.api#deprecated
string DeprecatedString2

@trait(selector: "*")
structure deprecated {

}

@deprecated
string DeprecatedString3

