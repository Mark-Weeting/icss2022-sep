package nl.han.ica.icss.transforms;

import java.io.IOException;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.IfClause;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.VariableAssignment;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.checker.Checker;
import nl.han.ica.icss.generator.Generator;
import nl.han.ica.icss.parser.ASTListener;
import nl.han.ica.icss.parser.ICSSLexer;
import nl.han.ica.icss.parser.ICSSParser;

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
        String expected
                = "p {\n"
                + "  background-color: #ffffff;\n"
                + "  width: 500px;\n"
                + "}\n"
                + "\n"
                + "a {\n"
                + "  color: #ff0000;\n"
                + "}\n"
                + "\n"
                + "#menu {\n"
                + "  width: 520px;\n"
                + "}\n"
                + "\n"
                + ".menu {\n"
                + "  color: #000000;\n"
                + "}\n";

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

    // TR02: if/else clauses
    @Test
    void level3GivesExpectedCss() throws IOException {
        String expected
                = "p {\n"
                + "  background-color: #ffffff;\n"
                + "  width: 500px;\n"
                + "  color: #124532;\n"
                + "  background-color: #000000;\n"
                + "  height: 20px;\n"
                + "}\n"
                + "\n"
                + "a {\n"
                + "  color: #ff0000;\n"
                + "}\n"
                + "\n"
                + "#menu {\n"
                + "  width: 520px;\n"
                + "}\n"
                + "\n"
                + ".menu {\n"
                + "  color: #000000;\n"
                + "  background-color: #ff0000;\n"
                + "}\n";

        AST ast = evaluate(CharStreams.fromStream(getClass().getClassLoader().getResourceAsStream("level3.icss")));

        assertEquals(expected, new Generator().generate(ast));
    }

    @Test
    void trueIfIsReplacedByItsBodyOnTheSamePlace() {
        assertEquals("p {\n  width: 1px;\n  height: 2px;\n  color: #000000;\n}\n",
                evaluateToCss("p { width: 1px; if [TRUE] { height: 2px; } color: #000000; }"));
    }

    @Test
    void falseIfWithoutElseIsRemoved() {
        assertEquals("p {\n  width: 1px;\n}\n", evaluateToCss("p { width: 1px; if [FALSE] { height: 2px; } }"));
    }

    @Test
    void falseIfIsReplacedByElseBody() {
        assertEquals("p {\n  height: 3px;\n}\n",
                evaluateToCss("p { if [FALSE] { height: 2px; } else { height: 3px; } }"));
    }

    @Test
    void trueIfIgnoresElseBody() {
        assertEquals("p {\n  height: 2px;\n}\n",
                evaluateToCss("p { if [TRUE] { height: 2px; } else { height: 3px; } }"));
    }

    @Test
    void conditionCanBeVariable() {
        assertEquals("p {\n  height: 3px;\n}\n",
                evaluateToCss("Flag := FALSE; p { if [Flag] { height: 2px; } else { height: 3px; } }"));
    }

    @Test
    void nestedIfsAreAllRemoved() {
        assertEquals("p {\n  width: 1px;\n  height: 3px;\n}\n",
                evaluateToCss("p { if [TRUE] { width: 1px; if [FALSE] { height: 2px; } else { height: 3px; } } }"));
    }

    @Test
    void noIfClausesAreLeftInAst() {
        AST ast = evaluate("p { if [TRUE] { if [TRUE] { width: 1px; } } if [FALSE] { width: 2px; } }");
        Stylerule rule = (Stylerule) ast.root.body.get(0);

        for (ASTNode node : rule.body) {
            assertFalse(node instanceof IfClause);
        }
    }

    @Test
    void variableInsideIfIsUsedInsideIf() {
        assertEquals("p {\n  width: 20px;\n}\n",
                evaluateToCss("p { if [TRUE] { W := 10px; width: W * 2; } }"));
    }

    @Test
    void assignmentInsideIfOnlyChangesVariableInsideIf() {
        // Example from the assignment (4.4). Following the scope rules of Appendix A, the assignment
        // inside the if makes a new local variable, so WidthVar is still 0px after the if.
        String icss
                = "AdjustWidth := TRUE;\n"
                + "WidthVar := 0px;\n"
                + "p {\n"
                + "  if [AdjustWidth] {\n"
                + "    WidthVar := 200px;\n"
                + "  } else {\n"
                + "    WidthVar := 250px;\n"
                + "  }\n"
                + "  width: WidthVar;\n"
                + "}";

        assertEquals("p {\n  width: 0px;\n}\n", evaluateToCss(icss));
    }
}
