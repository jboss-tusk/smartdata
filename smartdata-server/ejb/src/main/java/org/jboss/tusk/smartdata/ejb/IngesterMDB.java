package org.jboss.tusk.smartdata.ejb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;

@MessageDriven(name = "IngesterMDB", activationConfig = {
	    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
	    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
	    @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/exported/cv-jdg-q"), //this is for an amqp queue
	    //@ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/IngesterQueue"), //this is for a local hornetq queue
	    @ActivationConfigProperty(propertyName = "connectionURL", propertyValue = "amqp://guest:guest@cirries-2002/?brokerlist='tcp://cirries-2002:5672?sasl_mechs='PLAIN''"),
	    @ActivationConfigProperty(propertyName = "useLocalTx", propertyValue = "false"),
	    @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")})
public class IngesterMDB implements MessageListener {

	private final static Logger LOG = Logger.getLogger(IngesterMDB.class);

	private static int totalNumMessagesReceived = 0;
	
	/**
	 * @see MessageListener#onMessage(Message)
	 */
	public void onMessage(Message rcvMessage) {
		totalNumMessagesReceived++;
		LOG.info("Received message number " + totalNumMessagesReceived + " for ingest.");
		
		CachedItemHelper helper = CachedItemHelperFactory.getInstance();
		
		CachedItem[] cachedItems = null;
		
		//parse the item(s) out of the message
		try {
			if (rcvMessage instanceof TextMessage) {
				cachedItems = helper.parseFromString(((TextMessage) rcvMessage).getText());
			} else if (rcvMessage instanceof BytesMessage) {
				cachedItems = helper.parseFromBytes(((BytesMessage) rcvMessage));
			} else {
				LOG.warn("Message of wrong type: " + rcvMessage.getClass().getName());
			}
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}
		
		IngesterHelper ingesterHelper = new IngesterHelper();
		ingesterHelper.ingestBatch(cachedItems);
		
	}

	
}
