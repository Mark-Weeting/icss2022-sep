package nl.han.ica.icss.checker;

import java.util.HashMap;
import java.util.List;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.IfClause;
import nl.han.ica.icss.ast.Literal;
import nl.han.ica.icss.ast.Operation;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.Stylesheet;
import nl.han.ica.icss.ast.VariableAssignment;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.types.ExpressionType;

/**
 * Checks the AST for mistakes. A mistake is stored in the node where it happens
 * (setError).
 *
 * variableTypes holds one HashMap per scope (name -> type). The current scope
 * is at the front of the list, the global scope at the back.
 */
public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();
        checkStylesheet(ast.root);
    }

    private void checkStylesheet(Stylesheet stylesheet) {
        enterScope();
        checkBody(stylesheet.body);
        exitScope();
    }

    private void checkStylerule(Stylerule stylerule) {
        enterScope();
        checkBody(stylerule.body);
        exitScope();
    }

    private void checkIfClause(IfClause ifClause) {
        checkExpression(ifClause.conditionalExpression);

        enterScope();
        checkBody(ifClause.body);
        exitScope();

        if (ifClause.elseClause != null) {
            enterScope();
            checkBody(ifClause.elseClause.body);
            exitScope();
        }
    }

    private void checkBody(List<ASTNode> body) {
        for (ASTNode node : body) {
            if (node instanceof Stylerule) {
                checkStylerule((Stylerule) node);
            } else if (node instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) node);
            } else if (node instanceof Declaration) {
                checkDeclaration((Declaration) node);
            } else if (node instanceof IfClause) {
                checkIfClause((IfClause) node);
            }
        }
    }

    private void checkDeclaration(Declaration declaration) {
        checkExpression(declaration.expression);
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType type = checkExpression(assignment.expression);
        variableTypes.getFirst().put(assignment.name.name, type);
    }

    // Checks an expression and returns its type (UNDEFINED if unknown).
    private ExpressionType checkExpression(Expression expression) {
        if (expression instanceof Literal) {
            return getLiteralType((Literal) expression);
        }
        if (expression instanceof VariableReference) {
            return checkVariableReference((VariableReference) expression);
        }
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;
            checkExpression(operation.lhs);
            checkExpression(operation.rhs);
            // The type of an operation is added later (CH02).
            return ExpressionType.UNDEFINED;
        }
        return ExpressionType.UNDEFINED;
    }

    // CH01: a variable must be defined before you use it
    private ExpressionType checkVariableReference(VariableReference reference) {
        ExpressionType type = lookupVariable(reference.name);
        if (type == null) {
            reference.setError("Variable '" + reference.name + "' is not defined");
            return ExpressionType.UNDEFINED;
        }
        return type;
    }

    private ExpressionType getLiteralType(Literal literal) {
        if (literal instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        } else if (literal instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        } else if (literal instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        } else if (literal instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        } else if (literal instanceof BoolLiteral) {
            return ExpressionType.BOOL;
        }
        return ExpressionType.UNDEFINED;
    }

    // Searches the variable from the current scope to the global scope.
    // Returns null if the variable is not found.
    private ExpressionType lookupVariable(String name) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private void enterScope() {
        variableTypes.addFirst(new HashMap<>());
    }

    private void exitScope() {
        variableTypes.removeFirst();
    }
}
