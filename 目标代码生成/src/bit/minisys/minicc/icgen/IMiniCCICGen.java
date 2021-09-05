package bit.minisys.minicc.icgen;

import java.io.IOException;

public interface IMiniCCICGen {
	/*
	 * @return String the path of the output file
	 * @param iFile input file path
	 */
	public Returnarg run(String iFile) throws Exception;
}
