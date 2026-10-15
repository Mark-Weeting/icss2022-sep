package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;

/**
 * Turns the AST into CSS. This only works after the Evaluator has run:
 * then all expressions are literals and all if-clauses are gone.
 */
public class Generator {

    // GE02: two spaces per scope level
    private static final String INDENT = "  ";

    public String generate(AST ast) {
        StringBuilder css = new StringBuilder();
        for (ASTNode node : ast.root.body) {
            // Variable assignments are not CSS, so they are skipped.
            if (node instanceof Stylerule) {
                if (css.length() > 0) {
                    css.append("\n");
                }
                generateStylerule((Stylerule) node, css);
            }
        }
        return css.toString();
    }

    private void generateStylerule(Stylerule stylerule, StringBuilder css) {
        css.append(generateSelectors(stylerule)).append(" {\n");
        for (ASTNode node : stylerule.body) {
            if (node instanceof Declaration) {
                generateDeclaration((Declaration) node, 1, css);
            }
        }
        css.append("}\n");
    }

    private String generateSelectors(Stylerule stylerule) {
        StringBuilder selectors = new StringBuilder();
        for (Selector selector : stylerule.selectors) {
            if (selectors.length() > 0) {
                selectors.append(", ");
            }
            selectors.append(selector.toString());
        }
        return selectors.toString();
    }

    private void generateDeclaration(Declaration declaration, int level, StringBuilder css) {
        css.append(INDENT.repeat(level))
                .append(declaration.property.name)
                .append(": ")
                .append(generateValue(declaration.expression))
                .append(";\n");
    }

    private String generateValue(Expression expression) {
        if (expression instanceof PixelLiteral) {
            return ((PixelLiteral) expression).value + "px";
        } else if (expression instanceof PercentageLiteral) {
            return ((PercentageLiteral) expression).value + "%";
        } else if (expression instanceof ColorLiteral) {
            return ((ColorLiteral) expression).value;
        }
        throw new IllegalStateException("Cannot generate CSS for '" + expression.getNodeLabel()
                + "', run Transform first");
    }
}
