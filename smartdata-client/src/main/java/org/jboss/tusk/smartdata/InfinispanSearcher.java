package org.jboss.tusk.smartdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

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
import org.jboss.tusk.smartdata.ejb.RemoteIngester;
import org.jboss.tusk.smartdata.ejb.RemoteSearcher;

public class InfinispanSearcher {
	
	private static RemoteSearcher searcher = null;
	private static Properties config = new Properties();

	public static int init(String propsFile) {
		System.out.println("In init");

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

		String searcherServer = config.getProperty("searcher.server", "localhost:4447");

		//get the remote ingester server hostname and port
		if (StringUtils.isEmpty(searcherServer)) {
			System.err.println("Could not create any initial context. Check the 'searcher.server' " +
					"property and make sure that the corresponding server is running.");
		}
		
		searcher = getSearcher(searcherServer);

		return 1;
	}

	private static RemoteSearcher getSearcher(String searcherServer) {
		String server = searcherServer.trim();
		String host = server.substring(0, server.indexOf(":"));
		String port = server.substring(server.indexOf(":") + 1);
		
		Properties props = new Properties();
		props.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
		props.put("remote.connections", "default");
		props.put("remote.connection.default.port", port);
		props.put("remote.connection.default.host", host);
		props.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
		props.put("remote.connection.default.username", InfinispanWriterEJB.USERNAME);
		props.put("remote.connection.default.password", InfinispanWriterEJB.PASSWORD);
		props.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
		EJBClientConfiguration clientConfig = new PropertiesBasedEJBClientConfiguration(props);
		ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(clientConfig);
		final ContextSelector<EJBClientContext> previousSelector = EJBClientContext.setSelector(selector);
		
		final Hashtable jndiProperties = new Hashtable();
		jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
		jndiProperties.put("jboss.naming.client.ejb.context", true);
		
		try {
			InitialContext context = new InitialContext(jndiProperties);
			RemoteSearcher searcher = (RemoteSearcher) context
					.lookup("ejb:smartdata-ear/smartdata-ejb//SearcherEJB!"
							+ RemoteSearcher.class.getName());
			return searcher;
		} catch (NamingException ex) {
			System.err.println("Caught NamingException getting initial context " +
					"for remote searcher server " + server + "; parsed as '" + 
					host + "':'" + port + "'.");
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		return null;
	}

	public static String search(String criteria) {
		if (searcher == null) {
			init(null);
		}

		return searcher.localSearch(criteria);
	}

	public static String distributedSearch(String criteria) {
		if (searcher == null) {
			init(null);
		}

		return searcher.search(criteria);
	}

	public static String mapReduce(String criteria) {
		if (searcher == null) {
			init(null);
		}

		return searcher.mapReduce(criteria);
	}

	public static void main(String[] args) {
		System.out.println("In main");
//		System.out.println("Args are:");
//		for (int i = 0; i < args.length; i++) {
//			System.out.println("  " + i + "=" + args[i]);
//		}
		
		init(args.length > 0 ? args[0] : null);

		String type = args.length > 1 ? args[1] : RemoteSearcher.SEARCH;
		String criteria = args.length > 2 ? args[2] : "origIP=192.168.0.0";
		
		System.out.println("Searching with type '" + type + "' and criteria '" + criteria + "'.");

		// write messages
		long start = System.currentTimeMillis();

		String result = RemoteSearcher.SEARCH.equalsIgnoreCase(type) ?
				search(criteria) :
					RemoteSearcher.MAP_REDUCE.equalsIgnoreCase(type) ?
							mapReduce(criteria) :
								distributedSearch(criteria);
		
		long total = System.currentTimeMillis() - start;

//		System.out.println(type + " took " + total + " ms. Result was " + result);
		System.out.println(type + " took " + total + " ms. Result had " + StringUtils.countMatches(result, "transIp") + " matches.");
		
		System.out.println("Finished with main");
	}
}
