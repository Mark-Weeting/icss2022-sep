package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.AST;
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

class GeneratorTest {

    private String generate(CharStream input) {
        ICSSParser parser = new ICSSParser(new CommonTokenStream(new ICSSLexer(input)));
        parser.setErrorHandler(new BailErrorStrategy());

        ASTListener listener = new ASTListener();
        new ParseTreeWalker().walk(listener, parser.stylesheet());

        AST ast = listener.getAST();
        return new Generator().generate(ast);
    }

    private String generate(String icss) {
        return generate(CharStreams.fromString(icss));
    }

    @Test
    void level0GivesExpectedCss() throws IOException {
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

        String css = generate(CharStreams.fromStream(getClass().getClassLoader().getResourceAsStream("level0.icss")));

        assertEquals(expected, css);
    }

    @Test
    void declarationsAreIndentedWithTwoSpaces() {
        assertEquals("p {\n  width: 10px;\n}\n", generate("p { width: 10px; }"));
    }

    @Test
    void percentagesGetPercentSign() {
        assertEquals("p {\n  height: 50%;\n}\n", generate("p { height: 50%; }"));
    }

    @Test
    void emptyStylesheetGivesEmptyCss() {
        assertEquals("", generate(""));
    }

    @Test
    void variableAssignmentsAreNotInCss() {
        assertEquals("p {\n  width: 10px;\n}\n", generate("W := 5px; p { V := 1px; width: 10px; }"));
    }

    @Test
    void expressionThatIsNotTransformedGivesClearError() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> generate("W := 5px; p { width: W; }"));

        assertTrue(error.getMessage().contains("run Transform first"));
    }
}
