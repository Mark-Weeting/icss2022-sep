package nl.han.ica.icss.parser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.Operation;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

class ExpressionParserTest {

    private Expression parseExpression(String expression) {
        ICSSLexer lexer = new ICSSLexer(CharStreams.fromString("p { width: " + expression + "; }"));
        ICSSParser parser = new ICSSParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new BailErrorStrategy());

        ASTListener listener = new ASTListener();
        new ParseTreeWalker().walk(listener, parser.stylesheet());

        Stylerule rule = (Stylerule) listener.getAST().root.body.get(0);
        return ((Declaration) rule.body.get(0)).expression;
    }

    // This method checks the type and casts the type.
    private static <T> T assertType(Class<T> expectedType, Object actual) {
        assertTrue(expectedType.isInstance(actual),
                "Expected " + expectedType.getSimpleName() + " but was " + actual.getClass().getSimpleName());
        return expectedType.cast(actual);
    }

    @Test
    void subtractionIsParsedAsSubtractOperation() {
        Expression expression = parseExpression("10px - 2px");

        assertType(SubtractOperation.class, expression);
    }

    @Test
    void multiplicationBindsStrongerThanAddition() {
        // 2 + 3 * 4px  =>  2 + (3 * 4px)
        Operation add = assertType(AddOperation.class, parseExpression("2 + 3 * 4px"));

        assertType(ScalarLiteral.class, add.lhs);
        assertType(MultiplyOperation.class, add.rhs);
    }

    @Test
    void multiplicationOnLeftBindsStrongerThanSubtraction() {
        // 3 * 4px - 2px  =>  (3 * 4px) - 2px
        Operation subtract = assertType(SubtractOperation.class, parseExpression("3 * 4px - 2px"));

        assertType(MultiplyOperation.class, subtract.lhs);
        assertType(PixelLiteral.class, subtract.rhs);
    }

    @Test
    void subtractionIsLeftAssociative() {
        // 10px - 2px - 1px  =>  (10px - 2px) - 1px
        Operation outer = assertType(SubtractOperation.class, parseExpression("10px - 2px - 1px"));

        assertType(SubtractOperation.class, outer.lhs);
        assertEquals(1, ((PixelLiteral) outer.rhs).value);
    }

    @Test
    void mixedAdditionAndSubtractionGoLeftToRight() {
        // 10px - 2px + 1px  =>  (10px - 2px) + 1px
        Operation outer = assertType(AddOperation.class, parseExpression("10px - 2px + 1px"));

        assertType(SubtractOperation.class, outer.lhs);
    }

    @Test
    void variableReferenceCanBeUsedInOperation() {
        Operation add = assertType(AddOperation.class, parseExpression("Width + 2px"));

        assertEquals("Width", ((VariableReference) add.lhs).name);
    }
}
