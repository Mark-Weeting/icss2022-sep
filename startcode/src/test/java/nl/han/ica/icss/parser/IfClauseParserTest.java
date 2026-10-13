package nl.han.ica.icss.parser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.IfClause;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.VariableAssignment;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.BoolLiteral;

/**
 * IfClause.equals only compares the else-clause when the if-clause has one, so
 * the tests do not catch every mistake. These tests check if/else explicitly.
 */
class IfClauseParserTest {

    private Stylerule parseRule(String rule) {
        ICSSLexer lexer = new ICSSLexer(CharStreams.fromString(rule));
        ICSSParser parser = new ICSSParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new BailErrorStrategy());

        ASTListener listener = new ASTListener();
        new ParseTreeWalker().walk(listener, parser.stylesheet());

        return (Stylerule) listener.getAST().root.body.get(0);
    }

    @Test
    void ifWithoutElseHasNoElseClause() {
        Stylerule rule = parseRule("p { if [TRUE] { width: 10px; } }");
        IfClause ifClause = (IfClause) rule.body.get(0);

        assertTrue(ifClause.conditionalExpression instanceof BoolLiteral);
        assertEquals(1, ifClause.body.size());
        assertNull(ifClause.elseClause);
    }

    @Test
    void elseBodyEndsUpInElseClause() {
        Stylerule rule = parseRule("p { if [Flag] { width: 10px; } else { width: 20px; height: 5px; } }");
        IfClause ifClause = (IfClause) rule.body.get(0);

        assertEquals("Flag", ((VariableReference) ifClause.conditionalExpression).name);
        assertEquals(1, ifClause.body.size());
        assertNotNull(ifClause.elseClause);
        assertEquals(2, ifClause.elseClause.body.size());
    }

    @Test
    void elseBelongsToInnerIfWhenNested() {
        Stylerule rule = parseRule("p { if [A] { if [B] { width: 1px; } else { width: 2px; } } }");
        IfClause outer = (IfClause) rule.body.get(0);
        IfClause inner = (IfClause) outer.body.get(0);

        assertNull(outer.elseClause);
        assertNotNull(inner.elseClause);
    }

    @Test
    void variableAssignmentAllowedInsideIf() {
        Stylerule rule = parseRule("p { if [A] { W := 10px; width: W; } }");
        IfClause ifClause = (IfClause) rule.body.get(0);

        assertTrue(ifClause.body.get(0) instanceof VariableAssignment);
        assertTrue(ifClause.body.get(1) instanceof Declaration);
    }

    @Test
    void ifOutsideRuleIsSyntaxError() {
        assertThrows(ParseCancellationException.class,
                () -> parseRule("if [TRUE] { p { width: 10px; } }"));
    }
}
