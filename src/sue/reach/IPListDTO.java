package sue.reach;

/**
 * IPList DTO
 * @author Takeshi
 *
 */
public class IPListDTO {

	/**
	 * コンストラクタ
	 * @param i OS種別(0:Unix 1:Windows)
	 */
	public IPListDTO(int i){
		switch(i){
		case 0:
			countStr=" -c 1";
			break;
		case 1:
			countStr=" -n 1";
			break;
		}

	}
		/**
	 * @return ipv4addr
	 */
	public String getIpv4addr() {
		return ipv4addr;
	}

	/**
	 * @param ipv4addr セットする ipv4addr
	 * @throws IllegalArgumentException
	 */
	public void setIpv4addr(String ipv4addr) {
		if ( ipv4addr == null){
			throw new IllegalArgumentException();
		}
		this.ipv4addr = PINGSTR + ipv4addr + countStr;
	}

	/**
	 * @return interval
	 */
	public String getInterval() {
		return interval;
	}

	/**
	 * @param interval セットする interval
	 */
	public void setInterval(String interval) {
		this.interval = interval;
	}

	/**
	 * @return count
	 */
	public String getCount() {
		return count;
	}

	/**
	 * @param count セットする count
	 */
	public void setCount(String count) {
		this.count = count;
	}

	/**
	 * RuleID
	 */
	private int Ruleid;
		/**
	 * @return ruleid
	 */
	public int getRuleid() {
		return Ruleid;
	}

	/**
	 * @param ruleid セットする ruleid
	 */
	public void setRuleid(int ruleid) {
		Ruleid = ruleid;
	}

		/**
		 * IPv4Address
		 */
		private String ipv4addr;
		
		/**
		 * 試行間隔
		 */
		private String interval;
		
		/**
		 * 試行回数
		 */
		private String count;

		private static String PINGSTR="ping ";
		private String countStr=" -n 1";
}
