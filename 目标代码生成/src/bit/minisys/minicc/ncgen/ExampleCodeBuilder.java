package bit.minisys.minicc.ncgen;

import bit.minisys.minicc.icgen.TableItem;

import java.util.ArrayList;
import java.util.Stack;

public class ExampleCodeBuilder {
    public ArrayList<String> code = new ArrayList<>();
    public String func_name;//当前在哪个函数里
    public int regcount=23;
    public int tmpregcount = 25;
    public int segcount =1;//代码段标识
    public ArrayList<RegItem> regs = new ArrayList<>();
    public Stack<String> regstack = new Stack<>();
    public int stackcount=4;


    //记录汇编代码中代码段标识和四元式中行数的对应关系
    public ArrayList<SegItem> segItemList = new ArrayList<>();

    //记录data中对应的变量名
    public ArrayList<DataItem> dataItemList = new ArrayList<>();

    //记录寄存器和需要恢复的堆栈
    public ArrayList<StackItem> stackItemList = new ArrayList<>();
    public Stack<String> saveStack = new Stack<>();

    public void addvalue(int i,String a,String f){
        RegItem ri = new RegItem(i,a,f);
        regs.add(ri);
    }
    public String get(String a){
        for(int i=0;i<regs.size();i++){
            if(regs.get(i).rvalue.equals(a)&&regs.get(i).function.equals(func_name)){
                return regs.get(i).reg_name;
            }
        }
        for(int i=0;i<regs.size();i++){
            if(regs.get(i).rvalue.equals(a)){
                return regs.get(i).reg_name;
            }
        }
        return null;
    }
    public int renew(int count){
        count-=1;
        if(count<16){
            count=23;
        }
        return count;
    }
    public int renew_tmp(int count){
        count-=1;
        if(count<24){
            count=25;
        }
        return count;
    }
    public boolean isdigit(String s){
        boolean digit = true;
        for(int k=0;k<s.length();k++){//判断字符串内是数字还是标识符
            if(!Character.isDigit(s.charAt(k))){
                digit = false;
            }
        }
        return digit;
    }
    public String getSegflag(String line){
        for(int i=0;i<segItemList.size();i++){
            if(segItemList.get(i).linecount.equals(line)){
                return segItemList.get(i).Segname;
            }
        }
        return "";
    }
    public ExampleCodeBuilder(ArrayList<String> srcLines, ArrayList<ArrayList<TableItem>> table_g ){

        //todo 由全局符号表添加data
        code.add(".data");
        code.add("blank : .asciiz \" \"");
        int string_tmp = 1;
        for(int i=1;i<table_g.size();i++){
            for(int j=1;j<table_g.get(i).size();j++){
                if(table_g.get(i).get(j).Kind.equals("StringConstant")){
                    code.add("_"+string_tmp+"sc : .asciiz "+table_g.get(i).get(j).Name);
                    dataItemList.add(new DataItem("_"+string_tmp+"sc",table_g.get(i).get(j).Name));
                    string_tmp +=1;
                }
            }
        }
        code.add(".text");
        code.add("__init:");
        code.add("\tlui $sp, 0x8000");
        code.add("\taddi $sp, $sp, 0x0000");
        code.add("\tmove $fp, $sp");
        code.add("\tadd $gp, $gp, 0x8000");
        code.add("\tjal main");
        code.add("\tli $v0, 10");
        code.add("\tsyscall");

        code.add("Mars_PrintInt:");
        code.add("\tli $v0, 1");
        code.add("\tsyscall");
        code.add("\tli $v0, 4");
        code.add("\tmove $v1, $a0");
        code.add("\tla $a0, blank");
        code.add("\tsyscall");
        code.add("\tmove $a0, $v1");
        code.add("\tjr $ra");

        code.add("Mars_GetInt:");
        code.add("\tli $v0, 5");
        code.add("\tsyscall");
        code.add("\tjr $ra");

        code.add("Mars_PrintStr:");
        code.add("\tli $v0, 4");
        code.add("\tsyscall");
        code.add("\tjr $ra");

        //分配寄存器
        for(int i=1;i<table_g.size();i++){
            for(int j=1;j<table_g.get(i).size();j++){
                TableItem item= table_g.get(i).get(j);
                if(item.Kind.equals("VariableDeclarator")){
                    addvalue(regcount,item.Name,table_g.get(i).get(0).Name);
                    regcount = renew(regcount);
                }
            }
        }

        for(int i=0;i<srcLines.size();i++){//先扫描一遍跳转语句，将标识和行数加入segItemList
            String four = srcLines.get(i);
            String str = "";
            for(int j=0;j<four.length();j++){
                if(four.charAt(j)=='('){
                    for(int k=j+1;four.charAt(k)!=')';k++){
                        str+=four.charAt(k);
                    }
                }
            }
            String str_a[] = str.split(",");
            if(str_a[0].equals("J")||str_a[0].equals("Jt")||str_a[0].equals("Jf")){
                SegItem segItem = new SegItem("L"+segcount,str_a[3]);
                segItemList.add(segItem);
                segcount+=1;
            }


        }
        for(int i=0;i<srcLines.size();i++){
            ArrayList<String> this_code=new ArrayList<String>();
            String four = srcLines.get(i);
            //判断是否需要添加代码段的标识
            String line = "";
            for(int j=0;four.charAt(j)!=':';j++){
                line+=four.charAt(j);
            }
            for(int j=0;j<segItemList.size();j++){
                if(segItemList.get(j).linecount.equals(line)){
                    this_code.add(segItemList.get(j).Segname+":");
                }
            }


            String str ="";
            if(four.charAt(3)==32){//是一个四元式
                for(int j=0;j<four.length();j++){
                    if(four.charAt(j)=='('){
                        for(int k=j+1;four.charAt(k)!=')';k++){
                            str+=four.charAt(k);
                        }
                    }
                }
                String str_a[] = str.split(",");
                if(str_a[0].equals("<")|| str_a[0].equals("<=")){
                    String tmp = "";
                    String tmp_reg1;
                    if(isdigit(str_a[2])){
                        if(str_a[0].equals("<")){
                            int test = Integer.parseInt((str_a[2]));
                            tmp = "li $"+regcount+", "+test;
                        }
                        else {
                            int test = Integer.parseInt((str_a[2]));
                            test +=1;
                            tmp = "li $" + regcount + ", " + test;
                        }
                        tmp_reg1="$"+regcount;
                        regcount = renew(regcount);
                        this_code.add("\t"+tmp);
                    }else {
                        tmp_reg1 = get(str_a[2]);
                    }
                    tmp="";
                    if(str_a[0].equals("<"))
                        tmp+= ("slt "+"$"+regcount+", ");
                    else if(str_a[0].equals("<="))
                        tmp+= ("slt "+"$"+regcount+", ");
                    regstack.push("$"+regcount);
                    regcount = renew(regcount);
                    String tmp_reg2;
                    if (str_a[1].charAt(0) == '%') {
                        tmp_reg2 = regstack.pop();
                    }else
                        tmp_reg2 = get(str_a[1]);
                    tmp+=tmp_reg2+", "+tmp_reg1;
                    this_code.add("\t"+tmp);
                }else if(str_a[0].equals("==")){
                    String tmp = "";
                    tmp = "li $"+regcount+", "+str_a[2];
                    String tmp_reg1="$"+regcount;
                    regcount = renew(regcount);
                    this_code.add("\t"+tmp);

                    String tmp_reg2 ;
                    if(str_a[1].charAt(0)=='%')
                        tmp_reg2 = regstack.pop();
                    else
                        tmp_reg2= get(str_a[1]);
                    String aim_reg = "$"+regcount;
                    this_code.add("\tsub "+aim_reg+", "+tmp_reg1+", "+tmp_reg2);
                    regstack.push(aim_reg);
                    regcount = renew(regcount);


                }else if(str_a[0].equals("Jt")){
                    String tmp = "";

                    tmp+= ("bne "+regstack.pop()+", $0, "+getSegflag(str_a[3]));
                    //SegItem segItem = new SegItem("L"+segcount,str_a[3]);
                    //segItemList.add(segItem);
                    //segcount+=1;
                    this_code.add("\t"+tmp);
                }else if(str_a[0].equals("J")){
                    String tmp = "";
                    String flag = "";
                    if(str_a[3].length()>=6){
                        for(int j=0;j<5;j++){
                            flag+=str_a[3].charAt(j);
                        }
                        if(flag.equals("endif")){
                            tmp = "j "+"_"+str_a[3];
                        }
                    }else{
                        tmp+= ("j "+getSegflag(str_a[3]));
                        //SegItem segItem = new SegItem("L"+segcount,str_a[3]);
                        //segItemList.add(segItem);
                        //segcount+=1;
                    }
                    this_code.add("\t"+tmp);
                }else if(str_a[0].equals("Call")){
                    String tmp = "";

                    if(str_a[1].equals("Mars_PrintStr")){//输出字符串，不需要获取函数返回结果
                        for(int j=0;j<dataItemList.size();j++){
                            if(str_a[2].equals(dataItemList.get(j).data_value)){
                                this_code.add("\tla $"+regcount+", "+dataItemList.get(j).data_name);
                                regstack.push("$"+regcount);
                                regcount=renew(regcount);
                            }
                        }
                        int back_reg=0;
                        this_code.add("\tsw $"+regcount+", -4($fp)");
                        back_reg = regcount;
                        regcount = renew(regcount);
                        this_code.add("\tsubu $sp, $sp, 4");
                        this_code.add("\tsw $fp, ($sp)");
                        this_code.add("\tmove $fp, $sp");
                        this_code.add("\tsw $31, 20($sp)");
                        this_code.add( "\tmove $4, "+regstack.pop());//传入函数参数
                        tmp= ("\tjal "+str_a[1]);
                        this_code.add(tmp);
                        this_code.add("\tlw $31, 20($sp)");
                        this_code.add("\tlw $fp, ($sp)");
                        this_code.add("\taddu $sp, $sp, 4");
                        this_code.add("\tlw $"+back_reg+", -4($fp)");

                    }else if(str_a[1].equals("Mars_GetInt")){//接收输入，不需要传入参数
                        this_code.add("\tsubu $sp, $sp, 4");
                        this_code.add("\tsw $fp, ($sp)");
                        this_code.add("\tmove $fp, $sp");
                        this_code.add("\tsw $31, 20($sp)");
                        tmp= ("\tjal "+str_a[1]);
                        this_code.add(tmp);
                        this_code.add("\tlw $31, 20($sp)");
                        this_code.add("\tlw $fp, ($sp)");
                        this_code.add("\taddu $sp, $sp, 4");
                        this_code.add("\tmove $"+regcount+", $2");//保存子函数的返回结果
                        regstack.push("$"+regcount);
                        regcount = renew(regcount);


                    }else if(str_a[1].equals("Mars_PrintInt")){//输出整数，不需要查找建立的data
                        this_code.add("\tsubu $sp, $sp, 4");
                        this_code.add("\tsw $fp, ($sp)");
                        this_code.add("\tmove $fp, $sp");
                        this_code.add("\tsw $31, 20($sp)");
                        //判断输出的是变量还是常量
                        boolean isdigit = true;
                        for(int k=0;k<str_a[2].length();k++){//判断字符串内是数字还是标识符
                            if(!Character.isDigit(str_a[2].charAt(k))){
                                isdigit = false;
                            }
                        }
                        if(isdigit){

                        }else{//查找变量所在的寄存器
                            this_code.add( "\tmove $4, "+get(str_a[2]));//传入函数参数

//                            for(int j=0;j<regs.size();j++){
//                                if(func_name.equals(regs.get(j).function)&&regs.get(j).rvalue.equals(str_a[2])){
//                                    this_code.add( "\tmove $4, "+regs.get(j).reg_name);//传入函数参数
//                                    //this_code.add( "\tmove $4, "+regstack.pop());//传入函数参数
//                                    break;
//                                }
//                            }
                        }
                        tmp= ("\tjal "+str_a[1]);
                        this_code.add(tmp);
                        this_code.add("\tlw $31, 20($sp)");
                        this_code.add("\tlw $fp, ($sp)");
                        this_code.add("\taddu $sp, $sp, 4");

                        //this_code.add( "\tmove $4, "+regstack.pop());//传入函数参数
                    }else{//不是系统函数
                        int back_reg=0;
                        String para_name="";
//                        for(int j=regcount;j<=23;j++){
//                            this_code.add("\tsw $"+j+", -"+stackcount+"($fp)");
//                            stackcount+=4;
//                            saveStack.push("$"+j);
//                        }
                        //找参数列表
                        for(int j=0;j<table_g.get(0).size();j++){
                            if(table_g.get(0).get(j).params.size()==1){//函数只有一个参数
                                //this_code.add("\tsw $"+regcount+", -4($fp)");
                                back_reg = regcount;
                                regcount = renew(regcount);

                                for(int k=1;k<table_g.size();k++){
                                    if(table_g.get(k).get(0).Name.equals(str_a[1])){
                                        para_name = table_g.get(k).get(1).Name;
                                    }
                                }

                                this_code.add("\tsw "+get(para_name)+", -4($fp)");
                                this_code.add("\tsubu $sp, $sp, 4");
                                this_code.add("\tsw $fp, ($sp)");
                                this_code.add("\tmove $fp, $sp");
                                this_code.add("\tsw $31, 20($sp)");
                                if(str_a.length<3){
                                    this_code.add( "\tmove $4, "+regstack.pop());//传入函数参数
                                }else{
                                    this_code.add( "\tmove $4, "+get(str_a[2]));
                                }

                            }
                        }
                        tmp= ("\tjal "+str_a[1]);
                        this_code.add(tmp);
                        this_code.add("\tlw $31, 20($sp)");
                        this_code.add("\tlw $fp, ($sp)");
                        this_code.add("\taddu $sp, $sp, 4");
                        this_code.add("\tlw "+get(para_name)+", -4($fp)");
                        //this_code.add("\tlw $"+back_reg+", -4($fp)");
                        this_code.add("\tmove $"+back_reg+", $2");//保存子函数的返回结果
                        regstack.push("$"+back_reg);
                        regcount = renew(regcount);

//                        for(;!saveStack.empty();){
//                            stackcount-=4;
//                            this_code.add("\tlw "+saveStack.pop()+", -"+stackcount+"($fp)");
//                        }
                    }



                }else if(str_a[0].equals("return")){
                    //判断返回值是变量还是常量
                    boolean isdigit = true;
                    for(int k=0;k<str_a[1].length();k++){//判断字符串内是数字还是标识符
                        if(!Character.isDigit(str_a[1].charAt(k))){
                            isdigit = false;
                        }
                    }
                    if(isdigit){
                        this_code.add("\tli $"+tmpregcount+", "+str_a[1]);
                        this_code.add("\tmove $2, $"+tmpregcount);
                        this_code.add("\tmove $sp, $fp");
                        this_code.add("\tjr $31");
                        tmpregcount=renew_tmp(tmpregcount);
                    }else{//需要返回的是变量
                        this_code.add("\tmove $2, "+get(str_a[1]));
                        this_code.add("\tmove $sp, $fp");
                        this_code.add("\tjr $31");
                    }

                }else if(str_a[0] .equals( "+")){
                    String tmp = "\tadd ";
                    if(str_a[1].equals("")&&str_a[2].equals("")){//两个寄存器中的内容相加
                        tmp+=get(str_a[3]);
                        tmp+=", "+regstack.pop();
                        tmp+=", "+regstack.pop();

                    }
                    this_code.add(tmp);
                }else if(str_a[0] .equals( "-")){
                    String tmp = "\tsub ";
                    for(int j=1;j<3;j++){
                        boolean isdigit = true;
                        for(int k=0;k<str_a[j].length();k++){//判断字符串内是数字还是标识符
                            if(!Character.isDigit(str_a[j].charAt(k))){
                                isdigit = false;
                            }
                        }
                        if(isdigit){
                            this_code.add("\tli $"+regcount+", "+str_a[j]);//是数字就先存到寄存器中
                            regstack.push("$"+regcount);
                            regcount = renew(regcount);
                        }else{
                            regstack.push(get(str_a[j]));//是变量就将变量所在的寄存器入栈
                            this_code.add("\tsw "+get(str_a[j])+", -4($fp)");
                        }
                    }
                    tmp+="$"+regcount;
                    regcount = renew(regcount);
                    String reverse = regstack.pop();
                    tmp+= ", "+regstack.pop();
                    tmp+=", "+reverse;

                    this_code.add(tmp);
                    regstack.push("$"+(regcount+1));//将减法结果入栈
                }else if(str_a[0] .equals( "=")){
                    String tmp = "";
                    if(str_a[1].equals("")){//赋值语句没有操作数，出栈
                        tmp +="move "+get(str_a[3])+", "+regstack.pop();
                        //tmp +="move "+get(str_a[3])+", "+"$"+(regcount+1);
                        this_code.add("\t"+tmp);
                    }else{
                        tmp +="li "+get(str_a[3])+", "+str_a[1];
                        this_code.add("\t"+tmp);
                    }

                }else if(str_a[0] .equals( "*")){
                    String tmp = "\tmul ";
                    for(int j=1;j<3;j++){
                        boolean isdigit = true;
                        for(int k=0;k<str_a[j].length();k++){//判断字符串内是数字还是标识符
                            if(!Character.isDigit(str_a[j].charAt(k))){
                                isdigit = false;
                            }
                        }
                        if(isdigit){
                            this_code.add("\tli $"+regcount+", "+str_a[j]);//是数字就先存到寄存器中
                            regstack.push("$"+regcount);
                            regcount = renew(regcount);
                        }else{
                            regstack.push(get(str_a[j]));//是变量就将变量所在的寄存器入栈
                        }
                    }
                    tmp+="$"+regcount;
                    regcount = renew(regcount);
                    String reverse = regstack.pop();
                    tmp+= ", "+regstack.pop();
                    tmp+=", "+reverse;
                    this_code.add(tmp);
                    regstack.push("$"+(regcount+1));//将乘法结果入栈

                }else if(str_a[0] .equals( "%")){
                    String tmp = "\trem ";
                    for(int j=1;j<3;j++){
                        boolean isdigit = true;
                        for(int k=0;k<str_a[j].length();k++){//判断字符串内是数字还是标识符
                            if(!Character.isDigit(str_a[j].charAt(k))){
                                isdigit = false;
                            }
                        }
                        if(isdigit){
                            this_code.add("\tli $"+regcount+", "+str_a[j]);//是数字就先存到寄存器中
                            regstack.push("$"+regcount);
                            regcount = renew(regcount);
                        }else{
                            regstack.push(get(str_a[j]));//是变量就将变量所在的寄存器入栈
                        }
                    }
                    tmp+="$"+regcount;
                    regcount = renew(regcount);
                    String reverse = regstack.pop();
                    tmp+= ", "+regstack.pop();
                    tmp+=", "+reverse;
                    this_code.add(tmp);
                    regstack.push("$"+(regcount+1));//将结果入栈
                }
            }else{//是一个函数声明
                for(int j=0;j<four.length();j++){
                    if(four.charAt(j)=='&'){
                        for(int k=j+1;four.charAt(k)!='(';k++){
                            str+=four.charAt(k);
                        }
                    }
                }
                String temp_name = func_name;
                func_name = str;
                if(func_name.equals("main")){//是主函数
                    this_code.add("main:");
                    this_code.add("\tsubu $sp, $sp, 28");
//                    this_code.add("");
//                    this_code.add("");
//                    this_code.add("");
//                    this_code.add("");
//                    this_code.add("");
//                    this_code.add("");

                }else{
                    if(func_name.equals("")){//endif标志
                        func_name = temp_name;
                        String tmp = "";
                        for(int j=0;j<four.length();j++){
                            if(four.charAt(j)==':'){
                                for(int k=j+1;k<four.length();k++){
                                    tmp+=four.charAt(k);
                                }
                                break;
                            }
                        }
                        this_code.add(tmp);
                    }else{
                        this_code.add(func_name+":");
                        this_code.add("\tsubu $sp, $sp, 32");
                        for(int j=0;j<table_g.get(0).size();j++){
                            if(table_g.get(0).get(j).params.size()==1){//函数只有一个参数
                                this_code.add("\tmove $25, $4");//接收函数参数
                                this_code.add("\tmove $"+regcount+", $25");//接收函数参数
                                regcount = renew(regcount);
                                String para_name="";//函数参数名
                                for(int m=1;m<table_g.size();m++){
                                    if(table_g.get(m).get(0).Name.equals(func_name)){
                                        para_name=table_g.get(m).get(1).Name;
                                    }
                                }
                                for(int k=0;k<regs.size();k++){
                                    if(regs.get(k).rvalue.equals(para_name)&&regs.get(k).function.equals(func_name)){
                                        regs.get(k).reg_name="$"+(regcount+1);
                                    }
                                }
                            }
                        }
                    }


                }
            }
            code.addAll(this_code);

        }

    }
}
