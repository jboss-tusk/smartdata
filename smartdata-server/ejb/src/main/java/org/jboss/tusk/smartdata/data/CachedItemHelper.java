package org.jboss.tusk.smartdata.data;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

public interface CachedItemHelper {
	
	/**
	 * This returns an array of CachedItem subclasses that is created from parsing a String.
	 * The String can be in any format (ie syslog vs json vs xml) and can be a batch of multiple 
	 * items (ie separated by newline or by the | char) or just a single item.  
	 * 
	 * Note that this is the method that gets called when the message to be handled comes from a 
	 * HornetQ queue.
	 * 
	 * It is very important that this method be as efficient as possible from both a memory and 
	 * processing time perspective.
	 * 
	 * @param val
	 * @return
	 */
	public CachedItem[] parseFromString(String val);
	
	/**
	 * This returns an array of CachedItem subclasses that is created from parsing a JMS BytesMessage.
	 * The contents of the bytes can be in any format (ie syslog vs json vs xml) and can be a batch of 
	 * multiple items (ie separated by newline or by the | char) or just a single item. 
	 * 
	 * Note that this is the method that gets called when the message to be handled comes from an 
	 * AMQP queue.
	 * 
	 * It is very important that this method be as efficient as possible from both a memory and 
	 * processing time perspective.
	 * 
	 * @param val
	 * @return
	 * @throws JMSException
	 */
	public CachedItem[] parseFromBytes(BytesMessage val) throws JMSException;
	
	/**
	 * Returns the concrete Class object to be used in searches. This should be the Class object for the
	 * subclass of CachedItem that this helper is responsible for.
	 * @return
	 */
	public Class getSearchClass();
	
	public CachedItem sample();
	
	public CachedItem fromString(String str);
	
	/**
	 * Return the character used to separate items in a batch.
	 * @return
	 */
	public char getBatchSeparator();

}
