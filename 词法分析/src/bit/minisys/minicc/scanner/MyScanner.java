package bit.minisys.minicc.scanner;

import java.util.ArrayList;
import java.util.HashSet;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


enum DFI_STATE{
    DFA_STATE_INITIAL,//初始状态
    DFA_STATE_ID_0,//
    DFA_STATE_ID_1,
    DFA_STATE_KB_O,
    DFA_STATE_KB_C,
    DFA_STATE_P_O,
    DFA_STATE_P_C,
    DFA_STATE_ADD_0,//+
    DFA_STATE_SM,
    DFA_STATE_CONS,//数字常量
    DFA_STATE_CONS2,//转义符号
    DFA_STATE_CONS3,//字符常量
    DFA_STATE_STRL,//字符串面常量
    DFA_STATE_CALC,//运算符

    DFA_STATE_UNKNW

}

public class MyScanner implements IMiniCCScanner {

    private int lIndex = 0;
    private int cIndex = 0;
    private int tIndex = 0;//在整个文件中的索引
    private int outflag=1;//当前行是否已输出

    private ArrayList<String> srcLines;

    private HashSet<String> keywordSet;
    private HashSet<String> calcuSet;

    public MyScanner(){
        this.keywordSet = new HashSet<String>();
        this.keywordSet.add("auto");
        this.keywordSet.add("break");
        this.keywordSet.add("case");
        this.keywordSet.add("char");
        this.keywordSet.add("const");
        this.keywordSet.add("continue");
        this.keywordSet.add("default");
        this.keywordSet.add("do");
        this.keywordSet.add("double");
        this.keywordSet.add("else");
        this.keywordSet.add("enum");
        this.keywordSet.add("extern");
        this.keywordSet.add("float");
        this.keywordSet.add("for");
        this.keywordSet.add("goto");
        this.keywordSet.add("if");
        this.keywordSet.add("inline");
        this.keywordSet.add("int");
        this.keywordSet.add("long");
        this.keywordSet.add("register");
        this.keywordSet.add("restrict");
        this.keywordSet.add("return");
        this.keywordSet.add("short");
        this.keywordSet.add("signed");
        this.keywordSet.add("sizeof");
        this.keywordSet.add("static");
        this.keywordSet.add("struct");
        this.keywordSet.add("switch");
        this.keywordSet.add("typedef");
        this.keywordSet.add("union");
        this.keywordSet.add("unsigned");
        this.keywordSet.add("void");
        this.keywordSet.add("volatile");
        this.keywordSet.add("while");


        this.calcuSet = new HashSet<String>();
        this.calcuSet.add("--");this.calcuSet.add("->");this.calcuSet.add("-");this.calcuSet.add("-=");
        this.calcuSet.add("/");this.calcuSet.add("/=");
        this.calcuSet.add("%");this.calcuSet.add("%=");this.calcuSet.add("%>");this.calcuSet.add("%:");this.calcuSet.add("%:%:");
        this.calcuSet.add("==");this.calcuSet.add("=");
        this.calcuSet.add("!=");this.calcuSet.add("!");
        this.calcuSet.add("?");
        this.calcuSet.add(":");this.calcuSet.add(":>");
        this.calcuSet.add("*");this.calcuSet.add("*=");
        this.calcuSet.add(">>=");this.calcuSet.add(">>");this.calcuSet.add(">=");this.calcuSet.add(">");
        this.calcuSet.add("&=");this.calcuSet.add("&");this.calcuSet.add("&&");
        this.calcuSet.add("||");this.calcuSet.add("|");this.calcuSet.add("|=");
        this.calcuSet.add("^");this.calcuSet.add("^=");
        this.calcuSet.add(".");this.calcuSet.add("...");
        this.calcuSet.add("~");
        this.calcuSet.add("#");this.calcuSet.add("##");
        this.calcuSet.add("<<");this.calcuSet.add("<<=");this.calcuSet.add("<");this.calcuSet.add("<=");

    }

    private char getNextChar() {
        char c = Character.MAX_VALUE;
        while(true) {
            if(lIndex < this.srcLines.size()) {
                String line = this.srcLines.get(lIndex);
                if(cIndex < line.length()) {
                    c = line.charAt(cIndex);
                    cIndex++;
                    tIndex++;
                    break;
                }else {//换了一行
                    //如果当前行没有进行过输出，则强制再循环一遍，否则读取下一行
                    if(outflag==0){
                        c=9;
                        cIndex++;

                        break;
                    }
                    else{
                        outflag=0;
                        lIndex++;
                        cIndex = 0;
                        tIndex+=2;
                    }

                }
            }else {
                break;

            }
        }
        if(c == '\u001a') {
            c = Character.MAX_VALUE;
        }
        return c;
    }

    private boolean isAlpha(char c) {
        return Character.isAlphabetic(c);
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private boolean isOct(char c) {//是否是八进制
        return (Character.isDigit(c)&&c!='8'&&c!='9');

    }

    private boolean isCalcu(char c){//是否是运算符
        if(c=='-'||c=='/'||c=='%'||c=='='||c=='!'||c=='?'||c==':'||c=='*'||c=='>'||c=='<'||c=='&'||c=='|'||c=='^'||c=='.'||c=='~'||c=='#')
            return TRUE;
        else  return FALSE;
    }

    private boolean isAlphaOrDigit(char c) {
        return Character.isLetterOrDigit(c);
    }

    private boolean isDigitSymbol(char c) {
        if(c=='u'||c=='U'||c=='l'||c=='L'||c=='f'||c=='x'||c=='X'||c=='A'||c=='B'||c=='C'||c=='D'||c=='E'||c=='F'||c=='e'||c=='.'||c=='p'||c=='P'||c=='a')
            return  TRUE;
        else return FALSE;
    }

    private boolean isEscape(char c){
//        if(c=='\''||c=='\"'||c=='\b'||c=='\f'||c=='\n'||c=='\r'||c=='\t'||c=='\\'||c=='?')
        if(c=='n'||c==39||c==34||c=='?'||c=='a'||c=='b'||c=='f'||c=='r'||c=='t'||c=='v')//简单转义序列
            return  TRUE;
        else if(isOct(c))//八进制转义序列
            return TRUE;
        else if(c=='x')//十六进制转义序列
            return  TRUE;
        else return FALSE;
    }


//    private String genToken(int num, String lexme, String type) {
//        return genToken(num, lexme, type, this.cIndex - 1, this.lIndex);
//    }
//    private String genToken2(int num, String lexme, String type) {
//        return genToken(num, lexme, type, this.cIndex - 2, this.lIndex);
//    }
//
//    private String genToken(int num, String lexme, String type, int cIndex, int lIndex) {
//        String strToken = "";
//
//        strToken += "[@" + num + "," + (cIndex - lexme.length() + 1) + ":" + cIndex;
//        strToken += "='" + lexme + "',<" + type + ">," + (lIndex + 1) + ":" + (cIndex - lexme.length() + 1) + "]\n";
//
//        return strToken;
//    }


    private String genToken(int num, String lexme, String type) {

        return genToken(num, lexme, type, this.cIndex - 1, this.lIndex,this.tIndex-1);
    }
    private String genToken2(int num, String lexme, String type) {

        return genToken(num, lexme, type, this.cIndex - 2, this.lIndex,this.tIndex-2);
    }
    private String genToken3(int num, String lexme, String type) {

        String strToken = "";

        strToken += "[@" + num + "," + (tIndex) + ":" + (tIndex-1);
        strToken += "='" + lexme + "',<" + type + ">," + (lIndex + 1) + ":" + (cIndex  ) + "]\n";
        return strToken;

    }

    private String genToken(int num, String lexme, String type, int cIndex, int lIndex,int tIndex) {
        outflag=1;
        String strToken = "";

        strToken += "[@" + num + "," + (tIndex - lexme.length() + 1) + ":" + tIndex;
        strToken += "='" + lexme + "',<" + type + ">," + (lIndex + 1) + ":" + (cIndex - lexme.length() + 1) + "]\n";

        return strToken;
    }

    @Override
    public String run(String iFile) throws Exception {

        System.out.println("Scanning...");
        String strTokens = "";
        int iTknNum = 0;
        int token_len=0;

        this.srcLines = MiniCCUtil.readFile(iFile);

        DFI_STATE state = DFI_STATE.DFA_STATE_INITIAL;		//FA state
        String lexme 	= "";		//token lexme
        char c 			= ' ';		//next char
        boolean keep 	= false;	//keep current char
        boolean end 	= false;

        while(!end) {				//scanning loop
            if(!keep) {
                c = getNextChar();
            }

            keep = false;

            switch(state) {
                case DFA_STATE_INITIAL:
                    lexme = "";
                    if(c==9) {//读到\t
//                        tIndex+=2;
                    }else if(c==32){//读到空格
//                        tIndex+=1;
                    }
                    else if(isAlpha(c)||c==95) {//c为字母或下划线
                        state = DFI_STATE.DFA_STATE_ID_0;
                        lexme = lexme + c;
                    }else if(isDigit(c)){
                        state = DFI_STATE.DFA_STATE_CONS;
                        lexme = lexme + c;
                    }else if(c==92){//c='\'
                        state = DFI_STATE.DFA_STATE_CONS2;
                        lexme = lexme + c;
                    }else if(c == '+') {
                        state = DFI_STATE.DFA_STATE_ADD_0;
                        lexme = lexme + c;
                    }else if(c == '{') {
                        strTokens += genToken(iTknNum, "{", "'{'");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }else if(c == '}') {
                        strTokens += genToken(iTknNum, "}", "'}'");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }else if(c == '(') {
                        strTokens += genToken(iTknNum, "(", "'('");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }else if(c == ')') {
                        strTokens += genToken(iTknNum, ")", "')'");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }else if(c == '['){
                        strTokens += genToken(iTknNum, "[", "'['");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }else if(c == ']'){
                        strTokens += genToken(iTknNum, "]", "']'");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }
                    else if(c == ';') {
                        strTokens += genToken(iTknNum, ";", "';'");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }
                    else if(c == ',') {
                        strTokens += genToken(iTknNum, ",", "','");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }
                    else if(c==39){//读到单引号
                        lexme = lexme+c;

                        state = DFI_STATE.DFA_STATE_CONS3;

                    }
                    else if(isCalcu(c)) {
                        state = DFI_STATE.DFA_STATE_CALC;
                        lexme = lexme + c;
                    }
                    else if(c==34) {//"开头
                        state = DFI_STATE.DFA_STATE_STRL;
                        lexme = lexme + c;
                    }
                    else if(Character.isSpace(c)) {

                    }else if(c == Character.MAX_VALUE) {
                        cIndex = 1;
                        tIndex-=2;
                        lIndex-=1;
                        strTokens += genToken3(iTknNum, "<EOF>", "EOF");
                        end = true;
                    }
                    break;
                case DFA_STATE_ADD_0:
                    if(c == '+'||c=='=') {
                        //TODO:++
                        lexme=lexme+c;

                    }else if(lexme.equals("+")){
                        strTokens += genToken2(iTknNum, "+", "'+'");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }else if(lexme.equals("++")){
                        strTokens += genToken2(iTknNum, "++", "'++'");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }else if(lexme.equals("+=")){
                        strTokens += genToken2(iTknNum, "+=", "'+='");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    //state = DFI_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_ID_0:
                    if(isAlphaOrDigit(c)||c==95) {//字母/数字/下划线
                        lexme = lexme + c;
                    }
//                    else if(c==10){
//                        lexme = lexme + c;
//                    }
                    else if((lexme.equals("u8")||lexme.equals("u")||lexme.equals("U")||lexme.equals("L"))&&c==34){//前缀+双引号
                        lexme = lexme+c;
                        state = DFI_STATE.DFA_STATE_STRL;
                    }
                    else if((lexme.equals("u")||lexme.equals("U")||lexme.equals("L"))&&c==39){//前缀+单引号
                        lexme = lexme+c;
                        state = DFI_STATE.DFA_STATE_CONS3;
                    }else if(c==9){//在id内出现了换行符：该id没有及时结束
                        if(this.keywordSet.contains(lexme)) {
                            tIndex++;
                            strTokens += genToken2(iTknNum, lexme, "'" + lexme + "'");
                            tIndex--;
                        } else{
                            tIndex++;
                            strTokens += genToken2(iTknNum, lexme, "Identifier");
                            tIndex--;
                        }
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    else {
                        if(this.keywordSet.contains(lexme)) {
                            strTokens += genToken2(iTknNum, lexme, "'" + lexme + "'");
                        } else{
                            strTokens += genToken2(iTknNum, lexme, "Identifier");
                        }
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_CONS:
                    if(isDigit(c)||isDigitSymbol(c)) {
                        lexme = lexme + c;
                    }  else{
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_CONS2:
                    if(isEscape(c)) {
                        lexme = lexme + c;
                    }else{
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_CONS3:
                    if(c==92){//可能是转义符
                        lexme = lexme + c;
                    }
                    else if(c==39){//字符常量结束
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }
                    else if(c==10) {//换行符
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    else{
                        lexme = lexme+c;
                    }
                    break;
                case DFA_STATE_CALC:
                    if(isCalcu(c)) {
                        lexme = lexme + c;
                    }else{
                        if(this.calcuSet.contains(lexme)) {
                            strTokens += genToken2(iTknNum, lexme, "'" + lexme + "'");
                        }else {
                            strTokens += genToken2(iTknNum, lexme, "Identifier");
                        }
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_STRL:
                    if(c==92){//可能是转义符
                        lexme = lexme + c;

                    }
                    else if(c==34){//字符串面结束
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "StringLiteral");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                    }
                    else if(c==10) {//换行符
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFI_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    else{
                        lexme = lexme+c;
                    }
                    break;
                default:
                    System.out.println("[ERROR]Scanner:line " + lIndex + ", column=" + cIndex + ", unreachable state!");
                    break;
            }
        }


        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
        MiniCCUtil.createAndWriteFile(oFile, strTokens);

        return oFile;
    }

}
