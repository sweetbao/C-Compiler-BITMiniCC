package bit.minisys.minicc.icgen;

import bit.minisys.minicc.parser.ast.ASTIdentifier;
import bit.minisys.minicc.parser.ast.ASTNode;
import bit.minisys.minicc.parser.ast.ASTVisitor;

public class CursorValue extends ASTNode {
    private Integer id;

    public String name() {
        if(id!=null){
            return id.toString();
        }
        else{
            return null;
        }
    }
    @Override
    public void accept(ASTVisitor visitor) throws Exception {

    }
    public CursorValue(Integer id) {
        super("CursorValue");
        this.id = id;
    }
    public CursorValue(String type) {
        super(type);
    }
}
