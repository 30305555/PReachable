package sue.reach;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import sue.reach.async.AsyncPing;

/**
 * Reachabilityに使用するIP情報を生成し、キャッシュに保持するクラスです。
 * @Singleton
 * @author Takeshi
 *
 */
public class ReachManager {

	/**
	 * キャッシュされたステータスファイル
	 */
	private static final String CACHESTATUS = "cachestatus";

	private static String FILENAME = "iplist.txt";

	/**
	 * IPList cache!
	 */
	private List<IPListDTO> cacheIPlist = new ArrayList<IPListDTO>();

	/**
	 * Status cache
	 */
	private Map<Integer,List<ResultDTO>> cacheStatus = new HashMap<Integer,List<ResultDTO>>();

	/**
	 * Operating System
	 * 0:Unix 1:Win7
	 */
	private int os = 0;
	
	/**
	 * GCInterval
	 */
	private long gcInterval = 10000;
	
	private boolean gc = false;
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;

	/**
	 *  Ping結果キュー
	 */
	private Queue<AsyncPing> asyncQueue = new ConcurrentLinkedQueue<AsyncPing>();

	/**
	 * IPリスト件数を取得します
	 * @return int
	 */
	public int getIPlistcnt(){
		return this.cacheIPlist.size();
	}
	/**
	 * @return gc
	 */
	public boolean isGc() {
		return gc;
	}
	/**
	 * @param gc セットする gc
	 */
	public void setGc(boolean gc) {
		this.gc = gc;
	}
	/**
	 * @return gcInterval
	 */
	public long getGcInterval() {
		return gcInterval;
	}
	/**
	 * @param gcInterval セットする gcInterval
	 */
	public void setGcInterval(long gcInterval) {
		this.gcInterval = gcInterval;
	}
	/**
	 * @return IPListDTO
	 */
	public boolean isCacheStatus(Integer key) {
		return cacheStatus.containsKey(key);
	}
	/**
	 * 直近の結果を取得する
	 * @return IPListDTO
	 */
	public ResultDTO getCacheStatus(Integer key) {
		if (!isCacheStatus(key)){
			return null;
		}
		ResultDTO dto = this.cacheStatus.get(key).get(this.cacheStatus.get(key).size()-1);
		if ( dto == null ){
			dto = this.cacheStatus.get(key).get(0);
		}
		return dto;
	}
	/**
	 * 指定した結果を取得する
	 * @return IPListDTO
	 */
	public ResultDTO getCacheStatus(Integer key, int cycle) {
		if (!isCacheStatus(key)){
			return null;
		}
		ObjectInputStream ois = null;

		synchronized(this.cacheStatus){
			try{
				ois = new ObjectInputStream(new FileInputStream(CACHESTATUS));
				HashMap<?, ?> map = (HashMap<?, ?>)ois.readObject();

				return (ResultDTO)((HashMap<?, ?>)map.get(key)).get(cycle);
			} catch ( Exception ex){
				try {
					ois.close();
				} catch (IOException e) {
				} finally {
					ois = null;
				}
			}
		}
		System.out.println("永続化キャッシュの取得に失敗");
		return this.cacheStatus.get(key).get(cycle);
	}
	
	/**
	 * キャッシュにputする.
	 * @param cacheStatus セットする cacheStatus
	 */
	public void putCacheStatus(Integer key,ResultDTO value) {
		synchronized(this){
			// 当該のリザルトリストを取得
		List<ResultDTO> list = this.cacheStatus.get(key);
		boolean nullflg = false;
		// 要素なし
		if ( list == null){
			nullflg = true;
			// ArrayList を生成
			list = new ArrayList<ResultDTO>();
		}
		
		// リストサイズ-1生成
		ResultDTO dto[] = new ResultDTO[list.size()+1];
		for ( int i = 0; i< list.size();i++){
			dto[i] = list.get(i);
		}
			list.clear();
			int i = 0;
			for ( i = 0; i< dto.length;i++){
				list.add(i,dto[i]);
			}
			list.add(i,value);
			this.cacheStatus.put(key, list);

			try{
				// Statusの永続化！
				oos.writeObject(this.cacheStatus);
				System.out.println("Cache list:"+nullflg+", key:"+key+", size:"+this.cacheStatus.size()+", list size:"+list.size());
			} catch ( Exception ex){
				System.out.println("永続化キャッシュの出力に失敗");
				ex.printStackTrace();
				// 再オープンを行う
				reOpen();
			}
		}
	}
	
	/**
	 * キャッシュライタの再生成
	 */
	public void reOpen(){
		// いったんstreamを閉じる
		try {
			this.oos.close();
		} catch (IOException e) {
		}
		try {
			// 再度新たなStreamを生成
			this.oos = new ObjectOutputStream(new FileOutputStream(CACHESTATUS));
		} catch (FileNotFoundException e) {
			try {
				// ファイルが存在しない場合、新規作成
				new File(CACHESTATUS).createNewFile();
			} catch (Exception ex2) {
				ex2.printStackTrace();
				// 作成エラーは異常終了とする
				System.exit(-1);
			}
		} catch (IOException e) {
		}
	}
	/**
	 * ReachManager my  instance
	 */
	private static ReachManager me = null;

	/**
	 * load IPList
	 * 
	 * this format.
	 * ------------------
	 * ipaddress,interval,count
	 * ------------------
	 */
	public void loadIplist(){

		// IPlist Reader
		FileReader freader = null;
		BufferedReader breader = null;

		String line = null;
		String[] split;
		IPListDTO iplistdto = null;
		int ruleid=0;

		// バックアップ用
		List<IPListDTO> cacheIPlistbkup = this.cacheIPlist;

		// エラーフラグ
		boolean errflg = false;
		synchronized (this.cacheIPlist) {
			
			// 現在のキャッシュリストをクリア
			this.cacheIPlist.clear();
			
			try {
				// FileReader
				freader = new FileReader(FILENAME);
				// BufferedReader
				breader = new BufferedReader(freader); 
				// ipListの1行読み込み
				while((line = breader.readLine()) != null){
					
					// csv桁区切り
					split = line.split(",");
					iplistdto = new IPListDTO(this.os);
					// csvの桁数不足
					if ( split.length != 3) {
						continue;
					}
					iplistdto.setRuleid(ruleid);
					ruleid++;
					
					for( int i = 0; i < split.length; i++){
						switch(i){
							case 0:
								// IPAddr
								iplistdto.setIpv4addr(split[i]);
								break;
							case 1:
								// Interval
								iplistdto.setInterval(split[i]);
								break;
							case 2:
								// Count
								iplistdto.setCount(split[i]);
								break;
							default:
								throw new IOException("FileFormatError:["+FILENAME+"]");
						}
					}
					// キャッシュに追加
					this.cacheIPlist.add(iplistdto);
					
				}
				// ルールが1件も存在しない
				if ( ruleid == 0 ){
					errflg = true;
					throw new NullPointerException(FILENAME+"が0行です。監視対象を設定してください。");
				}

				System.out.println("IPList.txt 読み込み完了:件数["+ruleid+"]");
	
			} catch (IOException e) {
				errflg = true;
				e.printStackTrace(new PrintStream(System.err));
			} finally {
				try {
					breader.close();
					freader.close();
				} catch ( Exception ex){}
				// エラー発生
				if ( errflg == true ){
					// バックアップ情報を復元!
					this.cacheIPlist = cacheIPlistbkup;
				}

			}
		}
	}
	/**
	 * public {@link Constructor}
	 * @return 
	 */
	ReachManager(int os){
		this.os = os;
		
		// IPListの読み込み
		loadIplist();
		
		// cacheStatusを永続化エリアからの復元
		restoreCacheStatus();

		try{
			// キャッシュステータスのストリームを退避
			oos = new ObjectOutputStream(new FileOutputStream(CACHESTATUS));
			ois = new ObjectInputStream(new FileInputStream(CACHESTATUS));
			
		}catch(IOException ioex){
			System.out.println("cachestatus出力情報の生成に失敗しました。");
			ioex.printStackTrace();
			System.exit(-1);
		}
	};
	/**
	 * ReachFactory::getinstance()
	 * @return ReachFactory instance
	 */
	public synchronized static ReachManager getInstance(int os){
		if ( me == null){
			me = new ReachManager(os);
		}
		return me;
	}
	
	/**
	 * キャッシュからIP情報を取得します。
	 * @param index
	 * @return
	 */
	public IPListDTO getIPList(int index){
		if ( cacheIPlist.size() <= index) {
			// ない場合、null
			return null;
		}
		return cacheIPlist.get(index);
	}
	
	/**
	 * キャッシュをクリアします
	 */
	public void clearCache(){
		this.cacheIPlist.clear();
		this.cacheStatus.clear();
		this.asyncQueue.clear();
	}

	/**
	 * キャッシュを復元
	 */
	@SuppressWarnings("unchecked")
	public void restoreCacheStatus(){

		synchronized(this.cacheStatus){
			try{
				if ( ois == null){
					ois = new ObjectInputStream(new FileInputStream(CACHESTATUS));
				}
				// 読み込んだ内容をMapへ設定
				Map<?, ?> map = (HashMap<?, ?>)ois.readObject();

				// 取得した情報をキャッシュステータスへ設定
				this.cacheStatus = (Map<Integer, List<ResultDTO>>) map;
				
			} catch ( Exception ex){
				System.out.println("new cache!:"+ex);
				// キャッシュステータスをクリア
				this.cacheStatus = new HashMap<Integer, List<ResultDTO>>();
				try {
					ois.close();
				} catch (IOException e) {
				} finally {
					ois = null;
				}
			}
		}
	}

	/**
	 * 分析キューにエンキュー
	 * @param asyncPing
	 */
	public void enqueue(AsyncPing asyncPing) {
		this.asyncQueue .offer(asyncPing);
	}
	
	/**
	 * 分析キューからデキュー
	 * @return
	 */
	public AsyncPing dequeue() {
		return this.asyncQueue.poll();
	}
}
