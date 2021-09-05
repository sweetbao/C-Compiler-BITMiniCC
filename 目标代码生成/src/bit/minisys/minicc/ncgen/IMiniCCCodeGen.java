package bit.minisys.minicc.ncgen;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.icgen.TableItem;

import java.util.ArrayList;

public interface IMiniCCCodeGen {
	/*
	 * @return String the path of the output file
	 * @param iFile input file path
	 * @param type architecture
	 */

	public String run(String iFile, ArrayList<ArrayList<TableItem>> table_g ,MiniCCCfg cfg) throws Exception;
}
