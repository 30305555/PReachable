package sue.reach.JMX_test;

import java.util.ArrayList;
import java.util.List;

import sue.reach.IPListDTO;
import sue.reach.ReachManager;

/**
 * JMX Interface 実装クラス
 * @author Takeshi
 *
 */
public class MessageBox implements MessageBoxMBean {

	private String message = null;
	private ReachManager factory = null;
	
	public MessageBox(ReachManager factory) {
		this.factory = factory;
		this.message = "こんにちは!! コンストラクタが作成したメッセージです。";
	}

	/**
	 * IPリスト読み込み
	 */
	public void loadIPlist(){
		this.factory.loadIplist();
	}
	/**
	 * リザルトステータスのクリア
	 */
	public void clearResultSts(){
		this.factory.clearCache();
	}
	/**
	 * すべてのIPリストDTOを取得します
	 */
	public List<IPListDTO> getAllIPListdto(){
		List<IPListDTO> list = new ArrayList<IPListDTO>();
		int index = 0;
		while(true){
			try{
				list.add(this.factory.getIPList(index));
				index++;
			}catch(Exception ex){
				// 要素なし
				break;
			}
		}
		return list;
	}
	/**
	 * 指定されたルールIDのIPリストを取得します
	 */
	public IPListDTO getIPListdto(int index){
		return this.factory.getIPList(index);
	}
	/**
	 * コンストラクタ
	 * @param msg
	 */
	public MessageBox(String msg) {
		this.message = msg;
	}
	/**
	 * コンストラクタ
	 */
	public void setMessage(String msg) {
		this.message = msg;
	}
	/**
	 * メッセージの取得
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * メッセージ出力
	 */
	public String printMessage() {
		return message+ new java.util.Date();
	}
	/**
	 * カウントの取得
	 */
	public int getCount(){
		int i=0;
		for(i = 0;;i++){
			try{
				this.factory.getCacheStatus(0, i);
				i++;
			} catch (Error ex){
				break;
			}
		}
		return i;
	}
	/**
	 * GCインターバルの設定
	 */
	public void setGcInterval(long interval){
		this.factory.setGcInterval(interval);
	}

	/**
	 * GCインターバルの取得
	 */
	public long getGcInterval(){
		return this.factory.getGcInterval();
	}

}