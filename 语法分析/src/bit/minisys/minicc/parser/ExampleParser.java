package bit.minisys.minicc.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.antlr.v4.gui.TreeViewer;

import com.fasterxml.jackson.databind.ObjectMapper;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.*;

/*
 * PROGRAM     --> FUNC_LIST
 * FUNC_LIST   --> FUNC FUNC_LIST | e
 * FUNC        --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
 * TYPE        --> INT
 * ARGS   	   --> e | ARG_LIST
 * ARG_LIST    --> ARG ',' ARGLIST | ARG
 * ARG    	   --> TYPE ID
 * CODE_BLOCK  --> '{' STMTS '}'
 * STMTS       --> STMT STMTS | e
 * STMT        --> RETURN_STMT
 *
 * RETURN STMT --> RETURN EXPR ';'
 *
 * EXPR        --> TERM EXPR'
 * EXPR'       --> '+' TERM EXPR' | '-' TERM EXPR' | e
 *
 * TERM        --> FACTOR TERM'
 * TERM'       --> '*' FACTOR TERM' | e
 *
 * FACTOR      --> ID  
 * 
 */

class ScannerToken{
	public String lexme;
	public String type;
	public int	  line;
	public int    column;
}

public class ExampleParser implements IMiniCCParser {

	private ArrayList<ScannerToken> tknList;
	private int tokenIndex;
	private ScannerToken nextToken;
	
	@Override
	public String run(String iFile) throws Exception {
		System.out.println("Parsing...");

		String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_PARSER_OUTPUT_EXT;
		String tFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
		
		tknList = loadTokens(tFile);
		tokenIndex = 0;

		ASTNode root = program();
		
		
		String[] dummyStrs = new String[16];
		TreeViewer viewr = new TreeViewer(Arrays.asList(dummyStrs), root);
	    viewr.open();

		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File(oFile), root);

		//TODO: write to file
		
		
		return oFile;
	}
	

	private ArrayList<ScannerToken> loadTokens(String tFile) {
		tknList = new ArrayList<ScannerToken>();
		
		ArrayList<String> tknStr = MiniCCUtil.readFile(tFile);
		
		for(String str: tknStr) {
			if(str.trim().length() <= 0) {
				continue;
			}
			
			ScannerToken st = new ScannerToken();
			//[@0,0:2='int',<'int'>,1:0]
			String[] segs;
			if(str.indexOf("<','>") > 0) {
				str = str.replace("','", "'DOT'");
				
				segs = str.split(",");
				segs[1] = "=','";
				segs[2] = "<','>";
				
			}else {
				segs = str.split(",");
			}
			st.lexme = segs[1].substring(segs[1].indexOf("=") + 1);
			st.type  = segs[2].substring(segs[2].indexOf("<") + 1, segs[2].length() - 1);
			String[] lc = segs[3].split(":");
			st.line = Integer.parseInt(lc[0]);
			st.column = Integer.parseInt(lc[1].replace("]", ""));
			
			tknList.add(st);
		}
		
		return tknList;
	}

	private ScannerToken getToken(int index){
		if (index < tknList.size()){
			return tknList.get(index);
		}
		return null;
	}

	public void matchToken(String type) {
		if(tokenIndex < tknList.size()) {
			ScannerToken next = tknList.get(tokenIndex);
			if(!next.type.equals(type)) {
				System.out.println("[ERROR]Parser: unmatched token, expected = " + type + ", " 
						+ "input = " + next.type);
			}
			else {
				tokenIndex++;
			}
		}
	}

	//PROGRAM --> FUNC_LIST
	public ASTNode program() {
		ASTCompilationUnit p = new ASTCompilationUnit();
		ArrayList<ASTNode> fl = funcList();
		if(fl != null) {
			//p.getSubNodes().add(fl);
			p.items.addAll(fl);
		}
		p.children.addAll(p.items);
		return p;
	}

	//FUNC_LIST --> FUNC FUNC_LIST | e
	public ArrayList<ASTNode> funcList() {
		ArrayList<ASTNode> fl = new ArrayList<ASTNode>();
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("EOF")) {
			return null;
		}
		else {
			ASTNode f = func();
			fl.add(f);
			ArrayList<ASTNode> fl2 = funcList();
			if(fl2 != null) {
				fl.addAll(fl2);
			}
			return fl;
		}
	}

	//FUNC --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
	public ASTNode func() {
		ASTFunctionDefine fdef = new ASTFunctionDefine();
		
		ASTToken s = type();
		
		fdef.specifiers.add(s);
		fdef.children.add(s);
		
		ASTFunctionDeclarator fdec = new ASTFunctionDeclarator();
		fdec.declarator = declor();
//		ASTIdentifier id = new ASTIdentifier();
//		id.tokenId = tokenIndex;
//		matchToken("Identifier");
//		fdef.children.add(id);
		
		matchToken("'('");
		ArrayList<ASTParamsDeclarator> pl = arguments();
		matchToken("')'");
		
		//fdec.identifiers.add(id);
		if(pl != null) {
			fdec.params.addAll(pl);
			fdec.children.addAll(pl);
		}
		
		ASTCompoundStatement cs = codeBlock();

		fdef.declarator = fdec;
		fdef.children.add(fdec);
		fdef.body = cs;
		fdef.children.add(cs);

		
		return fdef;
	}

	//TYPE --> INT |FLOAT | CHART
	public ASTToken type() {
		ScannerToken st = tknList.get(tokenIndex);
		
		ASTToken t = new ASTToken();
		if(st.type.equals("'int'")) {
			t.tokenId = tokenIndex;
			t.value = st.lexme;
			tokenIndex++;
		}
		return t;
	}

	//ARGUMENTS --> e | ARG_LIST
	public ArrayList<ASTParamsDeclarator> arguments() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("')'")) { //ending
			return null;
		}
		else {
			ArrayList<ASTParamsDeclarator> al = argList();
			return al;
		}
	}

	//ARG_LIST --> ARGUMENT ',' ARGLIST | ARGUMENT
	public ArrayList<ASTParamsDeclarator> argList() {
		ArrayList<ASTParamsDeclarator> pdl = new ArrayList<ASTParamsDeclarator>();
		ASTParamsDeclarator pd = argument();
		pdl.add(pd);
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("','")) {
			matchToken("','");
			ArrayList<ASTParamsDeclarator> pdl2 = argList();
			pdl.addAll(pdl2);
		}
		
		return pdl;
	}
		
	//ARGUMENT --> TYPE ID
	public ASTParamsDeclarator argument() {
		ASTParamsDeclarator pd = new ASTParamsDeclarator();
		ASTToken t = type();
		pd.specfiers.add(t);
		
		ASTIdentifier id = new ASTIdentifier();
		id.tokenId = tokenIndex;
		id.value = nextToken.lexme;
		//id.children.add(id.value);
		matchToken("Identifier");
		
		ASTVariableDeclarator vd =  new ASTVariableDeclarator();
		vd.identifier = id;
		pd.declarator = vd;
		pd.children.add(vd);
		vd.children.add(id);
		
		return pd;
	}

	

	//CODE_BLOCK --> '{' STMTS '}'
	public ASTCompoundStatement codeBlock() {
		ASTCompoundStatement cs = new ASTCompoundStatement();
		nextToken = tknList.get(tokenIndex);
		if(!nextToken.type.equals("'{'")){
			System.out.println("[ERROR]Parser: unmatched token, expected = {"  + ", "
					+ "input = " + nextToken.type);
		}
		matchToken("'{'");
		LinkedList<ASTNode> blockItems = new LinkedList<>();
		nextToken = tknList.get(tokenIndex);
		while(!nextToken.type.equals("'}'")){
			if(nextToken.type.equals("'int'")){//局部变量声明
				ASTDeclaration dec = decl();
				blockItems.add(dec);
				nextToken = tknList.get(tokenIndex);
				matchToken("';'");
			}
			else{//复合语句
				ASTCompoundStatement comstmt = stmts();
				blockItems.add(comstmt);
			}
			nextToken = tknList.get(tokenIndex);
		}
		matchToken("'}'");
		cs.blockItems = blockItems;
		return cs;

	}

	//STMTS --> STMT STMTS | e
	public ASTCompoundStatement stmts() {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'}'"))
			return null;
		else {
			ASTCompoundStatement cs = new ASTCompoundStatement();
			ASTStatement s = stmt();
			cs.blockItems.add(s);
			
			ASTCompoundStatement cs2 = stmts();
			if(cs2 != null)
				cs.blockItems.add(cs2);
			return cs;
		}
	}

	//STMT --> ASSIGN_STMT | RETURN_STMT | DECL_STMT | FUNC_CALL
	public ASTStatement stmt() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'return'")) {
			return returnStmt();
		}else if(nextToken.type.equals("'if'")){
			return selstmt();
		}else if(nextToken.type.equals("'for'")){
			nextToken = tknList.get(tokenIndex+2);
			if(nextToken.type.equals("'int'")){
				nextToken = tknList.get(tokenIndex);
				return itedstmt();
			}else{
				nextToken = tknList.get(tokenIndex);
				return itestmt();
			}
		}else if(nextToken.type.equals("Identifier")){
			//表达式语句
			ASTExpressionStatement estmt = new ASTExpressionStatement();
			LinkedList<ASTExpression> exprs = new LinkedList<>();

			ASTExpression expr = expr();
			nextToken = tknList.get(tokenIndex);
			if(expr!=null){
				while(nextToken.type.equals("';'")){
					nextToken = tknList.get(tokenIndex);
					exprs.add(expr);
					matchToken("';'");
					expr = expr();
				}
				estmt.exprs = exprs;
				return estmt;
			}
			else{//只有一个标识符
				return null;
			}
		}
		else{
			System.out.println("[ERROR]Parser: unreachable stmt!");
			return null;
		}
	}

	//RETURN_STMT --> RETURN EXPR ';'
	public ASTReturnStatement returnStmt() {
		matchToken("'return'");
		ASTReturnStatement rs = new ASTReturnStatement();
		ASTExpression e = expr();
		matchToken("';'");
		rs.expr.add(e);
		return rs;
	}

	//EXPR --> TERM EXPR'
	public ASTExpression expr() {
		ASTExpression term = term();
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'='"))//是一个赋值表达式
		{
			nextToken = tknList.get(tokenIndex);
			ASTAssignmentExpression be = assexpr();
			if(be != null) {
				be.id = (ASTIdentifier) term;
				return be;
			}else {
				return term;
			}
		}else if(nextToken.type.equals("'<'")||nextToken.type.equals("'>'")){//是一个关系表达式或者条件表达式
			nextToken = tknList.get(tokenIndex+2);
			if(nextToken.type.equals("'?'")){//条件表达式
				nextToken = tknList.get(tokenIndex);
				ASTRelationalExpression re = relexpr();
				ASTConditionExpression ce = conexpr();
				if(re!=null){
					re.expr1=term;
					ce.condExpr = re;
				}
				return ce;
			}
			else{//关系表达式
				nextToken = tknList.get(tokenIndex);
				ASTRelationalExpression re = relexpr();
				if(re!=null){
					re.expr1=term;
					return re;
				}else {
					return term;
				}
			}

		}
		else{//二元表达式：+-*/
			nextToken = tknList.get(tokenIndex);
			ASTBinaryExpression be = expr2();
			if(be != null) {
				be.expr1 = term;
				return be;
			}else {
				return term;
			}
		}
	}

	//EXPR' --> '+' TERM EXPR' | '-' TERM EXPR' | e
	public ASTBinaryExpression expr2() {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("';'"))
			return null;
		
		if(nextToken.type.equals("'+'")||nextToken.type.equals("'-'")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = nextToken.type;
			if(nextToken.type.equals("'+'"))
				matchToken("'+'");
			else if(nextToken.type.equals("'-'"))
				matchToken("'-'");
			be.op = tkn;
			be.expr2 = term();
			
			ASTBinaryExpression expr = expr2();
			if(expr != null) {
				expr.expr1 = be;
				return expr;
			}
			
			return be;
		}
		else {
			return null;
		}
	}





	//TERM --> FACTOR TERM2
	public ASTExpression term() {

		nextToken=tknList.get(tokenIndex);
		ASTExpression f = factor();
		ASTBinaryExpression be = term2();

		if(be != null) {
			be.expr1 = f;
			return be;
		}else {
			return f;
		}
//		}else{
//			return null;
//		}

	}

	//TERM'--> '*' FACTOR TERM' | '/' FACTOR TERM' | e
	public ASTBinaryExpression term2() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'*'")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			matchToken("'*'");
			
			be.op = tkn;
			be.expr2 = factor();
			
			ASTBinaryExpression term = term2();
			if(term != null) {
				term.expr1 = be;
				return term;
			}
			return be;
		}else {
			return null;
		}
	}

	//FACTOR --> '(' EXPR ')' | ID | CONST | FUNC_CALL
	public ASTExpression factor() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("Identifier")) {
			ASTIdentifier id = new ASTIdentifier();
			id.tokenId = tokenIndex;
			id.value = nextToken.lexme;
			matchToken("Identifier");
			return id;
		}else if(nextToken.type.equals("IntegerConstant")){
			ASTIntegerConstant intc = new ASTIntegerConstant();
			intc.tokenId = tokenIndex;
			intc.value = Integer.valueOf(nextToken.lexme.charAt(1))-48;
			matchToken("IntegerConstant");
			return intc;
		}else {
			return null;
		}
	}

	public ASTSelectionStatement selstmt(){
		if(nextToken.type.equals("'if'")) {
			matchToken("'if'");
			nextToken = tknList.get(tokenIndex);
			ASTSelectionStatement sel = new ASTSelectionStatement();
			if(nextToken.type.equals("'('")) {
				matchToken("'('");
				LinkedList<ASTExpression> elistr = new LinkedList<>();
				elistr.add(expr());
				//只添加了一个表达式
				sel.cond = elistr;
				matchToken("')'");
				nextToken = tknList.get(tokenIndex);
			}else {
				System.out.println("[ERROR]Parser:selectionStatement unmatched token, expected = ("  + ", "
						+ "input = " + nextToken.type);
			}
			if(nextToken.type.equals("'{'")){
				sel.then = codeBlock();
				//matchToken("'{'");
				//nextToken = tknList.get(tokenIndex);
			}else{
				sel.then = stmt();
			}
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("'else'")){
				matchToken("'else'");
				nextToken = tknList.get(tokenIndex);
				if(nextToken.type.equals("'{'")){
					sel.otherwise = codeBlock();
				}else {
					sel.otherwise = stmt();
				}
			}
			return sel;
		}else {
			return null;
		}
	}
	public ASTIterationStatement itestmt(){
		if(nextToken.type.equals("'for'")){
			matchToken("'for'");
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("'('")){
				matchToken("'('");
			}else {
				System.out.println("[ERROR]Parser:iterationStatement unmatched token, expected = ("  + ", "
						+ "input = " + nextToken.type);
			}

			nextToken = tknList.get(tokenIndex);

			ASTIterationStatement ite = new ASTIterationStatement();
			LinkedList<ASTExpression> init = new LinkedList<>();
			LinkedList<ASTExpression> cond = new LinkedList<>();
			LinkedList<ASTExpression> step = new LinkedList<>();
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("')'")){
				System.out.println("[ERROR]Parser:iterationStatement lack cond,step, expected token= ;"  + ", "
						+ "input = " + nextToken.type);
			}

			while(!nextToken.type.equals("';'")){
				init.add(expr());
				nextToken = tknList.get(tokenIndex);
				if(nextToken.type.equals("','")){
					matchToken("','");
				}
			}
			matchToken("';'");
			nextToken = tknList.get(tokenIndex);
			while(!nextToken.type.equals("';'")){
				cond.add(expr());
				nextToken = tknList.get(tokenIndex);
				if(nextToken.type.equals("','")){
					matchToken("','");
				}
			}
			matchToken("';'");
			nextToken = tknList.get(tokenIndex);
			while(!nextToken.type.equals("')'")){
				step.add(expr());
				nextToken = tknList.get(tokenIndex);
				if(nextToken.type.equals("','")){
					matchToken("','");
				}
			}
			matchToken("')'");
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("'{'")){
				ASTCompoundStatement stat = codeBlock();
				ite.stat = stat;
			}else{
				ite.stat = stmt();
			}
			ite.cond = cond;
			ite.init = init;
			ite.step = step;


			return ite;




		}else {
			return null;
		}
	}

	public ASTIterationDeclaredStatement itedstmt(){
		matchToken("'for'");
		nextToken = tknList.get(tokenIndex);
		matchToken("'('");
		nextToken = tknList.get(tokenIndex);
		ASTIterationDeclaredStatement ite = new ASTIterationDeclaredStatement();
		ASTDeclaration init = new ASTDeclaration();
		LinkedList<ASTExpression> cond = new LinkedList<>();
		LinkedList<ASTExpression> step = new LinkedList<>();
		init = decl();
		matchToken("';'");
		nextToken = tknList.get(tokenIndex);
		while(!nextToken.type.equals("';'")){
			cond.add(expr());
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("','")){
				matchToken("','");
			}
		}
		matchToken("';'");
		nextToken = tknList.get(tokenIndex);
		while(!nextToken.type.equals("')'")){
			step.add(expr());
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("','")){
				matchToken("','");
			}
		}
		matchToken("')'");
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'{'")){
			ASTCompoundStatement stat = codeBlock();
			ite.stat = stat;
		}else{
			ite.stat = stmt();
		}
		ite.cond = cond;
		ite.init = init;
		ite.step = step;


		return ite;


	}

	//ASSIGN_STMT --> ID = EXPR
	public ASTAssignmentStatement assign_stmt(){
		ASTAssignmentStatement assstmt = new ASTAssignmentStatement();
		nextToken = tknList.get(tokenIndex);
		ASTIdentifier id = new ASTIdentifier();
		id.value =  nextToken.lexme;
		id.tokenId = tokenIndex;
		assstmt.id = id;
		matchToken("Identifier");
		nextToken = tknList.get(tokenIndex);

		ASTToken tkn = new ASTToken();
		tkn.tokenId = tokenIndex;
		tkn.value = "=";
		matchToken("'='");
		assstmt.op = tkn;

		nextToken = tknList.get(tokenIndex);
		assstmt.expr = expr();

		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("';'"))
			matchToken("';'");

		return assstmt;
	}

	public ASTUnaryExpression unaexpr(){
		return null;
	}
	public ASTConditionExpression conexpr(){
		ASTConditionExpression cond = new ASTConditionExpression();
		nextToken = tknList.get(tokenIndex);
		if(!nextToken.type.equals("'?'")){
			System.out.println("[ERROR]Parser:conditionExpression unmatched token, expected = ?"  + ", "
					+ "input = " + nextToken.type);
			return null;
		}
		else{
			matchToken("'?'");
			nextToken = tknList.get(tokenIndex);
			LinkedList<ASTExpression> trueexpr = new LinkedList<>();
			while(!nextToken.type.equals("':'")){
				trueexpr.add(expr());
				nextToken = tknList.get(tokenIndex);
			}
			cond.trueExpr = trueexpr;
			matchToken("':'");
			nextToken = tknList.get(tokenIndex);
			cond.falseExpr = expr();
			nextToken = tknList.get(tokenIndex);
			if(!nextToken.type.equals("';'")){
				System.out.println("[ERROR]Parser:conditionExpression unmatched token, expected = ;"  + ", "
						+ "input = " + nextToken.type);
				return null;
			}

			return cond;
		}
	}
	public ASTAssignmentExpression assexpr(){
		nextToken = tknList.get(tokenIndex);
		ASTAssignmentExpression ae = new ASTAssignmentExpression();
		ASTToken tkn = new ASTToken();
		if(nextToken.type.equals("'='")){
			tkn.tokenId = tokenIndex;
			tkn.value = nextToken.type;
			matchToken("'='");
		}
		ae.op = tkn;
		ae.expr = expr();
		return ae;
	}

	public ASTRelationalExpression relexpr(){
		nextToken = tknList.get(tokenIndex);
		ASTRelationalExpression re = new ASTRelationalExpression();
		ASTToken tkn = new ASTToken();
		tkn.tokenId = tokenIndex;
		if(nextToken.type.equals("'<'")){
			tkn.value = nextToken.type;
			matchToken("'<'");
		}else if(nextToken.type.equals("'>'")){
			tkn.value = nextToken.type;
			matchToken("'>'");
		}
		re.op = tkn;
		re.expr2 = expr();
		return re;
	}

	public ASTDeclaration decl(){
		nextToken = tknList.get(tokenIndex);
		ASTDeclaration dc = new ASTDeclaration();
		LinkedList<ASTToken> specifiers =  new LinkedList<>();
		ASTToken specifier = new ASTToken();
		specifier.tokenId = tokenIndex;
		specifier.value = nextToken.type;
		matchToken(nextToken.type);
		specifiers.add(specifier);
		dc.specifiers = specifiers;

		nextToken = tknList.get(tokenIndex);
		LinkedList<ASTInitList> initLists =  new LinkedList<>();


		do{
			ASTInitList initList = new ASTInitList();
			initList.declarator = declor();
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("'='")) {//声明后面接有初始化列表
				matchToken("'='");
				nextToken = tknList.get(tokenIndex);
				if(nextToken.type.equals("'{'")){//数组初始化
					matchToken("'{'");
					LinkedList<ASTExpression> inlist_exprs = new LinkedList<>();
					while (!nextToken.type.equals("'}'")) {
						ASTExpression expr = expr();
						inlist_exprs.add(expr);
						nextToken = tknList.get(tokenIndex);
						if (nextToken.type.equals("','")) {
							matchToken("','");
						}
						nextToken = tknList.get(tokenIndex);
					}
					initList.exprs = inlist_exprs;
					matchToken("'}'");
				}else{
					LinkedList<ASTExpression> inlist_exprs = new LinkedList<>();
					while (!nextToken.type.equals("';'")) {
						ASTExpression expr = expr();
						inlist_exprs.add(expr);
						nextToken = tknList.get(tokenIndex);
					}
					initList.exprs = inlist_exprs;
				}


			}

			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("','")){
				matchToken("','");
			}
			initLists.add(initList);
		} while(nextToken.type.equals("','"));


		dc.initLists = initLists;

//		nextToken= tknList.get(tokenIndex);
//		if(nextToken.type.equals("';'")){
//			matchToken("';'");
//		}
		return dc;
	}
	public ASTDeclarator declor(){

		nextToken= tknList.get(tokenIndex);
		if(nextToken.type.equals("Identifier")){
			nextToken = tknList.get(tokenIndex+1);
			if(nextToken.type.equals("'['")){//数组
				nextToken = tknList.get(tokenIndex);
				ASTVariableDeclarator vd = new ASTVariableDeclarator();
				ASTIdentifier id = new ASTIdentifier();
				id.value = nextToken.lexme;
				id.tokenId = tokenIndex;
				matchToken("Identifier");
				vd.identifier = id;

				nextToken= tknList.get(tokenIndex);
				ASTArrayDeclarator ad = new ASTArrayDeclarator();
				ad.declarator = vd;
				nextToken= tknList.get(tokenIndex);
				matchToken("'['");
				ad.expr = expr();
				nextToken= tknList.get(tokenIndex);
				if(!nextToken.type.equals("']'")){
					System.out.println("[ERROR]Parser: unmatched token, expected = ]"  + ", "
							+ "input = " + nextToken.type);
				}else{
					matchToken("']'");
				}
				return ad;
			}
			else{//标识符
				nextToken= tknList.get(tokenIndex);
				ASTVariableDeclarator vd = new ASTVariableDeclarator();
				ASTIdentifier id = new ASTIdentifier();
				id.value = nextToken.lexme;
				id.tokenId = tokenIndex;
				matchToken("Identifier");
				vd.identifier = id;
				return vd;
			}

		}else{
			return null;
		}

	}
}
