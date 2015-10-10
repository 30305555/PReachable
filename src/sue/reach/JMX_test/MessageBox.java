package sue.reach.JMX_test;

import java.util.ArrayList;
import java.util.List;

import sue.reach.IPListDTO;
import sue.reach.ReachManager;

/**
 * JMX Interface �����N���X
 * @author Takeshi
 *
 */
public class MessageBox implements MessageBoxMBean {

	private String message = null;
	private ReachManager factory = null;
	
	public MessageBox(ReachManager factory) {
		this.factory = factory;
		this.message = "����ɂ���!! �R���X�g���N�^���쐬�������b�Z�[�W�ł��B";
	}

	/**
	 * IP���X�g�ǂݍ���
	 */
	public void loadIPlist(){
		this.factory.loadIplist();
	}
	/**
	 * ���U���g�X�e�[�^�X�̃N���A
	 */
	public void clearResultSts(){
		this.factory.clearCache();
	}
	/**
	 * ���ׂĂ�IP���X�gDTO���擾���܂�
	 */
	public List<IPListDTO> getAllIPListdto(){
		List<IPListDTO> list = new ArrayList<IPListDTO>();
		int index = 0;
		while(true){
			try{
				list.add(this.factory.getIPList(index));
				index++;
			}catch(Exception ex){
				// �v�f�Ȃ�
				break;
			}
		}
		return list;
	}
	/**
	 * �w�肳�ꂽ���[��ID��IP���X�g���擾���܂�
	 */
	public IPListDTO getIPListdto(int index){
		return this.factory.getIPList(index);
	}
	/**
	 * �R���X�g���N�^
	 * @param msg
	 */
	public MessageBox(String msg) {
		this.message = msg;
	}
	/**
	 * �R���X�g���N�^
	 */
	public void setMessage(String msg) {
		this.message = msg;
	}
	/**
	 * ���b�Z�[�W�̎擾
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * ���b�Z�[�W�o��
	 */
	public String printMessage() {
		return message+ new java.util.Date();
	}
	/**
	 * �J�E���g�̎擾
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
	 * GC�C���^�[�o���̐ݒ�
	 */
	public void setGcInterval(long interval){
		this.factory.setGcInterval(interval);
	}

	/**
	 * GC�C���^�[�o���̎擾
	 */
	public long getGcInterval(){
		return this.factory.getGcInterval();
	}

}