package bit.minisys.minicc.semantic;

import java.util.ArrayList;

public class TableItem {
    public String Name;
    public String Type;
    public String Kind;
    public int Scope_entry;
    public int Scope_exit;
    public ArrayList<String> params=new ArrayList<>();
    public ArrayList<TableItem> sub_table;
}
