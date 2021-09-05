package bit.minisys.minicc.ncgen;

public class StackItem {
    public String stack_name;
    public String regname;
    public String function;
    StackItem(int i,String reg){
        stack_name = "-"+i+"($fp)";
        regname=reg;
    }
}
