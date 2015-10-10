package sue.reach.async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import sue.reach.ReachManager;
import sue.reach.ResultDTO;

/**
 * Ping結果解析君
 * @author Takeshi
 *
 */
public class PingAnalizer extends Thread {

	private static String ANA_KEYWORD="パケット数:";
	private static String RECEIVE_KEYWORD="受信 = ";
	private static String ERR_KEYWORD="損失 = ";
	private static final int DEQUEUE_INTERVAL = 100;
	private ReachManager manager;

	/**
	 * コンストラクタ
	 * @param manager
	 */
	public PingAnalizer(ReachManager manager) {
		this.manager = manager;
	}
	/**
	 * readProcess
	 */
	public void run() {
	
		// 刈り取りループ
		while (true) {
			
			// 分析キューより刈り取り
			AsyncPing ping = this.manager.dequeue();

			if ( ping == null ) {
				// なし
				try {
					// インターバルを置いて再刈り取りを行う
					Thread.sleep(DEQUEUE_INTERVAL);
					continue;
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			
			// プロセスリストの取得
			List<Process> processList = ping.getProcs();
		
			int okcnt = 0;
			
			// プロセスリスト数くり返し
			for ( Process proc : processList ) {
				
				// プロセス終了待ち(現状無限待ち) TODO:無限待ちを改善したい
				try {
					proc.waitFor ();
				} catch (InterruptedException e) {
					// 割り込み例外
					e.printStackTrace();
					// 例外対応
					errInterrupted(ping.getResultDto(), e);
					continue;
				}

				// 戻り値取得
				int exitvalue = proc.exitValue();

				// プロセスのストリームから実行結果を取得
				BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));			
				StringBuilder builder = new StringBuilder();

				while(true){
					try {
						// スリープ TODO:外出し検討
						Thread.sleep(5);
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}

					// TODO:別メソッドに処理を移管する
					String str;
					try {
						str = reader.readLine();
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
					if (str == null){
						break;
					}
					if ( "".equals(str)){
						continue;
					}
					// 収集分析
					okcnt += clctAnalyse(str)[0];
					builder.append(str+"\n");
				}
				
				try {
					reader.close();
				} catch (IOException e) {
					errInterrupted(ping.getResultDto(), e);
					e.printStackTrace();
					continue;
				}
				
				if ( exitvalue != 0){
					// コマンド実行エラー
					ResultDTO resultDto = ping.getResultDto();
					
					// Ping例外
					resultDto.okcnt = 0;
					resultDto.procEndDateTime = new java.util.Date();
					resultDto.errStack = null;
					resultDto.msg = builder.toString();

					continue;
				}

				ResultDTO resultDto = ping.getResultDto();
				// 成功回数を解析
				resultDto.okcnt = okcnt;
				resultDto.procEndDateTime = new java.util.Date();

				//正常
				resultDto.errStack = null;
				resultDto.msg = "OK.";
				
				// マネージャキャッシュへ設定
				this.manager.putCacheStatus(resultDto.Ruleid,resultDto);

				
			}
		}
	}

	/**
	 * 割り込み例外返却情報生成
	 * @param t Throwable
	 * @param cnt 施行回数
	 * @param ruleid
	 * @return
	 */
	private void errInterrupted(ResultDTO resultDto, Throwable t) {

		// Ping例外
		resultDto.okcnt = 0;
		resultDto.procEndDateTime = new java.util.Date();
		resultDto.errStack = t.getStackTrace();
		resultDto.msg = t.getMessage();

		// マネージャキャッシュ設定
		this.manager.putCacheStatus(resultDto.Ruleid,resultDto);
	}

	/**
	 *  分析処理
	 * @param str
	 * @return int array
	 */
	private int[] clctAnalyse(String str) {

		int retval[] = {0,0};

		// 該当行かチェック
		if ( str.indexOf(ANA_KEYWORD) == -1){
			// スルーpoint
			return retval;
		} 
		try{
			// 受信位置取得
			retval[0] = getCount(str,str.indexOf(RECEIVE_KEYWORD),RECEIVE_KEYWORD.length());
			
			// 損失位置取得
			retval[1] = getCount(str,str.indexOf(ERR_KEYWORD),ERR_KEYWORD.length());
		} catch ( Exception e){
			System.err.println("PINGMSG["+str+"] RCVKWD["+RECEIVE_KEYWORD+"] ERRKWD["+ERR_KEYWORD+"]");
		}
		return retval;
	}

	/**
	 * 結果文字列から件数を抜き出す
	 * @param str
	 * @param ana_point
	 * @param kwdlength
	 * @return 結果
	 */
	private int getCount(String str, int ana_point,int kwdlength){

		// 抜き出し開始index
		int stindex = ana_point+kwdlength;
		// 抜き出し終了index
		int edindex = ana_point + kwdlength + 3;
		try{
			// 抜き出しtry1回目
			return Integer.parseInt(str.substring(stindex,edindex));
		} catch (Exception ex){
			try{
				edindex--;
				// 抜き出しtry2回目
				return Integer.parseInt(str.substring(stindex,edindex));
			} catch( Exception ex2){
				try{
					edindex--;
					// 抜き出しtry3回目
					return Integer.parseInt(str.substring(stindex,edindex));
				} catch (Exception exc){
					throw new RuntimeException();
				}
			}
		}

	}
}
