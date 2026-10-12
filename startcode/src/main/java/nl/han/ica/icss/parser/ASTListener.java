package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 *
 * Nodes that can have children (stylesheet, stylerule, declaration) are pushed on the
 * stack when entered and popped when exited, after which they are added to their parent.
 * Leaf nodes (selectors, property names, literals) are added to the parent directly.
 */
public class ASTListener extends ICSSBaseListener {

	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
	}
    public AST getAST() {
        return ast;
    }

	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
		currentContainer.push(new Stylesheet());
	}

	@Override
	public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
		ast.setRoot((Stylesheet) currentContainer.pop());
	}

	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx) {
		currentContainer.push(new Stylerule());
	}

	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx) {
		addToParent(currentContainer.pop());
	}

	@Override
	public void enterTagSelector(ICSSParser.TagSelectorContext ctx) {
		addToParent(new TagSelector(ctx.getText()));
	}

	@Override
	public void enterIdSelector(ICSSParser.IdSelectorContext ctx) {
		addToParent(new IdSelector(ctx.getText()));
	}

	@Override
	public void enterClassSelector(ICSSParser.ClassSelectorContext ctx) {
		addToParent(new ClassSelector(ctx.getText()));
	}

	@Override
	public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
		currentContainer.push(new Declaration());
	}

	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		addToParent(currentContainer.pop());
	}

	@Override
	public void enterPropertyName(ICSSParser.PropertyNameContext ctx) {
		addToParent(new PropertyName(ctx.getText()));
	}

	@Override
	public void enterColorLiteral(ICSSParser.ColorLiteralContext ctx) {
		addToParent(new ColorLiteral(ctx.getText()));
	}

	@Override
	public void enterPixelLiteral(ICSSParser.PixelLiteralContext ctx) {
		addToParent(new PixelLiteral(ctx.getText()));
	}

	@Override
	public void enterPercentageLiteral(ICSSParser.PercentageLiteralContext ctx) {
		addToParent(new PercentageLiteral(ctx.getText()));
	}

	private void addToParent(ASTNode node) {
		currentContainer.peek().addChild(node);
	}
}
