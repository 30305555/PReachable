package sue.reach;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Reachability Test
 * @author Takeshi
 *
 */
public class ReachabilityTest {
	public static int COUNT = 2000;
    public static final int TIMEOUT = 1200;
    public static long count = 0;

    /**
     * コンストラクタ
     * @param paddressText address
     * @param pcount count
     */
    public ReachabilityTest(String paddressText, int pcount) {
        try {
            long sum = 0;
            int num = 0;
            int ok = 0;
            int ng = 0;
            long st = 0;
            long ed = 0;

            COUNT = pcount;
            st = System.currentTimeMillis();

            InetAddress address = InetAddress.getByName(paddressText);
            Sendping[] sendp = new Sendping[COUNT];

            System.out.println("Pinging " + address.getHostName()
                               + " [" + address.getHostAddress() + "].\n");

            int i = 0;
            for (; i < COUNT; i++) {
            	try {
            		if(i % 5 == 0){
            			Thread.sleep(10);
            		}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            	sendp[i] = new Sendping(address);
            	Runnable r = sendp[i];
            	Thread thr = new Thread(r);

            	thr.start();
            }
            System.out.println("Send End");
            System.out.println("Analyse Start!!");

            while(true){
            	try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            	for(int j = 0; j< COUNT;j++){
            		if( sendp[j].getStatus() != 2){
	            		if( sendp[j].getStatus() == 1){
	            			ok++;
	            			sum++;
	            			sendp[j].setStatus(2);
	            		} else if (sendp[j].getStatus() == -1){
	            			ng++;
	            			sum++;
	            			sendp[j].setStatus(2);
	            		}
            		}
            	}
            	if ( sum >=COUNT){
            		break;
            	}
            	num++;
            }
            ed = System.currentTimeMillis();
            System.out.println("Thread OK");
            System.out.println("Thread count:"+ReachabilityTest.count);
            System.out.println("OK cnt:"+ok);
            System.out.println("NG cnt:"+ng);
            System.out.println("Analyse cnt:"+num);
            System.out.println("Ping time:"+(ed-st));


        } catch (UnknownHostException ex) {
            System.err.println("Unknown host " + paddressText + ".");
        }
    }

    public static void main(String[] args) {
    	try{
    		new ReachabilityTest(args[0], Integer.parseInt(args[1]));
    	}catch(Exception e){
    		System.out.println(e.toString());
    	}
    }
}
/**
 * java ping.
 * @author Takeshi
 *
 */
class Sendping implements Runnable {
	private InetAddress address;
	private int status = 0;
	/**
	 * status set
	 * @param status
	 */
	public void setStatus(int status) {
		this.status = status;
	}
	/**
	 * status get
	 * @return int 
	 */
	public int getStatus() {
		return status;
	}
	/**
	 * java ping constructor
	 * @param args
	 */
	public Sendping( InetAddress args ) {
		this.address = args;
	}
	/**
	 * Java Reachable
	 */
	@Override
	public void run() {

		boolean result = false;
		this.status = 0;

		try {
			// jdk Reachable test
			result = this.address.isReachable(ReachabilityTest.TIMEOUT);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if(result){
			this.status = 1;
		} else {
			this.status = -1;
		}
		// 実施件数カウンタ
		ReachabilityTest.count++;
	}
}

/**
 * send ping 2 
 * ping command version
 * @author Takeshi
 *
 */
class Sendping2 extends Thread {
	private InetAddress address;
	private int status = 0;

	/**
	 * ステータスの設定
	 * @param status
	 */
	public void setStatus(int status) {
		this.status = status;
	}
	/**
	 * ステータスの取得
	 * @return
	 */
	public int getStatus() {
		return status;
	}
	/**
	 * コンストラクタ
	 * @param args
	 */
	public Sendping2( InetAddress args ) {
		this.address = args;
	}

	/**
	 * タスク
	 */
	@Override
	public void run() {
		super.run();

		this.status = 0;
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec("ping -n 1 " + this.address);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this .status = proc.exitValue();
	}
}
