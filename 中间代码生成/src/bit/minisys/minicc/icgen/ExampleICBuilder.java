package bit.minisys.minicc.icgen;

import java.util.*;

import bit.minisys.minicc.parser.ast.*;
import org.python.antlr.AST;

// ???????????????????
public class ExampleICBuilder implements ASTVisitor{

	public Map<ASTNode, ASTNode> map;				// ???map?洢???????????key???????value??????????value????????ASTIdentifier,ASTIntegerConstant,TemportaryValue...
	public List<Quat> quats;						// ??????????б?
	public Integer tmpId;							// ??????????
	public Integer cursor;
	public ExampleICBuilder() {
		map = new HashMap<ASTNode, ASTNode>();
		quats = new LinkedList<Quat>();
		tmpId = 0;
		cursor = 1;
	}
	public List<Quat> getQuats() {
		return quats;
	}

	//符号表
	public ArrayList<TableItem> table_0 = new ArrayList<>();//当前函数符号表
	public ArrayList<ArrayList<TableItem>> table_g = new ArrayList<ArrayList<TableItem>>();//全局符号表
	public ArrayList<DopeVector> table_vec = new ArrayList<>();//数组内情向量表

	public void addItem(ASTIdentifier identifier){
		TableItem item = new TableItem();
		item.Name=identifier.value;
		item.Scope_entry = cursor;
		this.table_0.add(item);
	}
	@Override
	public void visit(ASTCompilationUnit program) throws Exception {
		for (ASTNode node : program.items) {
			if(node instanceof ASTFunctionDefine) {
				ExampleICBuilder icb = new ExampleICBuilder();
				icb.table_g = this.table_g;
				icb.cursor = this.cursor;
				ASTFunctionDefine fd = (ASTFunctionDefine)node;
				fd.accept(icb);
				this.cursor = icb.cursor;
				if(table_g.size()==0){
					ArrayList<TableItem> temp_t = new ArrayList<TableItem>();
					if(icb.table_0!=null){
						temp_t.add(icb.table_0.get(0));
						table_g.add(temp_t);
					}
				}
				else{
					if(icb.table_0!=null){
						table_g.get(0).add(icb.table_0.get(0));
					}
				}
				table_g.add(icb.table_0);
				//复制子函数的quat和map
				quats.addAll(icb.quats);
				tmpId = icb.tmpId;
				table_vec.addAll(icb.table_vec);
			}
		}
	}

	@Override
	public void visit(ASTDeclaration declaration) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0;i<(declaration.initLists.size());i++){
			declaration.initLists.get(i).accept(this);
		}
		//声明类型 todo
		for(int i=0;i<table_0.size();i++){
			table_0.get(i).Type = declaration.specifiers.get(0).value;
		}
	}

	@Override
	public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {
		DopeVector vector = new DopeVector();
		vector.dim = 0;

		if(arrayDeclarator.declarator instanceof ASTVariableDeclarator){
			this.table_vec.add(vector);
			ASTVariableDeclarator vd = (ASTVariableDeclarator) arrayDeclarator.declarator;
			this.table_vec.get(table_vec.size()-1).name = vd.identifier.value;
			this.table_vec.get(table_vec.size()-1).dim += 1;
			visit((ASTVariableDeclarator)arrayDeclarator.declarator);
		}else if(arrayDeclarator.declarator instanceof ASTArrayDeclarator){
			visit((ASTArrayDeclarator)arrayDeclarator.declarator);
			this.table_vec.get(table_vec.size()-1).dim+=1;
		} if(arrayDeclarator.expr instanceof ASTIntegerConstant){

			ASTIntegerConstant ic = (ASTIntegerConstant) arrayDeclarator.expr;
			Up_Low ul = new Up_Low();
			ul.lower = 0;
			ul.upper = ic.value-1;
			table_vec.get(table_vec.size()-1).bound.add(ul);
		}


	}

	@Override
	public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {
		addItem(variableDeclarator.identifier);
		table_0.get(table_0.size()-1).Kind = "VariableDeclarator";
		
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


	}

	@Override
	public void visit(ASTArrayAccess arrayAccess) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTBinaryExpression binaryExpression) throws Exception {
		String op = binaryExpression.op.value;
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;

		if (op.equals("=")) {//赋值表达式
			// ???????
			// ?????????????res
			visit(binaryExpression.expr1);
			res = map.get(binaryExpression.expr1);
			// ?ж????????????, ?????????a = b + c; ??????????????tmp1 = b + c; a = tmp1;??????????????????????
			if (binaryExpression.expr2 instanceof ASTIdentifier) {
				opnd1 = binaryExpression.expr2;
			} else if (binaryExpression.expr2 instanceof ASTIntegerConstant) {
				opnd1 = binaryExpression.expr2;
			} else if (binaryExpression.expr2 instanceof ASTBinaryExpression) {
				ASTBinaryExpression value = (ASTBinaryExpression) binaryExpression.expr2;
				op = value.op.value;
				visit(value.expr1);
				opnd1 = map.get(value.expr1);
				visit(value.expr2);
				opnd2 = map.get(value.expr2);
			} else if(binaryExpression.expr2 instanceof ASTUnaryExpression) {
				ASTUnaryExpression ue = (ASTUnaryExpression)binaryExpression.expr2;
				visit(ue);
				opnd1 = new TemporaryValue(tmpId);
			} else {
				// else ...
			}

		} else if (op.equals("+")||op.equals("-")) {
			// ?????????????洢???м????
			res = new TemporaryValue(++tmpId);
			visit(binaryExpression.expr1);
			opnd1 = map.get(binaryExpression.expr1);
			visit(binaryExpression.expr2);
			opnd2 = map.get(binaryExpression.expr2);
		} else if(op.equals("<")||op.equals(">")||op.equals(">=")||op.equals("<=")||op.equals("==")){//布尔表达式
			res = new TemporaryValue(++tmpId);
			visit(binaryExpression.expr1);
			opnd1 = map.get(binaryExpression.expr1);
			visit(binaryExpression.expr2);
			opnd2 = map.get(binaryExpression.expr2);

		}else {
			// else..
		}
		
		// build quat
		Quat quat = new Quat(cursor++,op, res, opnd1, opnd2);
		quats.add(quat);
		map.put(binaryExpression, res);
	}

	@Override
	public void visit(ASTBreakStatement breakStat) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTContinueStatement continueStatement) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTCastExpression castExpression) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTCharConstant charConst) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTCompoundStatement compoundStat) throws Exception {
		for (ASTNode node : compoundStat.blockItems) {
			if(node instanceof ASTDeclaration) {
				ASTDeclaration dc = (ASTDeclaration)node;
				Integer temp = table_vec.size();//记录此时数组的内情向量表元素个数，便于后续为内情向量添加类型
				visit((ASTDeclaration)node);
				if(dc.initLists.get(0).declarator instanceof ASTArrayDeclarator){
					for(int i=temp;i<table_vec.size();i++){
						table_vec.get(i).type = dc.specifiers.get(0).value;
					}
				}
			}else if (node instanceof ASTStatement) {
				visit((ASTStatement)node);
			}
		}
		
	}

	@Override
	public void visit(ASTConditionExpression conditionExpression) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTExpression expression) throws Exception {
		if(expression instanceof ASTArrayAccess) {
			visit((ASTArrayAccess)expression);
		}else if(expression instanceof ASTBinaryExpression) {
			visit((ASTBinaryExpression)expression);
		}else if(expression instanceof ASTCastExpression) {
			visit((ASTCastExpression)expression);
		}else if(expression instanceof ASTCharConstant) {
			visit((ASTCharConstant)expression);
		}else if(expression instanceof ASTConditionExpression) {
			visit((ASTConditionExpression)expression);
		}else if(expression instanceof ASTFloatConstant) {
			visit((ASTFloatConstant)expression);
		}else if(expression instanceof ASTFunctionCall) {
			visit((ASTFunctionCall)expression);
		}else if(expression instanceof ASTIdentifier) {
			visit((ASTIdentifier)expression);
		}else if(expression instanceof ASTIntegerConstant) {
			visit((ASTIntegerConstant)expression);
		}else if(expression instanceof ASTMemberAccess) {
			visit((ASTMemberAccess)expression);
		}else if(expression instanceof ASTPostfixExpression) {
			visit((ASTPostfixExpression)expression);
		}else if(expression instanceof ASTStringConstant) {
			visit((ASTStringConstant)expression);
		}else if(expression instanceof ASTUnaryExpression) {
			visit((ASTUnaryExpression)expression);
		}else if(expression instanceof ASTUnaryTypename){
			visit((ASTUnaryTypename)expression);
		}
	}

	@Override
	public void visit(ASTExpressionStatement expressionStat) throws Exception {
		for (ASTExpression node : expressionStat.exprs) {
			visit((ASTExpression)node);
		}
	}

	@Override
	public void visit(ASTFloatConstant floatConst) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTFunctionCall funcCall) throws Exception {
		//在符号表中寻找函数入口
		ASTNode res= null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		ASTIdentifier funid = new ASTIdentifier();
		if(funcCall.funcname instanceof ASTIdentifier){
			funid = (ASTIdentifier)funcCall.funcname;
		}
		for(int i=0;i<table_g.get(0).size();i++){
			if(table_g.get(0).get(i).Name.equals(funid.value)){
				res = new CursorValue(table_g.get(0).get(i).Scope_entry);
				break;
			}
		}
		Quat quat1 = new Quat(cursor++,"J",res,opnd1,opnd2);
		quats.add(quat1);
		map.put(funcCall, res);
	}

	@Override
	public void visit(ASTGotoStatement gotoStat) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTIdentifier identifier) throws Exception {
		map.put(identifier, identifier);
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
	public void visit(ASTIntegerConstant intConst) throws Exception {
		map.put(intConst, intConst);
	}

	@Override
	public void visit(ASTIterationDeclaredStatement iterationDeclaredStat) throws Exception {

		
	}

	@Override
	public void visit(ASTIterationStatement iterationStat) throws Exception {
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;

		for(int i=0;i<iterationStat.init.size();i++){
			visit(iterationStat.init.get(i));
		}
		Integer L1 = cursor;
		for(int i=0;i<iterationStat.cond.size();i++){
			visit(iterationStat.cond.get(i));
		}
		res = new CursorValue(cursor+3+iterationStat.step.size());
		Quat quat1 = new Quat(cursor++,"Jt",res,opnd1,opnd2);
		quats.add(quat1);

		if(iterationStat.stat instanceof ASTCompoundStatement){//循环主体部分不止一个语句
			ASTCompoundStatement cs = (ASTCompoundStatement)iterationStat.stat;
			res = new CursorValue(cursor+3+iterationStat.step.size()+cs.blockItems.size());
		}else{
			res = new CursorValue(cursor+4+iterationStat.step.size());
		}

		Quat quat2 = new Quat(cursor++,"Jf",res,opnd1,opnd2);
		quats.add(quat2);

		Integer L4 = cursor;
		for(int i=0;i<iterationStat.step.size();i++){
			visit(iterationStat.step.get(i));
		}
		res = new CursorValue(L1);
		Quat quat3 = new Quat(cursor++,"J",res,opnd1,opnd2);
		quats.add(quat3);

		visit(iterationStat.stat);
		res = new CursorValue(L4);
		Quat quat4 = new Quat(cursor++,"J",res,opnd1,opnd2);
		quats.add(quat4);
	}

	@Override
	public void visit(ASTLabeledStatement labeledStat) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTMemberAccess memberAccess) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTPostfixExpression postfixExpression) throws Exception {
		String op = postfixExpression.op.value;
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		if (postfixExpression.expr instanceof ASTIdentifier) {
			opnd1 =postfixExpression.expr;
		} else if (postfixExpression.expr instanceof ASTIntegerConstant) {
			opnd1 = postfixExpression.expr;
		}  else {
			// else ...
		}
		res = opnd1;

		// build quat
		Quat quat = new Quat(cursor++,op, res, opnd1, opnd2);
		quats.add(quat);
		map.put(postfixExpression, res);
		
	}

	@Override
	public void visit(ASTReturnStatement returnStat) throws Exception {
		for(int i=0;i<returnStat.expr.size();i++) {
			if (returnStat.expr.get(i) instanceof ASTBinaryExpression) {
				ASTBinaryExpression be = (ASTBinaryExpression) returnStat.expr.get(i);
				visit(be);
				ASTNode res = null;
				ASTNode opnd1 = quats.get(quats.size() - 1).getRes();
				ASTNode opnd2 = null;
				Quat quat = new Quat(cursor++, "return", res, opnd1, opnd2);
				quats.add(quat);
			} else if (returnStat.expr.get(i) instanceof ASTIntegerConstant) {
				ASTIntegerConstant ic = (ASTIntegerConstant) returnStat.expr.get(i);
				ASTNode res = null;
				ASTNode opnd1 = new CursorValue(ic.value);
				ASTNode opnd2 = null;
				Quat quat = new Quat(cursor++, "return", res, opnd1, opnd2);
				quats.add(quat);
			}else if (returnStat.expr.get(i) instanceof ASTIdentifier) {
				ASTIdentifier id = (ASTIdentifier)returnStat.expr.get(i);
				ASTNode res = null;
				ASTNode opnd1 = new CursorValue(id.value);
				ASTNode opnd2 = null;
				Quat quat = new Quat(cursor++,"return", res, opnd1, opnd2);
				quats.add(quat);
			}	else{
				ASTNode res = null;
				ASTNode opnd1 = null;
				ASTNode opnd2 = null;
				Quat quat = new Quat(cursor++,"return", res, opnd1, opnd2);
				quats.add(quat);
			}

		}

		
	}

	@Override
	public void visit(ASTSelectionStatement selectionStat) throws Exception {
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		for(int i=0;i<selectionStat.cond.size();i++){
			if(selectionStat.cond.get(i).getType().equals("Identifier")){//条件是一个字符
				visit((ASTIdentifier)selectionStat.cond.get(i));
			}else if(selectionStat.cond.get(i) instanceof ASTBinaryExpression){
				visit((ASTBinaryExpression)selectionStat.cond.get(i));
			}
		}
		//TODO then和otherwise有多条语句
		res = new CursorValue(cursor+2);
		Quat quat1 = new Quat(cursor++,"Jt",res,opnd1,opnd2);
		quats.add(quat1);

		res = new CursorValue(cursor+2);
		Quat quat2 = new Quat(cursor++,"J",res,opnd1,opnd2);
		quats.add(quat2);
		visit(selectionStat.then);
		visit(selectionStat.otherwise);
		
	}

	@Override
	public void visit(ASTStringConstant stringConst) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTTypename typename) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTUnaryExpression unaryExpression) throws Exception {
		String op = unaryExpression.op.value;
		ASTNode res = new TemporaryValue(++tmpId);
		ASTNode opnd1 =null;
		if(unaryExpression.expr instanceof ASTIdentifier){
			ASTIdentifier id = (ASTIdentifier)unaryExpression.expr;
			opnd1 = id;
		}
		ASTNode opnd2 = null;
		Quat quat = new Quat(cursor++,op,res,opnd1,opnd2);
		quats.add(quat);
		map.put(unaryExpression,res);
		
	}

	@Override
	public void visit(ASTUnaryTypename unaryTypename) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTFunctionDefine functionDefine) throws Exception {
		String temp_type = functionDefine.declarator.getType();

		if(temp_type.equals("FunctionDeclarator")) {
			ASTFunctionDeclarator fd = (ASTFunctionDeclarator) functionDefine.declarator;
			fd.accept(this);
			if(this.table_0.size()!=0){
				this.table_0.get(0).Kind = "FunctionDefine";
				this.table_0.get(0).Type = functionDefine.specifiers.get(0).value;
			}
		}

		String scope_Str = "func &"+this.table_0.get(0).Name+"(";
		//访问函数参数信息，并添加到要输出的字符串中
		for(int i=0;i<this.table_0.get(0).params.size();i++){
			scope_Str+=this.table_0.get(0).params.get(i);
			int temp_para = i+1;//在最后一个表中找参数名称
			scope_Str+=" ";
			scope_Str+=table_0.get(temp_para).Name;

			if(i!=this.table_0.get(0).params.size()-1){
				scope_Str+=",";
			}

		}
		scope_Str+=")";
		Quat quat_scope = new Quat(cursor++,scope_Str);
		quats.add(quat_scope);
		visit(functionDefine.body);
	}

	@Override
	public void visit(ASTDeclarator declarator) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTStatement statement) throws Exception {
		if(statement instanceof ASTIterationDeclaredStatement) {
			visit((ASTIterationDeclaredStatement)statement);
		}else if(statement instanceof ASTIterationStatement) {
			visit((ASTIterationStatement)statement);
		}else if(statement instanceof ASTCompoundStatement) {
			visit((ASTCompoundStatement)statement);
		}else if(statement instanceof ASTSelectionStatement) {
			visit((ASTSelectionStatement)statement);
		}else if(statement instanceof ASTExpressionStatement) {
			visit((ASTExpressionStatement)statement);
		}else if(statement instanceof ASTBreakStatement) {
			visit((ASTBreakStatement)statement);
		}else if(statement instanceof ASTContinueStatement) {
			visit((ASTContinueStatement)statement);
		}else if(statement instanceof ASTReturnStatement) {
			visit((ASTReturnStatement)statement);
		}else if(statement instanceof ASTGotoStatement) {
			visit((ASTGotoStatement)statement);
		}else if(statement instanceof ASTLabeledStatement) {
			visit((ASTLabeledStatement)statement);
		}
	}

	@Override
	public void visit(ASTToken token) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
