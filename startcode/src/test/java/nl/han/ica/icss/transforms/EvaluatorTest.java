package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.checker.Checker;
import nl.han.ica.icss.generator.Generator;
import nl.han.ica.icss.parser.ASTListener;
import nl.han.ica.icss.parser.ICSSLexer;
import nl.han.ica.icss.parser.ICSSParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {

    // Parses, checks and evaluates the input, just like the GUI does.
    private AST evaluate(CharStream input) {
        ICSSParser parser = new ICSSParser(new CommonTokenStream(new ICSSLexer(input)));
        parser.setErrorHandler(new BailErrorStrategy());

        ASTListener listener = new ASTListener();
        new ParseTreeWalker().walk(listener, parser.stylesheet());
        AST ast = listener.getAST();

        new Checker().check(ast);
        assertTrue(ast.getErrors().isEmpty(), "Input has errors: " + ast.getErrors());

        new Evaluator().apply(ast);
        return ast;
    }

    private AST evaluate(String icss) {
        return evaluate(CharStreams.fromString(icss));
    }

    private String evaluateToCss(String icss) {
        return new Generator().generate(evaluate(icss));
    }

    @Test
    void level2GivesExpectedCss() throws IOException {
        String expected =
                "p {\n" +
                "  background-color: #ffffff;\n" +
                "  width: 500px;\n" +
                "}\n" +
                "\n" +
                "a {\n" +
                "  color: #ff0000;\n" +
                "}\n" +
                "\n" +
                "#menu {\n" +
                "  width: 520px;\n" +
                "}\n" +
                "\n" +
                ".menu {\n" +
                "  color: #000000;\n" +
                "}\n";

        AST ast = evaluate(CharStreams.fromStream(getClass().getClassLoader().getResourceAsStream("level2.icss")));

        assertEquals(expected, new Generator().generate(ast));
    }

    @Test
    void exampleFromAssignmentIs68px() {
        assertEquals("div {\n  width: 68px;\n}\n", evaluateToCss("div { width: 50px + 2 * 10px - 2px; }"));
    }

    @Test
    void subtractionGoesFromLeftToRight() {
        assertEquals("p {\n  width: 7px;\n}\n", evaluateToCss("p { width: 10px - 2px - 1px; }"));
    }

    @Test
    void multiplyingPercentageWithScalars() {
        assertEquals("p {\n  width: 432%;\n}\n", evaluateToCss("p { width: 4% * 18 * 2 * 3; }"));
    }

    @Test
    void scalarOnTheRightKeepsUnitOfLeftSide() {
        assertEquals("p {\n  width: 30px;\n}\n", evaluateToCss("p { width: 10px * 3; }"));
    }

    @Test
    void variableCanUseOtherVariable() {
        assertEquals("p {\n  width: 20px;\n}\n", evaluateToCss("A := 10px; B := A * 2; p { width: B; }"));
    }

    @Test
    void variableInsideRuleIsUsed() {
        assertEquals("p {\n  width: 15px;\n}\n", evaluateToCss("p { W := 5px; width: W + 10px; }"));
    }

    @Test
    void colorVariableIsFilledIn() {
        assertEquals("p {\n  color: #123456;\n}\n", evaluateToCss("C := #123456; p { color: C; }"));
    }

    @Test
    void assignmentGetsLiteralValue() {
        AST ast = evaluate("W := 10px + 5px;");
        VariableAssignment assignment = (VariableAssignment) ast.root.body.get(0);

        assertEquals(new PixelLiteral(15), assignment.expression);
    }

    @Test
    void ifConditionGetsLiteralValue() {
        AST ast = evaluate("Flag := TRUE; p { if [Flag] { width: 1px; } }");
        Stylerule rule = (Stylerule) ast.root.body.get(1);
        IfClause ifClause = (IfClause) rule.body.get(0);

        assertTrue(ifClause.conditionalExpression instanceof BoolLiteral);
    }

    @Test
    void expressionsInsideIfAndElseAreEvaluated() {
        AST ast = evaluate("W := 5px; p { if [TRUE] { width: W * 2; } else { width: W + 1px; } }");
        Stylerule rule = (Stylerule) ast.root.body.get(1);
        IfClause ifClause = (IfClause) rule.body.get(0);

        assertEquals(new PixelLiteral(10), ((Declaration) ifClause.body.get(0)).expression);
        assertEquals(new PixelLiteral(6), ((Declaration) ifClause.elseClause.body.get(0)).expression);
    }
}
