package sue.reach.JMX_test;

import java.util.List;

import sue.reach.IPListDTO;

/**
 * JMX Interface
 * @author Takeshi
 *
 */
public interface MessageBoxMBean {
	public void setMessage(String msg);

	public String getMessage();

	public String printMessage();

	public List<IPListDTO> getAllIPListdto();

	public IPListDTO getIPListdto(int index);
	
	public void loadIPlist();
	
	public void clearResultSts();
	
	public int getCount();

	public void setGcInterval(long interval);

	public long getGcInterval();

}
