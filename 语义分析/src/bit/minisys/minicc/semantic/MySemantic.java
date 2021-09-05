package bit.minisys.minicc.semantic;

import bit.minisys.minicc.parser.ast.ASTCompilationUnit;
import bit.minisys.minicc.parser.ast.ASTDeclaration;
import bit.minisys.minicc.parser.ast.ASTFunctionDefine;
import bit.minisys.minicc.parser.ast.ASTNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;

public class MySemantic implements IMiniCCSemantic {
    public ArrayList<ArrayList<TableItem>> sum_table = new ArrayList<>();//符号表列表
    @Override
    public String run(String iFile) throws Exception {
        System.out.println("Semantic begin...");
        System.out.println("errors:");
        System.out.println("------------------------------------");
        ObjectMapper mapper = new ObjectMapper();
        ASTCompilationUnit program = (ASTCompilationUnit) mapper.readValue(new File(iFile), ASTCompilationUnit.class);

        ArrayList<TableItem> table_g= new ArrayList<>();//全局符号表
        sum_table.add(table_g);
        for (ASTNode item:program.items){
            //System.out.println(item.getChild(0));
            String class_name = "AST"+ item.getType();
            if(class_name.equals("ASTFunctionDefine")){//每个函数有一个符号表
                ASTFunctionDefine fdf = (ASTFunctionDefine) item;
                SemanticVisitor visitor = new SemanticVisitor();
                visitor.table_g = sum_table;
                fdf.accept(visitor);
                sum_table.add(visitor.table_0);
                sum_table.get(0).add(visitor.table_0.get(0));//把函数名添加到全局符号表
            }else if(class_name.equals("ASTDeclaration")){
                ASTDeclaration dc = (ASTDeclaration) item;
                SemanticVisitor visitor_2 = new SemanticVisitor();
                visitor_2.table_g=sum_table;
                item.accept(visitor_2);
                sum_table.get(0).addAll(visitor_2.table_0);//不属于任何函数的声明加进全局符号表
            }
        }

        for(int i=0;i<sum_table.size();i++){
            ArrayList<TableItem> tmp_table = sum_table.get(i);
            if(i==0){//全局符号表
                for(int j=0;j<tmp_table.size();j++){
                    for(int k=j+1;k<tmp_table.size();k++){
                        if((tmp_table.get(j).Name.equals(tmp_table.get(k).Name))&&(tmp_table.get(j).Kind.equals(tmp_table.get(k).Kind))&&(tmp_table.get(k).Kind.equals("VariableDeclarator")||tmp_table.get(k).Kind.equals("FunctionDeclarator"))){
                            System.out.println("ES2 >> Declaration:"+tmp_table.get(k).Name+" has been declarated.");
                        }
                    }
                }
            }else{//函数符号表
                ArrayList<TableItem> global_table = sum_table.get(0);
//                for(int j=0;j<tmp_table.size();j++){//与全局符号表比较
//                    for(int k=j+1;k<global_table.size();k++){
//                        if((tmp_table.get(j).Name.equals(global_table.get(k).Name))&&(tmp_table.get(j).Kind.equals(global_table.get(k).Kind))&&(tmp_table.get(k).Kind.equals("VariableDeclarator"))){
//                            System.out.println("ES2 >> Declaration:"+tmp_table.get(k).Name+" has been declarated.");
//                        }
//                    }
//                }
                for(int j=0;j<tmp_table.size();j++){//与自己比较
                    for(int k=j+1;k<tmp_table.size();k++){
                        if((tmp_table.get(j).Name.equals(tmp_table.get(k).Name))&&(tmp_table.get(j).Kind.equals(tmp_table.get(k).Kind))&&(tmp_table.get(k).Kind.equals("VariableDeclarator"))){
                            System.out.println("ES2 >> Declaration:"+tmp_table.get(k).Name+" has been declarated.");
                        }
                    }
                }
            }
        }
        System.out.println("------------------------------------");
        System.out.println("4. Semantic Finished!");

        sum_table.size();
        return "";
    }

}
