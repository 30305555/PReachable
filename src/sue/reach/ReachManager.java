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
 * Reachability�Ɏg�p����IP���𐶐����A�L���b�V���ɕێ�����N���X�ł��B
 * @Singleton
 * @author Takeshi
 *
 */
public class ReachManager {

	/**
	 * �L���b�V�����ꂽ�X�e�[�^�X�t�@�C��
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
	 *  Ping���ʃL���[
	 */
	private Queue<AsyncPing> asyncQueue = new ConcurrentLinkedQueue<AsyncPing>();

	/**
	 * IP���X�g�������擾���܂�
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
	 * @param gc �Z�b�g���� gc
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
	 * @param gcInterval �Z�b�g���� gcInterval
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
	 * ���߂̌��ʂ��擾����
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
	 * �w�肵�����ʂ��擾����
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
		System.out.println("�i�����L���b�V���̎擾�Ɏ��s");
		return this.cacheStatus.get(key).get(cycle);
	}
	
	/**
	 * �L���b�V����put����.
	 * @param cacheStatus �Z�b�g���� cacheStatus
	 */
	public void putCacheStatus(Integer key,ResultDTO value) {
		synchronized(this){
			// ���Y�̃��U���g���X�g���擾
		List<ResultDTO> list = this.cacheStatus.get(key);
		boolean nullflg = false;
		// �v�f�Ȃ�
		if ( list == null){
			nullflg = true;
			// ArrayList �𐶐�
			list = new ArrayList<ResultDTO>();
		}
		
		// ���X�g�T�C�Y-1����
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
				// Status�̉i�����I
				oos.writeObject(this.cacheStatus);
				System.out.println("Cache list:"+nullflg+", key:"+key+", size:"+this.cacheStatus.size()+", list size:"+list.size());
			} catch ( Exception ex){
				System.out.println("�i�����L���b�V���̏o�͂Ɏ��s");
				ex.printStackTrace();
				// �ăI�[�v�����s��
				reOpen();
			}
		}
	}
	
	/**
	 * �L���b�V�����C�^�̍Đ���
	 */
	public void reOpen(){
		// ��������stream�����
		try {
			this.oos.close();
		} catch (IOException e) {
		}
		try {
			// �ēx�V����Stream�𐶐�
			this.oos = new ObjectOutputStream(new FileOutputStream(CACHESTATUS));
		} catch (FileNotFoundException e) {
			try {
				// �t�@�C�������݂��Ȃ��ꍇ�A�V�K�쐬
				new File(CACHESTATUS).createNewFile();
			} catch (Exception ex2) {
				ex2.printStackTrace();
				// �쐬�G���[�ُ͈�I���Ƃ���
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

		// �o�b�N�A�b�v�p
		List<IPListDTO> cacheIPlistbkup = this.cacheIPlist;

		// �G���[�t���O
		boolean errflg = false;
		synchronized (this.cacheIPlist) {
			
			// ���݂̃L���b�V�����X�g���N���A
			this.cacheIPlist.clear();
			
			try {
				// FileReader
				freader = new FileReader(FILENAME);
				// BufferedReader
				breader = new BufferedReader(freader); 
				// ipList��1�s�ǂݍ���
				while((line = breader.readLine()) != null){
					
					// csv����؂�
					split = line.split(",");
					iplistdto = new IPListDTO(this.os);
					// csv�̌����s��
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
					// �L���b�V���ɒǉ�
					this.cacheIPlist.add(iplistdto);
					
				}
				// ���[����1�������݂��Ȃ�
				if ( ruleid == 0 ){
					errflg = true;
					throw new NullPointerException(FILENAME+"��0�s�ł��B�Ď��Ώۂ�ݒ肵�Ă��������B");
				}

				System.out.println("IPList.txt �ǂݍ��݊���:����["+ruleid+"]");
	
			} catch (IOException e) {
				errflg = true;
				e.printStackTrace(new PrintStream(System.err));
			} finally {
				try {
					breader.close();
					freader.close();
				} catch ( Exception ex){}
				// �G���[����
				if ( errflg == true ){
					// �o�b�N�A�b�v���𕜌�!
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
		
		// IPList�̓ǂݍ���
		loadIplist();
		
		// cacheStatus���i�����G���A����̕���
		restoreCacheStatus();

		try{
			// �L���b�V���X�e�[�^�X�̃X�g���[����ޔ�
			oos = new ObjectOutputStream(new FileOutputStream(CACHESTATUS));
			ois = new ObjectInputStream(new FileInputStream(CACHESTATUS));
			
		}catch(IOException ioex){
			System.out.println("cachestatus�o�͏��̐����Ɏ��s���܂����B");
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
	 * �L���b�V������IP�����擾���܂��B
	 * @param index
	 * @return
	 */
	public IPListDTO getIPList(int index){
		if ( cacheIPlist.size() <= index) {
			// �Ȃ��ꍇ�Anull
			return null;
		}
		return cacheIPlist.get(index);
	}
	
	/**
	 * �L���b�V�����N���A���܂�
	 */
	public void clearCache(){
		this.cacheIPlist.clear();
		this.cacheStatus.clear();
		this.asyncQueue.clear();
	}

	/**
	 * �L���b�V���𕜌�
	 */
	@SuppressWarnings("unchecked")
	public void restoreCacheStatus(){

		synchronized(this.cacheStatus){
			try{
				if ( ois == null){
					ois = new ObjectInputStream(new FileInputStream(CACHESTATUS));
				}
				// �ǂݍ��񂾓��e��Map�֐ݒ�
				Map<?, ?> map = (HashMap<?, ?>)ois.readObject();

				// �擾���������L���b�V���X�e�[�^�X�֐ݒ�
				this.cacheStatus = (Map<Integer, List<ResultDTO>>) map;
				
			} catch ( Exception ex){
				System.out.println("new cache!:"+ex);
				// �L���b�V���X�e�[�^�X���N���A
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
	 * ���̓L���[�ɃG���L���[
	 * @param asyncPing
	 */
	public void enqueue(AsyncPing asyncPing) {
		this.asyncQueue .offer(asyncPing);
	}
	
	/**
	 * ���̓L���[����f�L���[
	 * @return
	 */
	public AsyncPing dequeue() {
		return this.asyncQueue.poll();
	}
}
