package bit.minisys.minicc.ncgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.icgen.TableItem;
import bit.minisys.minicc.icgen.internal.IRBuilder;
import bit.minisys.minicc.icgen.internal.MiniCCICGen;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.ncgen.IMiniCCCodeGen;


public class ExampleCodeGen implements IMiniCCCodeGen{
	private ArrayList<String> srcLines;

	public ExampleCodeGen() {
		
	}
	
	@Override
	public String run(String iFile, ArrayList<ArrayList<TableItem>> table_g ,MiniCCCfg cfg) throws Exception {
		String oFile = MiniCCUtil.remove2Ext(iFile) + MiniCCCfg.MINICC_CODEGEN_OUTPUT_EXT;
		this.srcLines = MiniCCUtil.readFile(iFile);
		if(cfg.target.equals("mips")) {
			ExampleCodeBuilder cb = new ExampleCodeBuilder(srcLines,table_g);
			FileWriter fileWriter = new FileWriter(new File(oFile));
			for(int i=0;i<cb.code.size();i++){
				fileWriter.write(cb.code.get(i));
				fileWriter.write("\r\n");
			}
			fileWriter.close();
		}else if (cfg.target.equals("riscv")) {
			//TODO:
		}else if (cfg.target.equals("x86")){
		}

		System.out.println("7. Target code generation finished!");


		
		return oFile;
	}
}
