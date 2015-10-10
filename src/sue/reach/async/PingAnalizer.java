package sue.reach.async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import sue.reach.ReachManager;
import sue.reach.ResultDTO;

/**
 * Ping���ʉ�͌N
 * @author Takeshi
 *
 */
public class PingAnalizer extends Thread {

	private static String ANA_KEYWORD="�p�P�b�g��:";
	private static String RECEIVE_KEYWORD="��M = ";
	private static String ERR_KEYWORD="���� = ";
	private static final int DEQUEUE_INTERVAL = 100;
	private ReachManager manager;

	/**
	 * �R���X�g���N�^
	 * @param manager
	 */
	public PingAnalizer(ReachManager manager) {
		this.manager = manager;
	}
	/**
	 * readProcess
	 */
	public void run() {
	
		// �����胋�[�v
		while (true) {
			
			// ���̓L���[��芠����
			AsyncPing ping = this.manager.dequeue();

			if ( ping == null ) {
				// �Ȃ�
				try {
					// �C���^�[�o����u���čĊ�������s��
					Thread.sleep(DEQUEUE_INTERVAL);
					continue;
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			
			// �v���Z�X���X�g�̎擾
			List<Process> processList = ping.getProcs();
		
			int okcnt = 0;
			
			// �v���Z�X���X�g������Ԃ�
			for ( Process proc : processList ) {
				
				// �v���Z�X�I���҂�(���󖳌��҂�) TODO:�����҂������P������
				try {
					proc.waitFor ();
				} catch (InterruptedException e) {
					// ���荞�ݗ�O
					e.printStackTrace();
					// ��O�Ή�
					errInterrupted(ping.getResultDto(), e);
					continue;
				}

				// �߂�l�擾
				int exitvalue = proc.exitValue();

				// �v���Z�X�̃X�g���[��������s���ʂ��擾
				BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));			
				StringBuilder builder = new StringBuilder();

				while(true){
					try {
						// �X���[�v TODO:�O�o������
						Thread.sleep(5);
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}

					// TODO:�ʃ��\�b�h�ɏ������ڊǂ���
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
					// ���W����
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
					// �R�}���h���s�G���[
					ResultDTO resultDto = ping.getResultDto();
					
					// Ping��O
					resultDto.okcnt = 0;
					resultDto.procEndDateTime = new java.util.Date();
					resultDto.errStack = null;
					resultDto.msg = builder.toString();

					continue;
				}

				ResultDTO resultDto = ping.getResultDto();
				// �����񐔂����
				resultDto.okcnt = okcnt;
				resultDto.procEndDateTime = new java.util.Date();

				//����
				resultDto.errStack = null;
				resultDto.msg = "OK.";
				
				// �}�l�[�W���L���b�V���֐ݒ�
				this.manager.putCacheStatus(resultDto.Ruleid,resultDto);

				
			}
		}
	}

	/**
	 * ���荞�ݗ�O�ԋp��񐶐�
	 * @param t Throwable
	 * @param cnt �{�s��
	 * @param ruleid
	 * @return
	 */
	private void errInterrupted(ResultDTO resultDto, Throwable t) {

		// Ping��O
		resultDto.okcnt = 0;
		resultDto.procEndDateTime = new java.util.Date();
		resultDto.errStack = t.getStackTrace();
		resultDto.msg = t.getMessage();

		// �}�l�[�W���L���b�V���ݒ�
		this.manager.putCacheStatus(resultDto.Ruleid,resultDto);
	}

	/**
	 *  ���͏���
	 * @param str
	 * @return int array
	 */
	private int[] clctAnalyse(String str) {

		int retval[] = {0,0};

		// �Y���s���`�F�b�N
		if ( str.indexOf(ANA_KEYWORD) == -1){
			// �X���[point
			return retval;
		} 
		try{
			// ��M�ʒu�擾
			retval[0] = getCount(str,str.indexOf(RECEIVE_KEYWORD),RECEIVE_KEYWORD.length());
			
			// �����ʒu�擾
			retval[1] = getCount(str,str.indexOf(ERR_KEYWORD),ERR_KEYWORD.length());
		} catch ( Exception e){
			System.err.println("PINGMSG["+str+"] RCVKWD["+RECEIVE_KEYWORD+"] ERRKWD["+ERR_KEYWORD+"]");
		}
		return retval;
	}

	/**
	 * ���ʕ����񂩂猏���𔲂��o��
	 * @param str
	 * @param ana_point
	 * @param kwdlength
	 * @return ����
	 */
	private int getCount(String str, int ana_point,int kwdlength){

		// �����o���J�nindex
		int stindex = ana_point+kwdlength;
		// �����o���I��index
		int edindex = ana_point + kwdlength + 3;
		try{
			// �����o��try1���
			return Integer.parseInt(str.substring(stindex,edindex));
		} catch (Exception ex){
			try{
				edindex--;
				// �����o��try2���
				return Integer.parseInt(str.substring(stindex,edindex));
			} catch( Exception ex2){
				try{
					edindex--;
					// �����o��try3���
					return Integer.parseInt(str.substring(stindex,edindex));
				} catch (Exception exc){
					throw new RuntimeException();
				}
			}
		}

	}
}
