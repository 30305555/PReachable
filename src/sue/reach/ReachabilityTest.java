package sue.reach;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class ReachabilityTest {
	public static int COUNT = 2000;
    public static final int TIMEOUT = 1200;
    public static long count = 0;
//    public enum Start {Sato, Sue, Take, Yamaguchi};

    public ReachabilityTest(String paddressText, int pcount) {
        try {
            long max = 0;
            long min = 0;
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
					// TODO 自動生成された catch ブロック
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
					// TODO 自動生成された catch ブロック
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
        } catch (IOException ex) {
            System.err.println("Network Error Occurred.");
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
class Sendping implements Runnable {
	private InetAddress address;
	private int status = 0;
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
	public Sendping( InetAddress args ) {
		// TODO 自動生成されたコンストラクター・スタブ
		this.address = args;
	}
	@Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ
//		super.run();
		boolean result = false;
		this.status = 0;

		try {
			result = this.address.isReachable(ReachabilityTest.TIMEOUT);
		} catch (Throwable e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		if(result){
			this.status = 1;
		} else {
			this.status = -1;
		}
		ReachabilityTest.count++;
	}
}

class Sendping2 extends Thread {
	private InetAddress address;
	private int status = 0;
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
	public Sendping2( InetAddress args ) {
		// TODO 自動生成されたコンストラクター・スタブ
		this.address = args;
	}
	@Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ
		super.run();
		boolean result = false;
		int read = 0;
		char readc;

		this.status = 0;
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec("ping -n 1 -4");
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		if(result){
			this.status = 1;
		} else {
			this.status = -1;
		}
	}
}
class SenderStatus {
	private int status = 0;
}