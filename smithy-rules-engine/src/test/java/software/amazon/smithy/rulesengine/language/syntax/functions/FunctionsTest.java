package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.UriEncode;

public class FunctionsTest {
    @Test
    public void booleanEqualsOfExpression() {
        BooleanEquals function = BooleanEquals.ofExpressions(Expression.of(true), Expression.of(true));
        assertThat(function, instanceOf(BooleanEquals.class));
    }

    @Test
    public void getAttrOfExpression() {
        GetAttr function = GetAttr.ofExpressions(
                Expression.getReference(Identifier.of("a"), SourceLocation.none()),
                Expression.of("b"));
        assertThat(function, instanceOf(GetAttr.class));
    }

    @Test
    public void isValidHostLabelOfExpression() {
        IsValidHostLabel function = IsValidHostLabel.ofExpressions(Expression.of("foobar"), Expression.of(true));
        assertThat(function, instanceOf(IsValidHostLabel.class));
    }

    @Test
    public void notOfExpression() {
        Not function = Not.ofExpressions(Expression.of(false));
        assertThat(function, instanceOf(Not.class));

        assertThat(Expression.of(true).not(), instanceOf(Not.class));
    }

    @Test
    public void parseUrlOfExpression() {
        ParseUrl function = ParseUrl.ofExpressions(Expression.of("http://example.com"));
        assertThat(function, instanceOf(ParseUrl.class));
    }

    @Test
    public void stringEqualsOfExpression() {
        StringEquals function = StringEquals.ofExpressions(Expression.of("foo"), Expression.of("foo"));
        assertThat(function, instanceOf(StringEquals.class));
    }

    @Test
    public void substringOfExpression() {
        Substring function = Substring.ofExpressions(
                Expression.of("foobar"),
                Expression.of(1),
                Expression.of(2),
                Expression.of(false));
        assertThat(function, instanceOf(Substring.class));
    }

    @Test
    public void uriEncodeOfExpression() {
        UriEncode function = UriEncode.ofExpressions(Expression.of("foo bar%baz"));
        assertThat(function, instanceOf(UriEncode.class));
    }
}
