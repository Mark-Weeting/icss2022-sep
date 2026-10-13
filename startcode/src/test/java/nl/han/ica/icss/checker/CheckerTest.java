package nl.han.ica.icss.checker;

import java.io.IOException;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.parser.ASTListener;
import nl.han.ica.icss.parser.ICSSLexer;
import nl.han.ica.icss.parser.ICSSParser;

class CheckerTest {

    private List<SemanticError> check(CharStream input) {
        ICSSParser parser = new ICSSParser(new CommonTokenStream(new ICSSLexer(input)));
        parser.setErrorHandler(new BailErrorStrategy());

        ASTListener listener = new ASTListener();
        new ParseTreeWalker().walk(listener, parser.stylesheet());

        AST ast = listener.getAST();
        new Checker().check(ast);
        return ast.getErrors();
    }

    private List<SemanticError> check(String icss) {
        return check(CharStreams.fromString(icss));
    }

    private List<SemanticError> checkResource(String resource) throws IOException {
        return check(CharStreams.fromStream(getClass().getClassLoader().getResourceAsStream(resource)));
    }

    private void assertSingleError(List<SemanticError> errors, String expectedText) {
        assertEquals(1, errors.size(), "Errors: " + errors);
        assertTrue(errors.get(0).description.contains(expectedText), "Error was: " + errors.get(0));
    }

    @Test
    void levelFilesHaveNoErrors() throws IOException {
        assertTrue(checkResource("level0.icss").isEmpty());
        assertTrue(checkResource("level1.icss").isEmpty());
        assertTrue(checkResource("level2.icss").isEmpty());
        assertTrue(checkResource("level3.icss").isEmpty());
    }

    // CH01: using a undefined variable
    @Test
    void undefinedVariableInDeclarationIsError() {
        assertSingleError(check("p { width: Width; }"), "'Width' is not defined");
    }

    @Test
    void undefinedVariableInAssignmentIsError() {
        assertSingleError(check("Width := Other;"), "'Other' is not defined");
    }

    @Test
    void undefinedVariableInOperationIsError() {
        assertSingleError(check("p { width: 10px + Width; }"), "'Width' is not defined");
    }

    @Test
    void undefinedVariableInIfConditionIsError() {
        assertSingleError(check("p { if [Flag] { width: 10px; } }"), "'Flag' is not defined");
    }

    @Test
    void variableUsedBeforeDefinitionIsError() {
        assertSingleError(check("p { width: Width; } Width := 10px;"), "'Width' is not defined");
    }

    @Test
    void variableCannotReferToItself() {
        assertSingleError(check("Width := Width;"), "'Width' is not defined");
    }

    @Test
    void globalVariableCanBeUsedInRule() {
        assertTrue(check("Width := 10px; p { width: Width; }").isEmpty());
    }

    @Test
    void variableCanBeAssignedFromOtherVariable() {
        assertTrue(check("A := 10px; B := A; p { width: B; }").isEmpty());
    }
}
