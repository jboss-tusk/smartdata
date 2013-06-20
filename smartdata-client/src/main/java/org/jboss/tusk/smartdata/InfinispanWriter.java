package org.jboss.tusk.smartdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.tusk.smartdata.IngesterThreadJMS.MessageType;
import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;
import org.jboss.tusk.smartdata.ejb.RemoteIngester;

public class InfinispanWriter {
	
	private static int BUFFER_SIZE = -1;
	private static int BATCH_SIZE = -1;
	private static int NUM_INGESTERS = -1;
	private static String[] messagesStrBuffer = null;
	private static int bufferCt = 0;
	private static Properties config = new Properties();
	
	//security credentials
	private static final String USERNAME = "jboss";
	private static final String PASSWORD = "password";
	
	//JMS fields
    private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
    private static final String INGESTER_DESTINATION = "jms/queue/IngesterQueue";
//    private static final String DEFAULT_USERNAME = "quickstartUser";
//    private static final String DEFAULT_PASSWORD = "quickstartPassword";
    private static final String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
//    private static final String PROVIDER_URL = "remote://localhost:4447";
    private static List<Connection> jmsConnections = null;
	private static List<Session> jmsSessions = null;
	private static List<MessageProducer> jmsMessageProducers = null;
	
	private static MessageType messageType = MessageType.TEXT;

	private static int totalCt = 0;
	
	public static int init(String propsFile) {
		System.out.println("In init with propsFile=" + propsFile);

		//get all system properties in one place
		config.putAll(System.getProperties());

		propsFile = !StringUtils.isEmpty(propsFile) ? 
				propsFile : 
					"smartdata-client.properties";
		System.out.println("Using properties file named " + propsFile + ".");
		
		try {
			config.load(ClassLoader.getSystemClassLoader().getResourceAsStream(propsFile));
		} catch (Exception ex) {
			System.err.println("Could not load the " + propsFile + " file.");
		}

		BUFFER_SIZE = Integer.valueOf(config.getProperty("buffer.size", "3000"));
		BATCH_SIZE = Integer.valueOf(config.getProperty("batch.size", "150"));
		NUM_INGESTERS = BUFFER_SIZE / BATCH_SIZE;
		
		jmsConnections = new ArrayList<Connection>();
		jmsSessions = new ArrayList<Session>(NUM_INGESTERS);
		jmsMessageProducers = new ArrayList<MessageProducer>(NUM_INGESTERS);
		messagesStrBuffer = new String[BUFFER_SIZE];

		System.out.println("BUFFER_SIZE=" + BUFFER_SIZE);
		System.out.println("BATCH_SIZE=" + BATCH_SIZE);
		System.out.println("NUM_INGESTERS=" + NUM_INGESTERS);
		
		String ingesterServers = config.getProperty("ingester.server", "localhost:4447");
		System.out.println("ingesterServers=" + ingesterServers);

		//get the JMS sessions from the ingester server(s)
		try {
			jmsInit(ingesterServers);
		} catch (JMSException ex) {
			System.err.println("Caught JMSException initializing JMS: " + ex.getMessage());
			ex.printStackTrace();
		} catch (NamingException ex) {
			System.err.println("Caught NamingException initializing JMS: " + ex.getMessage());
			ex.printStackTrace();
		}

		return 1;
	}

	private static void jmsInit(String ingesterServers) throws JMSException, NamingException {
		String[] ingesterServerArr = ingesterServers.split(",");
		
		//make list of connections; one connection per provider
		List<Context> contexts = new ArrayList<Context>();
		for (int i = 0; i < ingesterServerArr.length; i++) {
			String providerURL = "remote://" + ingesterServerArr[i];
			System.out.println("Creating JMS connection " + i + " for provder at " + providerURL);
			
	        final Properties env = new Properties();
	        env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
	        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, providerURL));
	        env.put(Context.SECURITY_PRINCIPAL, System.getProperty("username", USERNAME));
	        env.put(Context.SECURITY_CREDENTIALS, System.getProperty("password", PASSWORD));
	        Context context = new InitialContext(env);

	        String connectionFactoryString = System.getProperty("connection.factory", DEFAULT_CONNECTION_FACTORY);
//	        System.out.println("  Attempting to acquire connection factory \"" + connectionFactoryString + "\"");
	        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(connectionFactoryString);
//	        System.out.println("  Found connection factory \"" + connectionFactoryString + "\" in JNDI");

	        // Create the JMS connection and return
	        contexts.add(context);
			jmsConnections.add(connectionFactory.createConnection(USERNAME, PASSWORD));
		}
		
		//create jms sessions and message producers by round robinning through JMS_CONNECTION list
		for (int i = 0; i < NUM_INGESTERS; i++) {
			int connIdx = i % jmsConnections.size();
			System.out.println("Ingester " + i + " is using JMS connection " + connIdx);
			
			Connection currConn = jmsConnections.get(connIdx);
            Session session = currConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
//            System.out.println("  Created session: " + session);

			Destination destination = (Destination) contexts.get(connIdx).lookup(INGESTER_DESTINATION);
//			System.out.println("  Found destination \"" + destination + "\" in JNDI");
			MessageProducer messageProducer = session.createProducer(destination);
//            System.out.println("  Created messageProducer: " + messageProducer);
            
            jmsSessions.add(session);
            jmsMessageProducers.add(messageProducer);
		}
	}

//	private static void doJMSInitForOneProvider(String ingesterServer) throws JMSException, NamingException {
//		//since this is assuming a single host in the provider url, prepend "remote://" and use that as the provider
//		String namingProviderURL = "remote://" + ingesterServer;
//		
//        // Set up the context for the JNDI lookup
//        final Properties env = new Properties();
//        env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
//        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, namingProviderURL));
//        env.put(Context.SECURITY_PRINCIPAL, System.getProperty("username", USERNAME));
//        env.put(Context.SECURITY_CREDENTIALS, System.getProperty("password", PASSWORD));
//        Context context = new InitialContext(env);
//
//        String connectionFactoryString = System.getProperty("connection.factory", DEFAULT_CONNECTION_FACTORY);
//        System.out.println("Attempting to acquire connection factory \"" + connectionFactoryString + "\"");
//        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(connectionFactoryString);
//        System.out.println("Found connection factory \"" + connectionFactoryString + "\" in JNDI");
//
//        // Create the JMS connection, session, producer, and consumer
//        JMS_CONNECTION = connectionFactory.createConnection(USERNAME, PASSWORD);
//
//		for (int i = 0; i < NUM_INGESTERS; i++) {
//            Session session = JMS_CONNECTION.createSession(false, Session.AUTO_ACKNOWLEDGE);
//            System.out.println("Created session: " + session);
//
//			Destination destination = (Destination) context.lookup(INGESTER_DESTINATION);
//			System.out.println("Found destination \"" + destination + "\" in JNDI");
//			MessageProducer messageProducer = session.createProducer(destination);
//            System.out.println("Created messageProducer: " + messageProducer);
//            
//            jmsSessions.add(session);
//            jmsMessageProducers.add(messageProducer);
//		}
//	}

	public static int write(String message) {
		if (jmsSessions.isEmpty() && jmsMessageProducers.isEmpty()) {
			init(null);
		}

		if (bufferCt < BUFFER_SIZE) {
			// add to buffer
			messagesStrBuffer[bufferCt] = message;
			bufferCt++;
//			System.out.println("*Added message to buffer; bufferCt is now " + bufferCt);
		}

		// now flush if we've filled it up
		if (bufferCt == BUFFER_SIZE) {
			flushBuffer();
			messagesStrBuffer = new String[BUFFER_SIZE];
		}

		// System.out.println("Added '" + message + "' to data grid with key " +
		// uuid + ". Response was: " + response);

		totalCt++;
		
//		if (totalCt % 100000 == 0) {
//			System.out.println("  Wrote " + totalCt
//					+ " messages. Last one was '" + message + "'");
//		}
		
		return 1;
	}

	private static void flushBuffer() {
		System.out.println("Flushing buffer, which has " + bufferCt + " items.");

		long start = System.currentTimeMillis();

		List<StringBuffer> batches = new ArrayList<StringBuffer>(NUM_INGESTERS);
		for (int i = 0; i < NUM_INGESTERS; i++) {
			batches.add(new StringBuffer());
		}
		
		int[] cts = new int[NUM_INGESTERS];
		for (int i = 0; i < NUM_INGESTERS; i++) {
			cts[i] = 0;
		}

		CachedItemHelper helper = CachedItemHelperFactory.getInstance();
		
		for (int i = 0; i < bufferCt; i++) {
			if (messagesStrBuffer[i] != null) {
				int whichBatch = i / BATCH_SIZE;
				StringBuffer currBatch = batches.get(whichBatch);
				cts[whichBatch]++;
				currBatch.append(messagesStrBuffer[i]);
				//if our batches will contain more than one item, add a pipe to separate the items.
				//this way, if the incoming message itself had already batched up many items, we can
				//just pass that batch along by using a batch size of 1 (in which case we wouldn't want
				//to add a pipe at the end of the item
				if (BATCH_SIZE > 1) {
					currBatch.append(helper.getBatchSeparator());
				}
			}
		}

		List<IngesterThreadJMS> threads = new ArrayList<IngesterThreadJMS>(NUM_INGESTERS);
		for (int i = 0; i < NUM_INGESTERS; i++) {
			threads.add(new IngesterThreadJMS(jmsSessions.get(i), jmsMessageProducers.get(i), batches.get(i), messageType));
		}
		
//		System.out.println("About to start flush threads");
		for (int i = 0; i < NUM_INGESTERS; i++) {
			if (cts[i] > 0) { 
				threads.get(i).start(); 
			}
		}

		for (int i = 0; i < NUM_INGESTERS; i++) {
			if (cts[i] > 0) { 
				try {
					threads.get(i).join();
				} catch (InterruptedException ex) {
					System.err.println("Caught InterruptedException joining thread " + i + ": " + ex.getMessage());
				} 
			}
		}
		
		//System.out.println("Flush threads finished");

		long total = System.currentTimeMillis() - start;
		System.out.println("Flushing buffer of " + bufferCt
				+ " messages took " + total + " ms.");

		// System.out.println("Added '" + message + "' to data grid with key " +
		// uuid + ". Response was: " + response);
		
		//sleep to throttle dispatching of messages
//		System.out.println("About to sleep for 5 secs.");
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException ex) {
//			System.err.println("Caught InterruptedException while sleeping: " + ex.getMessage());
//		}
//		System.out.println("Woke up after our nap.");

		bufferCt = 0;
	}

	public static int cleanup() {
		System.out.println("Cleaning up; bufferCt is " + bufferCt);
		if (bufferCt > 0) {
			// if we don't have a full buffer, make a smaller one and send it
			if (bufferCt < BUFFER_SIZE) {
				String[] messagesStrBuffer2 = new String[bufferCt];
				for (int i = 0; i < bufferCt; i++) {
					messagesStrBuffer2[i] = messagesStrBuffer[i];
				}
				messagesStrBuffer = messagesStrBuffer2;
			}
			flushBuffer();
		}
		
		//clean up JMS objects
		for (Session session : jmsSessions) {
			try {
				session.close();
			} catch (JMSException ex) {
				System.err.println("Caught JMSException closing JMS session: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		for (Connection conn : jmsConnections) {
			try {
				conn.close();
			} catch (JMSException ex) {
				System.err.println("Caught JMSException closing JMS connection : " + ex.getMessage());
				ex.printStackTrace();
			}
		}

		return 1;
	}

	public static void main(String[] args) {
		System.out.println("In main");
		init(args.length > 2 ? args[2] : null);

		// see how many messages to write
		int num = 1;
		if (args.length > 0) {
			try {
				num = Integer.valueOf(args[0]);
			} catch (NumberFormatException ex) {
				System.out.println("Couldn't convert '" + args[0]
						+ "' to an integer; Using default of " + num);
			}
			if (args.length > 1) {
			   try {
			      messageType = MessageType.valueOf(args[1]);
			   } catch (Exception e) {
			      System.out.println("Couldn't convert '" + args[1]
	                  + "' to MessageType; Using default of " + messageType);
			   }
			}
		}
		System.out.println("Writing " + num + " message(s) in " + messageType);

		CachedItemHelper helper = CachedItemHelperFactory.getInstance();
		
		// prepare messages to write
		String[] messages = new String[num];
		for (int i = 0; i < num; i++) {
			messages[i] = helper.sample().toString();
//			System.out.println(i + ". Message=" + messages[i]);
		}

		// write messages
		long start = System.currentTimeMillis();
		for (int i = 0; i < num; i++) {
			write(messages[i]);
		}
		long total = System.currentTimeMillis() - start;

		cleanup();

		System.out.println(num + " writes took " + total + " ms. Messages per sec: " + (1.0 * num / total));
		
		System.out.println("Finished with main");
	}
}

class IngesterThreadJMS extends Thread {
   enum MessageType {
      TEXT, BYTES
   }
   
	private Session jmsSession;
	private MessageProducer jmsMessageProducer;
	private StringBuffer batch;
	private MessageType type;

	public IngesterThreadJMS(Session jmsSession, MessageProducer jmsMessageProducer, StringBuffer batch, MessageType type) {
		this.jmsSession = jmsSession;
		this.jmsMessageProducer = jmsMessageProducer;
		this.batch = batch;
		this.type = type;
	}

	@Override
	public void run() {
		long start = System.currentTimeMillis();

		System.out.println("Sending message: " + this.batch.toString());
		try {
		   switch (type) {
		   case TEXT: {
		      TextMessage message = jmsSession.createTextMessage();
	         message.setText(this.batch.toString());
	         jmsMessageProducer.send(message);
		      break;
		   }
		   case BYTES: {
		      BytesMessage message = jmsSession.createBytesMessage();
	         message.writeBytes(this.batch.toString().getBytes());
	         jmsMessageProducer.send(message);
		      break;
		   }
		   default:
		      throw new IllegalArgumentException("unknown message type");
		   }
		} catch (JMSException ex) {
			System.err.println("Caught JMSException sending ingester message: " + ex.getMessage());
			ex.printStackTrace();
		}

		long total = System.currentTimeMillis() - start;
		System.out.println("  " + System.currentTimeMillis() + ": Call to ingest batch took " + total + " ms.");
	}

}
