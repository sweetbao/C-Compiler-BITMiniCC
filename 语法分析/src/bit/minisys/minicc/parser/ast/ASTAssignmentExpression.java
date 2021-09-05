package bit.minisys.minicc.parser.ast;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("AssignmentExpression")
public class ASTAssignmentExpression extends ASTExpression{

    public ASTToken op;
    public ASTIdentifier id;
    public ASTExpression expr;

    public ASTAssignmentExpression() {
        super("AssignmentExpression");
    }

    public ASTAssignmentExpression(ASTToken op,ASTIdentifier e1,ASTExpression e2) {
        super("AssignmentExpression");
        this.op = op;
        this.id = e1;
        this.expr = e2;
    }
    @Override
    public void accept(ASTVisitor visitor) throws Exception {
        visitor.visit(this);
    }

}