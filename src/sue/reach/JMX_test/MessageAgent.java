package sue.reach.JMX_test;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import sue.reach.ReachManager;

import com.sun.jdmk.comm.HtmlAdaptorServer;

public class MessageAgent {

	private static final String MBEAN_OBJECT_NAME = "MessageAgent:name=MessageBox";
	private static final String MBEAN_MESSAGE_MESSAGE_AGENT = "MessageAgent";
	private static final int PORTNO = 8082;
    private static final String MBEAN_OBJECT_NAME_ADAPTER = "MessageAgent:name=HTMLAdaptor, port="+PORTNO;
	public static final String ADDRESS = "service:jmx:rmi:///jndi/rmi://localhost/hello";


	/**
	 * JMX Interface Agent
	 * MBean��o�^
	 * @param reachFactory
	 */
	public MessageAgent(ReachManager reachFactory) {

		// MBeanServer�̎擾
		MBeanServer mbeanServer = MBeanServerFactory
				.createMBeanServer(MBEAN_MESSAGE_MESSAGE_AGENT);
		HtmlAdaptorServer htmlAdapterServer = new HtmlAdaptorServer();
		MessageBox messagebox = new MessageBox(reachFactory);

		try {

			ObjectName msgboxName = new ObjectName(
					MBEAN_OBJECT_NAME);
			mbeanServer.registerMBean(messagebox, msgboxName);

			// �ڑ����
			ObjectName objName = new ObjectName(
					MBEAN_OBJECT_NAME_ADAPTER);
			// JMXPort
			htmlAdapterServer.setPort(PORTNO);

			// MBean�ɓo�^
			mbeanServer.registerMBean(htmlAdapterServer, objName);
			// JMX��t�J�n
			htmlAdapterServer.start();

			System.out.println("MBean Registed. info start --------------------------------------");
			System.out.println(MBEAN_MESSAGE_MESSAGE_AGENT);
			System.out.println(MBEAN_OBJECT_NAME);
			System.out.println(PORTNO);
			System.out.println(MBEAN_OBJECT_NAME_ADAPTER);
			System.out.println("MBean Registed. info end  --------------------------------------");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * MBeanAgent�N��
	 * @param args
	 */
	public static void main(String args[]) {
		System.out.println("MessageAgent ���s��");
		new MessageAgent(ReachManager.getInstance(Integer.parseInt(args[0])));
	}

}