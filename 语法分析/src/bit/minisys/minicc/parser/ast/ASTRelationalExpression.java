package bit.minisys.minicc.parser.ast;

import com.fasterxml.jackson.annotation.JsonTypeName;
@JsonTypeName("RelationalExpression")
public class ASTRelationalExpression extends ASTExpression{
    public ASTToken op;
    public ASTExpression expr1;
    public ASTExpression expr2;

    public ASTRelationalExpression() {
        super("RelationalExpression");
    }

    public ASTRelationalExpression(ASTToken op,ASTExpression e1,ASTExpression e2) {
        super("AssignmentExpression");
        this.op = op;
        this.expr1 = e1;
        this.expr2 = e2;
    }
    @Override
    public void accept(ASTVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
