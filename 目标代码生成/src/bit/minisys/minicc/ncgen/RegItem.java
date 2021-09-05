package bit.minisys.minicc.ncgen;

public class RegItem {
    public String reg_name;
    public String rvalue;
    public String function;
    RegItem(int i,String a,String f){
        reg_name = "$"+i;
        rvalue = a;
        function = f;
    }
}
