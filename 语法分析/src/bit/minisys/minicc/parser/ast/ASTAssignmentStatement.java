package bit.minisys.minicc.parser.ast;

import com.fasterxml.jackson.annotation.JsonTypeName;
@JsonTypeName("AssignmentStatement")
public class ASTAssignmentStatement extends ASTStatement{
    public ASTIdentifier id;
    public ASTToken op;
    public ASTExpression expr;

    public ASTAssignmentStatement() {
        super("AssignmentStatement");
    }
    public ASTAssignmentStatement(ASTIdentifier id,ASTToken op,ASTExpression expr) {
        super("AssignmentStatement");
        this.id = id;
        this.op = op;
        this.expr = expr;
    }

    @Override
    public void accept(ASTVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
