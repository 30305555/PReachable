package sue.reach;

import java.io.Serializable;

public class ResultDTO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5771570697846471892L;
	
	public int Ruleid;
	public int cnt;
	public int okcnt;
	public java.util.Date procStartDateTime;
	public java.util.Date procEndDateTime;
	public StackTraceElement[] errStack;
	public String msg;
}
