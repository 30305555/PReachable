package sue.reach.async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sue.reach.IPListDTO;
import sue.reach.ReachManager;
import sue.reach.ResultDTO;


/**
 * PING非同期実行＆分析クラス
 * @author Takeshi
 *
 */
public class AsyncPing extends Thread {

	
	private static String ANA_KEYWORD="パケット数:";
	private static String RECEIVE_KEYWORD="受信 = ";
	private static String ERR_KEYWORD="損失 = ";

	// ランタイムの取得
	private Runtime myruntime = Runtime.getRuntime();
	
	// プロセスリスト
	private List<Process> procs = new LinkedList<Process>();

	/**
	 * プロセスリストを取得します.
	 * @return procs
	 */
	public List<Process> getProcs() {
		return procs;
	}
	// ResultDto生成
	private ResultDTO resultDto = new ResultDTO();

	/**
	 * @return resultDto
	 */
	public ResultDTO getResultDto() {
		return resultDto;
	}
	private IPListDTO listdto = null;
	
	// マネージャ
	private ReachManager factory = null;

	// 現在のルールID
	private int ruleid = 0;

	/**
	 *  コンストラクタ
	 * @param listdto IPリストDTO
	 * @param manager ReachManager
	 * @param index 当該処理index
	 * @param os os種別
	 */
	public AsyncPing(IPListDTO listdto,ReachManager factory,int index, int os){

		this.listdto = listdto;
		this.factory = ReachManager.getInstance(os);
		this.ruleid = factory.getIPList(index).getRuleid();
		
		switch(os){
		case 0:
			// Unix
			ANA_KEYWORD="packets transmitted";
			RECEIVE_KEYWORD="transmitted, ";
			ERR_KEYWORD="received, ";
			break;
		case 1:
			// Win7
			ANA_KEYWORD="パケット数:";
			RECEIVE_KEYWORD="受信 = ";
			ERR_KEYWORD="損失 = ";
			break;
		}
	}
	/**
	 *  プロセスの読み込み
	 *  PingAnalizerに委譲したため、利用非推奨
	 * @return 件数
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Deprecated
	public int readProc() throws InterruptedException, IOException{

		int okcnt = 0;
		for (Iterator<Process> ite = procs.iterator(); ite.hasNext();) {
			// 次のプロセスを取得
			Process proc = (Process)ite.next();
			// スレッド待ち
			proc.waitFor();
			int exitvalue = proc.exitValue();
			
//				System.out.println(this.factory.getIPList(this.ruleid).getIpv4addr()+" Thread Exit Value:"+exitvalue);

			// プロセスのストリームから実行結果を取得
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));			
			StringBuilder builder = new StringBuilder();
			while(true){
				Thread.sleep(5);
				String str = reader.readLine();
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
			reader.close();
			if ( exitvalue != 0){
				// コマンド実行エラー
//					System.err.println(builder.toString());
				throw new InterruptedException(builder.toString());
			}
		}
		
		return okcnt;
	}
	/**
	 *  分析
	 * @param str
	 * @return
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
	
	/**
	 * 非同期Main処理
	 */
	public void run(){

		// 非同期開始マークの出力
		System.out.println(".:" + this.getId());


		// 処理カウンタ
		int cnt=0;

		try{
		
			// 試行回数の取得
			cnt = Integer.parseInt(this.factory.getIPList(this.ruleid).getCount());

			// ルールIDを設定
			resultDto.Ruleid = ruleid;
			// 開始情報の取得
			resultDto.procStartDateTime = new java.util.Date();
			// 施行回数を設定
			resultDto.cnt = cnt;
			
			// 試行回数分繰り返す
			for ( int i = 0; i< cnt ;i++){

				// ping 実行（別pingプロセス）
				Process proc = myruntime.exec(listdto.getIpv4addr());

				// process を追加
				procs.add(proc);
				// 試行間隔sleep
				Thread.sleep(Long.parseLong(this.listdto.getInterval()));
			}

			// 分析依頼キューに詰め込む
			factory.enqueue(this);
			
		} catch (NumberFormatException numex ){
			// 定義例外
			resultDto.cnt = cnt;
			resultDto.okcnt = 0;
			resultDto.Ruleid = ruleid;
			resultDto.procEndDateTime = new java.util.Date();
			resultDto.errStack = numex.getStackTrace();
			resultDto.msg = numex.getMessage();
		} catch (InterruptedException Irpe) {
			// Ping例外
			resultDto.cnt = cnt;
			resultDto.okcnt = 0;
			resultDto.Ruleid = ruleid;
			resultDto.procEndDateTime = new java.util.Date();
			resultDto.errStack = Irpe.getStackTrace();
			resultDto.msg = Irpe.getMessage();
		} catch (IOException IOe) {
			// IOException
			resultDto.cnt = cnt;
			resultDto.okcnt = 0;
			resultDto.Ruleid = ruleid;
			resultDto.procEndDateTime = new java.util.Date();
			resultDto.errStack = IOe.getStackTrace();
			resultDto.msg = IOe.getMessage();
		} finally {
			this.factory.putCacheStatus(this.ruleid,resultDto);
		}
		

	}
}
