package sue.reach;

import java.util.Timer;
import java.util.TimerTask;

import sue.reach.JMX_test.MessageAgent;
import sue.reach.async.AsyncPing;
import sue.reach.async.PingAnalizer;

public class TestReach {

	private static final int ASYNC_INTERVAL = 50;
	private int os = 0;
	private int cnt = 0;
	
	class TimerTaskRun extends TimerTask {
		TimerTaskRun(){
		}
		@Override
		public void run() {
			try{
				aSyncGo();
			} catch ( Error err){
				err.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	private int procCount = 0;

	/**
	 * 
	 * @return
	 */
	private int aSyncGo() {
		return aSyncGo(null);
	}
	/**
	 * ���s
	 * @param ReachManager �����ς݂̃t�@�N�g��
	 * @return�@int
	 */
	private int aSyncGo(ReachManager manager){

		// �}�l�[�W���̊m�F
		if ( manager == null ) {
			try{
				manager = ReachManager.getInstance(os);
			} catch( Exception e){
				e.printStackTrace();
				System.err.println("Factory�̐����Ɏ��s���������ꂪ����܂��B");
				System.exit(-1);
			}
		}
		
		// �t�@�N�g�����IP���X�g�̌������擾
		int ipcnt = manager.getIPlistcnt();

		if ( ipcnt == 0 ) {
			System.err.println("IP���X�g�����݂��܂���");
		} else {

			// �����J�E���^��������
			procCount = 0;
			
			//IP���X�g���񓯊����s���s��
			while( procCount < ipcnt ){
				try {
					
					// IPListDTO�̎擾
					IPListDTO dto = manager.getIPList(procCount);
					if ( dto == null ) {
						System.err.println("IPListDTO ["+procCount+"] is null." );
						procCount++;
						break;
					}
					// �񓯊����s�̐��� (IP���X�g���̔񓯊����s)
					new AsyncPing(dto,manager,procCount,os).start();
	
					// interval
					Thread.sleep(ASYNC_INTERVAL);
					procCount++;
	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// �����J�E���g��ێ�
		this.cnt = procCount;
		return procCount;

	}
	/**
	 * �����̎��{
	 * @param os OS���(1:unix, 2:win)
	 */
	private void runTestReach(int os){

		this.os = os;
		
        // �}�l�[�W���̐���
		ReachManager manager = ReachManager.getInstance(os);

		// MBean Agent�̃T�[�o�̋N���EMBean�̓o�^
		new MessageAgent(manager);
		System.out.println("MessageAgent ���s��");

		// PING���ʉ�̓X���b�h�̋N��(����I�ȕ��͏����̊�����)
		new PingAnalizer(manager).start();
		
        // ������s
		aSyncGo(manager);

		// 2��ڈȍ~�̓^�C�}�[���s
		Timer t = new Timer();
		TimerTaskRun task = new TimerTaskRun();
		// �X�P�W���[���̓o�^
        t.schedule(task, 20000,20000);

        // Gavage Collect Interval
        long gcinterval = 0;
        
		int slpcnt = 0;
		int prntcnt = 0;
		
		// GC�C���^�[�o���̎擾
		gcinterval = manager.getGcInterval();
		
		while(true){
			try {
				Thread.sleep(1);
				slpcnt++;
				if ( gcinterval < slpcnt  || manager.isGc()){
					// Gavage Collect �񓯊����sw
					new Thread(){
						public void run(){
							System.gc();
						}
					}.start();
					manager.setGc(false);
					slpcnt = 0;
				}
				
				// ����J�E���g�̃C���N�������g
				prntcnt++;
				if ( 10000 < prntcnt){
					prntcnt=0;
					for(int j =0; j<this.cnt;j++){
						closeopen(manager);

						if ( manager.getIPlistcnt() > 0) {
							// ���X�g�o�͊J�n
							System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
							try {
								System.out.println("Rule:"+manager.getCacheStatus(j).Ruleid);
							} catch ( NullPointerException nullex) {
								System.out.println("row:"+j);
								System.out.println("IpAddr:"+manager.getIPList(j).getIpv4addr()+" count:"+manager.getIPList(j).getCount()+" interval:"+manager.getIPList(j).getInterval());
								System.out.println("rule proc not end.");
								continue;
							}
							System.out.println("IpAddr:"+manager.getIPList(j).getIpv4addr()+" count:"+manager.getIPList(j).getCount()+" interval:"+manager.getIPList(j).getInterval());
							System.out.println("-----------------");
							System.out.println(manager.getCacheStatus(j).okcnt+"/"+manager.getCacheStatus(j).cnt);
							System.out.println("Start:"+manager.getCacheStatus(j).procStartDateTime);
							System.out.println("End  :"+manager.getCacheStatus(j).procEndDateTime);
							if ( manager.getCacheStatus(j).errStack != null){
								System.out.println(manager.getCacheStatus(j).msg);
								for(int k =0; k<manager.getCacheStatus(j).errStack.length;k++){
									System.out.println(manager.getCacheStatus(j).errStack[k].toString());
								}
							}
						}  else {
							Thread.sleep(100);
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				
			} catch ( NullPointerException nullp) {
				nullp.printStackTrace();
				System.exit(-1);
			}
		}
	}
	private void closeopen(ReachManager factory){
		factory.reOpen();
	}

	/**
	 * pinger���C��
	 * @param args (0:Unix 1:Windows7)
	 */
	public static void main(String[] args) {

		TestReach reach = new TestReach();
		reach.runTestReach(Integer.parseInt(args[0]));

	}

}
