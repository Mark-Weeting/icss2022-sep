package nl.han.ica.icss.transforms;

import java.util.ArrayList;
import java.util.HashMap;

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
import nl.han.ica.icss.ast.VariableAssignment;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();

        enterScope();
        ast.root.body = transformBody(ast.root.body);
        exitScope();
    }

    // Transforms every node in a body and returns the new body.
    private ArrayList<ASTNode> transformBody(ArrayList<ASTNode> body) {
        ArrayList<ASTNode> newBody = new ArrayList<>();
        for (ASTNode node : body) {
            if (node instanceof Stylerule) {
                transformStylerule((Stylerule) node);
            } else if (node instanceof VariableAssignment) {
                transformVariableAssignment((VariableAssignment) node);
            } else if (node instanceof Declaration) {
                transformDeclaration((Declaration) node);
            } else if (node instanceof IfClause) {
                // TR02: the if-clause is not added, only the nodes of the chosen body.
                newBody.addAll(transformIfClause((IfClause) node));
                continue;
            }
            newBody.add(node);
        }
        return newBody;
    }

    private void transformStylerule(Stylerule stylerule) {
        enterScope();
        stylerule.body = transformBody(stylerule.body);
        exitScope();
    }

    // TR02: returns the nodes that replace the if-clause.
    private ArrayList<ASTNode> transformIfClause(IfClause ifClause) {
        BoolLiteral condition = (BoolLiteral) evaluateExpression(ifClause.conditionalExpression);

        ArrayList<ASTNode> chosenBody;
        if (condition.value) {
            chosenBody = ifClause.body;
        } else if (ifClause.elseClause != null) {
            chosenBody = ifClause.elseClause.body;
        } else {
            return new ArrayList<>();
        }

        enterScope();
        ArrayList<ASTNode> newNodes = transformBody(chosenBody);
        exitScope();
        return newNodes;
    }

    private void transformVariableAssignment(VariableAssignment assignment) {
        Literal value = evaluateExpression(assignment.expression);
        assignment.expression = value;
        variableValues.getFirst().put(assignment.name.name, value);
    }

    private void transformDeclaration(Declaration declaration) {
        declaration.expression = evaluateExpression(declaration.expression);
    }

    // TR01: calculates the value of an expression and returns it as a literal
    private Literal evaluateExpression(Expression expression) {
        if (expression instanceof Literal) {
            return (Literal) expression;
        }
        if (expression instanceof VariableReference) {
            return lookupVariable(((VariableReference) expression).name);
        }
        return evaluateOperation((Operation) expression);
    }

    private Literal evaluateOperation(Operation operation) {
        Literal left = evaluateExpression(operation.lhs);
        Literal right = evaluateExpression(operation.rhs);

        if (operation instanceof AddOperation) {
            return createLiteral(left, getValue(left) + getValue(right));
        }
        if (operation instanceof SubtractOperation) {
            return createLiteral(left, getValue(left) - getValue(right));
        }

        Literal sideWithUnit = (left instanceof ScalarLiteral) ? right : left;
        return createLiteral(sideWithUnit, getValue(left) * getValue(right));
    }

    private int getValue(Literal literal) {
        if (literal instanceof PixelLiteral) {
            return ((PixelLiteral) literal).value;
        } else if (literal instanceof PercentageLiteral) {
            return ((PercentageLiteral) literal).value;
        } else if (literal instanceof ScalarLiteral) {
            return ((ScalarLiteral) literal).value;
        }
        throw new IllegalArgumentException("Cannot calculate with " + literal.getNodeLabel());
    }

    // Makes a new literal with the same unit and the given value.
    private Literal createLiteral(Literal unit, int value) {
        if (unit instanceof PixelLiteral) {
            return new PixelLiteral(value);
        } else if (unit instanceof PercentageLiteral) {
            return new PercentageLiteral(value);
        }
        return new ScalarLiteral(value);
    }

    // Searches the variable from the current scope to the global scope.
    private Literal lookupVariable(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        throw new IllegalStateException("Variable '" + name + "' has no value, run Check first");
    }

    private void enterScope() {
        variableValues.addFirst(new HashMap<>());
    }

    private void exitScope() {
        variableValues.removeFirst();
    }
}
