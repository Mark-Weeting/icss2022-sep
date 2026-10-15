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

    // CH06: a variable can only be used inside its scope

    @Test
    void ruleVariableCanBeUsedLaterInSameRule() {
        assertTrue(check("p { W := 10px; width: W; }").isEmpty());
    }

    @Test
    void ruleVariableCannotBeUsedInOtherRule() {
        assertSingleError(check("p { W := 10px; } a { width: W; }"), "'W' is not defined");
    }

    @Test
    void ruleVariableCanBeUsedInsideIfOfThatRule() {
        assertTrue(check("p { W := 10px; if [TRUE] { width: W; } }").isEmpty());
    }

    @Test
    void globalVariableCanBeUsedInNestedIf() {
        assertTrue(check("W := 10px; p { if [TRUE] { if [TRUE] { width: W; } } }").isEmpty());
    }

    @Test
    void ifVariableCannotBeUsedAfterIf() {
        assertSingleError(check("p { if [TRUE] { W := 10px; } width: W; }"), "'W' is not defined");
    }

    @Test
    void ifVariableCannotBeUsedInElse() {
        assertSingleError(check("p { if [TRUE] { W := 10px; } else { width: W; } }"), "'W' is not defined");
    }

    @Test
    void elseVariableCannotBeUsedAfterElse() {
        assertSingleError(check("p { if [TRUE] { } else { W := 10px; } width: W; }"), "'W' is not defined");
    }

    @Test
    void nestedIfVariableCannotBeUsedInOuterIf() {
        assertSingleError(check("p { if [TRUE] { if [TRUE] { W := 10px; } width: W; } }"), "'W' is not defined");
    }

    @Test
    void variableAssignedAgainInsideIfIsAllowed() {
        assertTrue(check("W := 0px; p { if [TRUE] { W := 200px; } width: W; }").isEmpty());
    }

    // CH02: types in calculations

    @Test
    void addingSameTypesIsAllowed() {
        assertTrue(check("p { width: 10px + 5px - 2px; height: 10% + 5%; }").isEmpty());
    }

    @Test
    void addingPixelsAndPercentagesIsError() {
        assertSingleError(check("p { width: 10px + 5%; }"), "Cannot add or subtract pixel and percentage");
    }

    @Test
    void subtractingScalarFromPixelsIsError() {
        assertSingleError(check("p { width: 10px - 2; }"), "Cannot add or subtract pixel and scalar");
    }

    @Test
    void multiplyingWithScalarIsAllowed() {
        assertTrue(check("p { width: 3 * 10px; height: 20% * 3; } A := 4 * 5;").isEmpty());
    }

    @Test
    void multiplyingWithoutScalarIsError() {
        assertSingleError(check("p { width: 2px * 3px; }"), "at least one side must be a scalar");
    }

    @Test
    void typeOfVariableIsUsedInCalculation() {
        assertSingleError(check("W := 10px; p { width: W + 5%; }"), "pixel and percentage");
    }

    @Test
    void typeOfMultiplicationIsUsedInAddition() {
        // 2 * 10px is pixel, so this is pixel + pixel
        assertTrue(check("p { width: 50px + 2 * 10px; }").isEmpty());
        // 2 * 10% is percentage, so this is pixel + percentage
        assertSingleError(check("p { width: 50px + 2 * 10%; }"), "pixel and percentage");
    }

    @Test
    void typeOfCalculatedVariableIsKnown() {
        assertSingleError(check("W := 10px + 5px; p { width: W * 2px; }"), "at least one side must be a scalar");
    }

    @Test
    void errorInsideCalculationGivesOnlyOneError() {
        // Only the undefined variable is reported, not also the addition.
        assertSingleError(check("p { width: Unknown + 5px; }"), "'Unknown' is not defined");
    }

    // CH03: no colors in calculations

    @Test
    void addingColorsIsError() {
        assertSingleError(check("p { color: #ff0000 + #00ff00; }"), "Colors cannot be used in calculations");
    }

    @Test
    void multiplyingColorIsError() {
        assertSingleError(check("p { color: 2 * #ff0000; }"), "Colors cannot be used in calculations");
    }

    @Test
    void colorVariableInCalculationIsError() {
        assertSingleError(check("C := #ff0000; p { width: C - 10px; }"), "Colors cannot be used in calculations");
    }

    @Test
    void booleanInCalculationIsError() {
        assertSingleError(check("p { width: TRUE + 10px; }"), "Booleans cannot be used in calculations");
    }

    // CH04: the value must fit the property

    @Test
    void correctValuesForPropertiesAreAllowed() {
        assertTrue(check("p { color: #ff0000; background-color: #00ff00; width: 10px; height: 50%; }").isEmpty());
    }

    @Test
    void colorForWidthIsError() {
        assertSingleError(check("p { width: #ff0000; }"), "Property 'width' needs a pixel or percentage value, not a color");
    }

    @Test
    void pixelsForColorIsError() {
        assertSingleError(check("p { color: 12px; }"), "Property 'color' needs a color, not a pixel");
    }

    @Test
    void scalarAsValueIsError() {
        assertSingleError(check("p { height: 10; }"), "not a scalar");
    }

    @Test
    void booleanAsValueIsError() {
        assertSingleError(check("p { background-color: TRUE; }"), "not a bool");
    }

    @Test
    void typeOfVariableIsUsedForProperty() {
        assertSingleError(check("C := #ff0000; p { width: C; }"), "not a color");
    }

    @Test
    void typeOfCalculationIsUsedForProperty() {
        assertSingleError(check("p { width: 2 * 5; }"), "not a scalar");
    }

    @Test
    void unknownPropertyIsError() {
        assertSingleError(check("p { font-size: 10px; }"), "Unknown property 'font-size'");
    }

    @Test
    void errorInValueGivesOnlyOneError() {
        assertSingleError(check("p { width: Unknown; }"), "'Unknown' is not defined");
    }

    // CH05: the condition of an if must be a boolean

    @Test
    void booleanConditionIsAllowed() {
        assertTrue(check("Flag := FALSE; p { if [TRUE] { width: 1px; } if [Flag] { width: 2px; } }").isEmpty());
    }

    @Test
    void pixelConditionIsError() {
        assertSingleError(check("p { if [10px] { width: 1px; } }"), "must be a boolean, not a pixel");
    }

    @Test
    void colorVariableAsConditionIsError() {
        assertSingleError(check("C := #ff0000; p { if [C] { width: 1px; } }"), "must be a boolean, not a color");
    }

    @Test
    void undefinedConditionGivesOnlyOneError() {
        assertSingleError(check("p { if [Unknown] { width: 1px; } }"), "'Unknown' is not defined");
    }
}
