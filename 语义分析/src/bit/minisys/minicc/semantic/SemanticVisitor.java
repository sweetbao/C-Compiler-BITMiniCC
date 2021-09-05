package bit.minisys.minicc.semantic;

import bit.minisys.minicc.parser.ast.*;
import bit.minisys.minicc.pp.internal.T;

import bit.minisys.minicc.semantic.MySemantic;

import java.util.ArrayList;

public class SemanticVisitor implements ASTVisitor{

    public ArrayList<TableItem> table_0 = new ArrayList<>();
    public ArrayList<ArrayList<TableItem>> table_g ;
    public int current;//指示当前访问到的token
    public int loop_flag =0;//指示当前是否在循环体内
    public int check(ASTIdentifier id){
        int flag=0;
        for(int i=0;i<table_0.size();i++){
            if(table_0.get(i).Name.equals(id.value)){
                flag=1;
            }
        }
        for(int i=0;i<table_g.get(0).size();i++){
            if(table_g.get(0).get(i).Name.equals(id.value)){
                flag=1;
            }
        }
        return flag;
    }
    public int check(ASTExpression expr){
        String type = expr.getType();
        int flag=0;
        if(type.equals("Identifier")){
            ASTIdentifier id = (ASTIdentifier) expr;
            flag = check(id);
            if(flag==0){
                System.out.println("ES1 >> Identifier "+id.value+" is not defined.");
            }
        }
        return flag;
    }
    public int check_funcdef(TableItem item){//函数重复定义
        int flag=0;

        for(int i=1;i<table_g.size();i++){
            //遍历除自己和全局符号表以外的所有符号表，
            // 找出是否已经定义过该函数
            if((table_g.get(i).get(0).Name.equals(item.Name))&&(table_g.get(i).get(0).Kind.equals(item.Kind))){
                flag=1;
            }
        }
        return flag;
    }

    public int check_vardec(TableItem item){
        int flag=0;
        for(int i=0;i<table_0.size()-1;i++){//先查当前符号表
            if((item.Name.equals(table_0.get(i).Name))&&(table_0.get(i).Kind.equals("VariableDeclarator"))){
                flag = 1;//存在重复定义
            }
        }

        for(int i=0;i<table_g.get(0).size();i++){//查全局符号表
            if((item.Name.equals(table_g.get(0).get(i).Name))&&(table_0.get(i).Kind.equals("VariableDeclarator"))){
                flag = 1;//存在重复定义
            }
        }
        return flag;
    }

    @Override
    public void visit(ASTToken token) throws Exception {

    }

    @Override
    public void visit(ASTInitList initList) throws Exception {
        initList.declarator.accept(this);
        for(int i=0;i<(initList.exprs.size());i++){
            ASTExpression expr = initList.exprs.get(i);
            expr.accept(this);
        }

    }

    @Override
    public void visit(ASTTypename typename) throws Exception {

    }

    @Override
    public void visit(ASTStatement statement) throws Exception {

    }

    @Override
    public void visit(ASTDeclarator declarator) throws Exception {
        String type = declarator.getType();
        if(type.equals("VariableDeclarator")){
            ASTVariableDeclarator vd = (ASTVariableDeclarator) declarator;
            vd.accept(this);
        }else if(type.equals("FunctionDeclarator")){
            ASTFunctionDeclarator fd = (ASTFunctionDeclarator) declarator;
            fd.accept(this);
        }
    }

    @Override
    public void visit(ASTExpression expression) throws Exception {
        String expr_type = expression.getType();
        if(expr_type.equals("FunctionCall")){
            ASTFunctionCall fc = (ASTFunctionCall) expression;
            fc.accept(this);
        }else if(expr_type.equals("BinaryExpression")) {
            ASTBinaryExpression be = (ASTBinaryExpression) expression;
            be.accept(this);
        }else if(expr_type.equals(("Identifier"))){
            ASTIdentifier id = (ASTIdentifier) expression;
            int flag = check(id);
            if(flag==0){
                System.out.println("ES1 >> Identifier "+id.value+" is not defined.");
            }
        }
    }

    @Override
    public void visit(ASTFunctionCall funcCall) throws Exception {
        String type = funcCall.funcname.getType();
        if(type.equals("Identifier")){
            ASTIdentifier id = (ASTIdentifier) funcCall.funcname;
            int flag = check(id);
            if(flag==0){
                System.out.println("ES1 >> FunctionCall:"+id.value+" is not declarated.");
            }
            //检查参数个数
            //首先找到定义的函数
            int func_def_ord =0;
            for(int i=0;i<table_g.get(0).size();i++){
                if(table_g.get(0).get(i).Name.equals(id.value)){
                    func_def_ord=i;
                    break;
                }
            }
            if(table_g.get(0).size()<1){
                return;
            }
            if(funcCall.argList.size()!=table_g.get(0).get(func_def_ord).params.size()){
                System.out.println("ES4 >> FunctionCall:"+table_0.get(0).Name+"'s param num is not matched.");
            }else{
                //检查参数类型
                for(int i=0;i<funcCall.argList.size();i++){
                    if(table_g.get(0).get(func_def_ord).params.get(i).equals("int")){
                        if(funcCall.argList.get(i).getType().equals("IntegerConstant")){
                            continue;
                        }else {
                            System.out.println("ES4 >> FunctionCall:"+table_0.get(0).Name+"'s param type is not matched.");
                        }
                    }
                }
            }

        }
    }

    @Override
    public void visit(ASTIdentifier identifier) throws Exception {
        TableItem item = new TableItem();
        item.Name=identifier.value;
        item.Scope_entry = identifier.tokenId;
        this.current = identifier.tokenId;
        this.table_0.add(item);
    }

    @Override
    public void visit(ASTCharConstant charConst) throws Exception {

    }

    @Override
    public void visit(ASTGotoStatement gotoStat) throws Exception {

    }

    @Override
    public void visit(ASTArrayAccess arrayAccess) throws Exception {

    }

    @Override
    public void visit(ASTCompilationUnit program) throws Exception {

    }

    @Override
    public void visit(ASTDeclaration declaration) throws Exception {
        for(int i=0;i<(declaration.initLists.size());i++){
            declaration.initLists.get(i).accept(this);
        }
        //声明类型 todo
        for(int i=0;i<table_0.size();i++){
            table_0.get(i).Type = declaration.specifiers.get(0).value;
        }

    }

    @Override
    public void visit(ASTBreakStatement breakStat) throws Exception {

    }

    @Override
    public void visit(ASTFloatConstant floatConst) throws Exception {

    }

    @Override
    public void visit(ASTIntegerConstant intConst) throws Exception {

    }

    @Override
    public void visit(ASTMemberAccess memberAccess) throws Exception {

    }

    @Override
    public void visit(ASTReturnStatement returnStat) throws Exception {

    }

    @Override
    public void visit(ASTStringConstant stringConst) throws Exception {

    }

    @Override
    public void visit(ASTUnaryTypename unaryTypename) throws Exception {

    }

    @Override
    public void visit(ASTLabeledStatement labeledStat) throws Exception {

    }

    @Override
    public void visit(ASTCastExpression castExpression) throws Exception {

    }

    @Override
    public void visit(ASTFunctionDefine functionDefine) throws Exception {
        String temp_type = functionDefine.declarator.getType();
        if(temp_type.equals("FunctionDeclarator")) {
            ASTFunctionDeclarator fd = (ASTFunctionDeclarator) functionDefine.declarator;
            fd.accept(this);
            if(this.table_0.size()!=0){

                this.table_0.get(0).Kind = "FunctionDefine";
                int flag = check_funcdef(this.table_0.get(0));
                if(flag==1){//存在函数重复定义
                    System.out.println("ES2 >> FunctionDefine:"+this.table_0.get(0).Name+" is defined.");
                }
            }
        }
        //table.get(0).Type = functionDefine.specifiers.get(0).value;

        //函数体部分
        functionDefine.body.accept(this);

    }

    @Override
    public void visit(ASTCompoundStatement compoundStat) throws Exception {
        for(int i=0;i<(compoundStat.blockItems.size());i++){
            ASTNode node = compoundStat.blockItems.get(i);
            convert(node);

            //compoundStat.blockItems.get(i).accept(this);
        }
    }

    @Override
    public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {

    }

    @Override
    public void visit(ASTUnaryExpression unaryExpression) throws Exception {

    }

    @Override
    public void visit(ASTIterationStatement iterationStat) throws Exception {
        this.loop_flag = 1;//进入循环体
        //todo 访问循环内部
        String tmp_type = iterationStat.stat.getType();
        if(tmp_type.equals("CompoundStatement")){
            ASTCompoundStatement cstmt = (ASTCompoundStatement) iterationStat.stat;
            cstmt.accept(this);
        }
        this.loop_flag = 0;//退出循环体
    }

    @Override
    public void visit(ASTSelectionStatement selectionStat) throws Exception {

    }

    @Override
    public void visit(ASTBinaryExpression binaryExpression) throws Exception {
        if(binaryExpression.expr1.getType().equals("Identifier")){
            check(binaryExpression.expr1);
        }
        if(binaryExpression.expr2.getType().equals("FunctionCall")){
            ASTFunctionCall fc = (ASTFunctionCall) binaryExpression.expr2;
            fc.accept(this);
        }else if(binaryExpression.expr2.getType().equals("Identifier")){
            check(binaryExpression.expr2);
        }
    }

    @Override
    public void visit(ASTParamsDeclarator paramsDeclarator) throws Exception {
        String tmp_type = paramsDeclarator.declarator.getType();
        int table_cur = table_0.size();//保存断点，用于后续为参数添加信息
        if(tmp_type.equals("VariableDeclarator")){
            ASTVariableDeclarator vd = (ASTVariableDeclarator)paramsDeclarator.declarator;
            vd.accept(this);
        }
        for(int i=table_cur;i<table_0.size();i++){//为参数添加类型
            table_0.get(i).Type = paramsDeclarator.specfiers.get(0).value;
        }


        //item.Type=paramsDeclarator.specfiers.get(0).value;

    }

    @Override
    public void visit(ASTExpressionStatement expressionStat) throws Exception {
        for(int i=0;i<expressionStat.exprs.size();i++){
            expressionStat.exprs.get(i).accept(this);
        }
    }

    @Override
    public void visit(ASTContinueStatement continueStatement) throws Exception {

    }

    @Override
    public void visit(ASTPostfixExpression postfixExpression) throws Exception {

    }

    @Override
    public void visit(ASTFunctionDeclarator functionDeclarator) throws Exception {
        String temp_type = functionDeclarator.declarator.getType();

        if(temp_type.equals("VariableDeclarator")) {
            ASTVariableDeclarator vd = (ASTVariableDeclarator) functionDeclarator.declarator;
            vd.accept(this);
            if(table_0.size()!=0){
                table_0.get(table_0.size()-1).Kind = "FunctionDeclarator";
                //将最后添加的符号设为FunctionDeclarator类型
            }

        }

        //遍历参数列表
        for(int i=0;i<functionDeclarator.params.size();i++){
            functionDeclarator.params.get(i).accept(this);
        }
        if(table_0.size()>1){//函数有参数
            for(int i=1;i<table_0.size();i++){//把每个参数类型添加到该函数的参数属性里
                table_0.get(0).params.add(table_0.get(i).Type);
            }
        }

    }

    @Override
    public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {
        variableDeclarator.identifier.accept(this);
        table_0.get(table_0.size()-1).Kind = "VariableDeclarator";
    }

    @Override
    public void visit(ASTConditionExpression conditionExpression) throws Exception {

    }

    @Override
    public void visit(ASTIterationDeclaredStatement iterationDeclaredStat) throws Exception {

    }

    public void convert(ASTNode node)throws Exception{
        String tmp_type = node.getType();
        if(tmp_type.equals("Declaration")){
            ASTDeclaration dc = (ASTDeclaration)node;
            dc.accept(this);
        }else if(tmp_type.equals("CompoundStatement")){
            ASTCompoundStatement cstmt = (ASTCompoundStatement) node;
            //有新的语句块，产生新的符号表
            SemanticVisitor visitor_2=new SemanticVisitor();
            cstmt.accept(visitor_2);

            TableItem sub_block = new TableItem();
            sub_block.Kind = "sub_block";
            sub_block.sub_table=visitor_2.table_0;
            this.table_0.add(sub_block);

        }else if(tmp_type.equals("Expression")){//检查表达式中的变量是否定义
            //todo
        }else if(tmp_type.equals("BreakStatement")){
            if(this.loop_flag!=1){
                System.out.println("ES3 >> BreakStatement:must be in a LoopStatement.");
            }
        }else if (tmp_type.equals("IterationStatement")){
            ASTIterationStatement istmt = (ASTIterationStatement) node;
            istmt.accept(this);
        }else if(tmp_type.equals("ExpressionStatement")){
            ASTExpressionStatement estmt = (ASTExpressionStatement) node;
            estmt.accept(this);
        }
    }
}
