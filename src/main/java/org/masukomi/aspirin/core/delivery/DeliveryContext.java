package org.masukomi.aspirin.core.delivery;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.masukomi.aspirin.core.store.queue.QueueInfo;

/**
 * This class is the context of a delivery which contains all required 
 * informations used or created in the delivery process.
 * 
 * @author Laszlo Solova
 *
 */
public class DeliveryContext {
	private QueueInfo queueInfo;
	public QueueInfo getQueueInfo() {
		return queueInfo;
	}
	public DeliveryContext setQueueInfo(QueueInfo queueInfo) {
		this.queueInfo = queueInfo;
		return this;
	}
	private MimeMessage message;
	public MimeMessage getMessage() {
		return message;
	}
	public DeliveryContext setMessage(MimeMessage message) {
		this.message = message;
		return this;
	}
	private Session mailSession;
	public Session getMailSession() {
		return mailSession;
	}
	public DeliveryContext setMailSession(Session mailSession) {
		this.mailSession = mailSession;
		return this;
	}
	
	private Map<String, Object> contextVariables = new HashMap<String, Object>();
	public Map<String, Object> getContextVariables() {
		return contextVariables;
	}
	public void addContextVariable(String name, Object variable) {
		contextVariables.put(name, variable);
	}
	@SuppressWarnings("unchecked")
	public <T> T getContextVariable(String name) {
		if( contextVariables.containsKey(name) )
			return (T)contextVariables.get(name);
		return null;
	}
	
	private transient String ctxToString;
	@Override
	public String toString() {
		if( ctxToString == null )
		{
			StringBuilder sb = new StringBuilder();
			sb.append(getClass().getSimpleName()).append(" [");
			sb.append("qi=").append(queueInfo);
			sb.append("]; ");
			ctxToString = sb.toString();
		}
		return ctxToString;
	}

}
