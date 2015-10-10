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
	 * 実行
	 * @param ReachManager 生成済みのファクトリ
	 * @return　int
	 */
	private int aSyncGo(ReachManager manager){

		// マネージャの確認
		if ( manager == null ) {
			try{
				manager = ReachManager.getInstance(os);
			} catch( Exception e){
				e.printStackTrace();
				System.err.println("Factoryの生成に失敗したおそれがあります。");
				System.exit(-1);
			}
		}
		
		// ファクトリよりIPリストの件数を取得
		int ipcnt = manager.getIPlistcnt();

		if ( ipcnt == 0 ) {
			System.err.println("IPリストが存在しません");
		} else {

			// 処理カウンタを初期化
			procCount = 0;
			
			//IPリスト数非同期実行を行う
			while( procCount < ipcnt ){
				try {
					
					// IPListDTOの取得
					IPListDTO dto = manager.getIPList(procCount);
					if ( dto == null ) {
						System.err.println("IPListDTO ["+procCount+"] is null." );
						procCount++;
						break;
					}
					// 非同期実行の生成 (IPリスト数の非同期実行)
					new AsyncPing(dto,manager,procCount,os).start();
	
					// interval
					Thread.sleep(ASYNC_INTERVAL);
					procCount++;
	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// 処理カウントを保持
		this.cnt = procCount;
		return procCount;

	}
	/**
	 * 試験の実施
	 * @param os OS種別(1:unix, 2:win)
	 */
	private void runTestReach(int os){

		this.os = os;
		
        // マネージャの生成
		ReachManager manager = ReachManager.getInstance(os);

		// MBean Agentのサーバの起動・MBeanの登録
		new MessageAgent(manager);
		System.out.println("MessageAgent 実行中");

		// PING結果解析スレッドの起動(定期的な分析処理の刈り取り)
		new PingAnalizer(manager).start();
		
        // 初回実行
		aSyncGo(manager);

		// 2回目以降はタイマー実行
		Timer t = new Timer();
		TimerTaskRun task = new TimerTaskRun();
		// スケジュールの登録
        t.schedule(task, 20000,20000);

        // Gavage Collect Interval
        long gcinterval = 0;
        
		int slpcnt = 0;
		int prntcnt = 0;
		
		// GCインターバルの取得
		gcinterval = manager.getGcInterval();
		
		while(true){
			try {
				Thread.sleep(1);
				slpcnt++;
				if ( gcinterval < slpcnt  || manager.isGc()){
					// Gavage Collect 非同期実行w
					new Thread(){
						public void run(){
							System.gc();
						}
					}.start();
					manager.setGc(false);
					slpcnt = 0;
				}
				
				// 印刷カウントのインクリメント
				prntcnt++;
				if ( 10000 < prntcnt){
					prntcnt=0;
					for(int j =0; j<this.cnt;j++){
						closeopen(manager);

						if ( manager.getIPlistcnt() > 0) {
							// リスト出力開始
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
	 * pingerメイン
	 * @param args (0:Unix 1:Windows7)
	 */
	public static void main(String[] args) {

		TestReach reach = new TestReach();
		reach.runTestReach(Integer.parseInt(args[0]));

	}

}
