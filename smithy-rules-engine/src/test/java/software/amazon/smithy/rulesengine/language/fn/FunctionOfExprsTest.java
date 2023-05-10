package software.amazon.smithy.rulesengine.language.fn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;

public class FunctionOfExprsTest {

    @Test
    void boolEquals() {
        Expression boolEquals = Expression.of(true).equal(true);
        assertEquals(boolEquals.accept(new ExpectLibFunction()), false);
        assertEquals(boolEquals.accept(new ExpectBuiltInFunction()), "bool");
    }


    @Test
    void stringEquals() {
        Expression stringEquals = Expression.of("asdf").equal("asdf");
        assertEquals(stringEquals.accept(new ExpectLibFunction()), false);
        assertEquals(stringEquals.accept(new ExpectBuiltInFunction()), "string");
    }

    @Test
    void not() {
        Expression not = Expression.of("asdf").equal("asdf").not();
        assertEquals(not.accept(new ExpectLibFunction()), false);
        assertEquals(not.accept(new ExpectBuiltInFunction()), "not");
    }

    static class ExpectLibFunction extends ExpressionVisitor.Default<Boolean> {
        @Override
        public Boolean visitLibraryFunction(FunctionDefinition fn, List<Expression> args) {
            return true;
        }

        @Override
        public Boolean getDefault() {
            return false;
        }
    }

    static class ExpectBuiltInFunction extends ExpressionVisitor.Default<String> {
        @Override
        public String getDefault() {
            return "other";
        }

        @Override
        public String visitBoolEquals(Expression left, Expression right) {
            return "bool";
        }


        @Override
        public String visitStringEquals(Expression left, Expression right) {
            return "string";
        }

        @Override
        public String visitNot(Expression not) {
            return "not";
        }
    }
}
