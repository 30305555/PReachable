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
 * PING�񓯊����s�����̓N���X
 * @author Takeshi
 *
 */
public class AsyncPing extends Thread {

	
	private static String ANA_KEYWORD="�p�P�b�g��:";
	private static String RECEIVE_KEYWORD="��M = ";
	private static String ERR_KEYWORD="���� = ";

	// �����^�C���̎擾
	private Runtime myruntime = Runtime.getRuntime();
	
	// �v���Z�X���X�g
	private List<Process> procs = new LinkedList<Process>();

	/**
	 * �v���Z�X���X�g���擾���܂�.
	 * @return procs
	 */
	public List<Process> getProcs() {
		return procs;
	}
	// ResultDto����
	private ResultDTO resultDto = new ResultDTO();

	/**
	 * @return resultDto
	 */
	public ResultDTO getResultDto() {
		return resultDto;
	}
	private IPListDTO listdto = null;
	
	// �}�l�[�W��
	private ReachManager factory = null;

	// ���݂̃��[��ID
	private int ruleid = 0;

	/**
	 *  �R���X�g���N�^
	 * @param listdto IP���X�gDTO
	 * @param manager ReachManager
	 * @param index ���Y����index
	 * @param os os���
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
			ANA_KEYWORD="�p�P�b�g��:";
			RECEIVE_KEYWORD="��M = ";
			ERR_KEYWORD="���� = ";
			break;
		}
	}
	/**
	 *  �v���Z�X�̓ǂݍ���
	 *  PingAnalizer�ɈϏ��������߁A���p�񐄏�
	 * @return ����
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Deprecated
	public int readProc() throws InterruptedException, IOException{

		int okcnt = 0;
		for (Iterator<Process> ite = procs.iterator(); ite.hasNext();) {
			// ���̃v���Z�X���擾
			Process proc = (Process)ite.next();
			// �X���b�h�҂�
			proc.waitFor();
			int exitvalue = proc.exitValue();
			
//				System.out.println(this.factory.getIPList(this.ruleid).getIpv4addr()+" Thread Exit Value:"+exitvalue);

			// �v���Z�X�̃X�g���[��������s���ʂ��擾
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
				// ���W����
				okcnt += clctAnalyse(str)[0];
				builder.append(str+"\n");
			}
			reader.close();
			if ( exitvalue != 0){
				// �R�}���h���s�G���[
//					System.err.println(builder.toString());
				throw new InterruptedException(builder.toString());
			}
		}
		
		return okcnt;
	}
	/**
	 *  ����
	 * @param str
	 * @return
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
	
	/**
	 * �񓯊�Main����
	 */
	public void run(){

		// �񓯊��J�n�}�[�N�̏o��
		System.out.println(".:" + this.getId());


		// �����J�E���^
		int cnt=0;

		try{
		
			// ���s�񐔂̎擾
			cnt = Integer.parseInt(this.factory.getIPList(this.ruleid).getCount());

			// ���[��ID��ݒ�
			resultDto.Ruleid = ruleid;
			// �J�n���̎擾
			resultDto.procStartDateTime = new java.util.Date();
			// �{�s�񐔂�ݒ�
			resultDto.cnt = cnt;
			
			// ���s�񐔕��J��Ԃ�
			for ( int i = 0; i< cnt ;i++){

				// ping ���s�i��ping�v���Z�X�j
				Process proc = myruntime.exec(listdto.getIpv4addr());

				// process ��ǉ�
				procs.add(proc);
				// ���s�Ԋusleep
				Thread.sleep(Long.parseLong(this.listdto.getInterval()));
			}

			// ���͈˗��L���[�ɋl�ߍ���
			factory.enqueue(this);
			
		} catch (NumberFormatException numex ){
			// ��`��O
			resultDto.cnt = cnt;
			resultDto.okcnt = 0;
			resultDto.Ruleid = ruleid;
			resultDto.procEndDateTime = new java.util.Date();
			resultDto.errStack = numex.getStackTrace();
			resultDto.msg = numex.getMessage();
		} catch (InterruptedException Irpe) {
			// Ping��O
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
