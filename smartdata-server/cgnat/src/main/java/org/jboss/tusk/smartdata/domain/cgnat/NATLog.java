package org.jboss.tusk.smartdata.domain.cgnat;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.ProvidedId;
import org.jboss.tusk.smartdata.data.CachedItem;

/**
 * Container for NAT log messages to be stored in the data grid.
 * @author justin
 *
 */
@Indexed @ProvidedId
public class NATLog extends CachedItem implements Serializable {

	@Field protected String key;
	@Field protected final String time;
	@Field protected final String type;
	@Field protected final String app;
	@Field protected final String orig;
	@Field protected final String origIP;
	@Field protected final String origPort;
	@Field protected final String destIP;
	@Field protected final String destPort;
	@Field protected final String transIP;
	@Field protected final String transPort;

	private static final long serialVersionUID = 7359651167731565025L;
	
	public NATLog() {
		this(null, null, null, null, null, null, null, null, null, null);
	}
	
	public NATLog(String time, String type, String app, String orig, 
			String origIP, String origPort, String destIP, String destPort,
			String transIP, String transPort) {
		this.time = time;
		this.type = type;
		this.app = app;
		this.orig = orig;
		this.origIP = origIP;
		this.origPort = origPort;
		this.destIP = destIP;
		this.destPort = destPort;
		this.transIP = transIP;
		this.transPort = transPort;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public NATLog(String json) {
//		System.out.println("Constructing from: '" + json + "'");
		//{"key":1355359129559555,"time":"2012-11-10 00:47:01","type":"c","app":"any","orig":"ge-1/0/0.120","origIp":"192.168.0.100","origPort":"16000","destIp":"66.129.224.36","destPort":"55261","transIp":"192.168.0.200","transPort":"18000"}
		String[] tokens = {
//				"\"key\":\"",
				"\"key\":",
				"\"time\":\"",
				"\"type\":\"",
				"\"app\":\"",
				"\"orig\":\"",
				"\"origIp\":\"",
//				"\"origPort\":\"",
				"\"origPort\":",
				"\"destIp\":\"",
//				"\"destPort\":\"",
				"\"destPort\":",
				"\"transIp\":\"",
//				"\"transPort\":\""
				"\"transPort\":"
				};
		String[] vals = new String[tokens.length];
//		System.out.println("json=" + json);
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
//			System.out.println("Working on token " + token);
			int fromIdx = json.indexOf(token);
//			System.out.println("  fromIdx = " + fromIdx);
			String endChar = "\""; //assume it's a double quote (ie this val is a string)
			//if it's the key, origPort, or destPort field, endChar is ,
			if (i == 0 || i == 6 || i == 8) endChar = ",";
			//if it's the transPort field, then the endChar is }
			if (i == 10) endChar = "}";
			vals[i] = json.substring(fromIdx + token.length(), json.indexOf(endChar, fromIdx + token.length()));
//			System.out.println("  vals[" + i + "]=" + vals[i]);
		}
		this.key = vals[0];
		this.time = vals[1];
		this.type = vals[2];
		this.app = vals[3];
		this.orig = vals[4];
		this.origIP = vals[5];
		this.origPort = vals[6];
		this.destIP = vals[7];
		this.destPort = vals[8];
		this.transIP = vals[9];
		this.transPort = vals[10];

		//overwrite the key with our own uuid
		this.key = NATLog.makeKey();
	}
	
	public NATLog sample() {
		return new NATLog("15:38:22 08/21/2012", "c", "any", "ge-1/0/0.120", 
	    		"192.168.0." + new Random().nextInt(256), String.valueOf((1000 + new Random().nextInt(9000))), 
	    		"192.168.0." + new Random().nextInt(256), String.valueOf((1000 + new Random().nextInt(9000))), 
	    		"192.168.0." + new Random().nextInt(256), String.valueOf((1000 + new Random().nextInt(9000))));
	}

	@Override
	public String toString() {
		//treats 'key' field as a number (ie no double quotes)
//		return "{\"time\":\"2012-11-10 00:47:01\",\"type\":\"c\",\"app\":\"any\",\"orig\":\"ge-1/0/0.120\",\"origIp\":\"192.168.0.119\",\"origPort\":\"10003\",\"destIp\":\"192.168.0.127\",\"destPort\":\"10105\",\"transIp\":\"192.168.0.233\",\"transPort\":\"10205\"}";
		return "{\"key\":" + (this.key == null ? "" : this.key) + ",\"time\":\"" + this.time + "\",\"type\":\"" + this.type + "\"," +
				"\"app\":\"" + this.app + "\",\"orig\":\"" + this.orig + "\"," + 
//				"\"origIp\":\"" + this.origIP + "\",\"origPort\":\"" + this.origPort + "\"," +
				"\"origIp\":\"" + this.origIP + "\",\"origPort\":" + this.origPort + "," +
//				"\"destIp\":\"" + this.destIP + "\",\"destPort\":\"" + this.destPort + "\"," +
				"\"destIp\":\"" + this.destIP + "\",\"destPort\":" + this.destPort + "," +
//				"\"transIp\":\"" + this.transIP + "\",\"transPort\":\"" + this.transPort + "\"}";
				"\"transIp\":\"" + this.transIP + "\",\"transPort\":" + this.transPort + "}";
	}
	
	//TODO
	public String asSyslog() {
		//Nov 10 01:09:55 testname1 (FPC Slot 8, PIC Slot 2) 2012-11-10 01:09:55 NAT444{nat444}[FWNAT]: ASP_SFW_DELETE_FLOW: proto 17 (UDP) 
		//application: any, ge-1/0/0.120:192.168.0.163:17000 -> 66.129.224.36:55261, deleting  forward or watch flow ; source address and port translate to 192.168.1.7:19000
		return "NOT IMPLEMENTED";				
	}
	
	public static NATLog fromSyslogOptimized(StringBuffer buf) throws Exception {
		int i = 0;
		int end = 0;
		
		i = ignoreUntilChar(buf, ')', i); //ignore until after first ")"
		i+= 2; //this gets us one past the ")"
		
		//time is until " NAT"
		String time = buf.substring(i, i + 19);
		
		//ignore until after "application:"
		do {
			i++;
		} while (!(buf.charAt(i) == ':' && (buf.charAt(i-1) == 'n') && (buf.charAt(i-2) == 'o') && (buf.charAt(i-3) == 'i') && 
				(buf.charAt(i-4) == 't') && (buf.charAt(i-5) == 'a') && (buf.charAt(i-6) == 'c') && (buf.charAt(i-7) == 'i') && 
				(buf.charAt(i-8) == 'l') && (buf.charAt(i-9) == 'p') && (buf.charAt(i-10) == 'p') && (buf.charAt(i-11) == 'a')));
		i+= 2; //skip past the space after the ':' 
		
		//app is until ","
		end = ignoreUntilChar(buf, ',', i);
		String app = buf.substring(i, end);
		
		//ignore until after ", "
		i = end + 2;
		
		//orig is until ":"
		end = ignoreUntilChar(buf, ':', i);
		String orig = buf.substring(i, end);
		
		//ignore until after ":"
		i = end + 1;
		
		//origIP is until ":"
		end = ignoreUntilChar(buf, ':', i);
		String origIP = buf.substring(i, end);
		
		//ignore until after ":"
		i = end + 1;
		
		//origPort is until " "
		end = ignoreUntilChar(buf, ' ', i);
		String origPort = buf.substring(i, end);
		
		//ignore until after " -> "
		i = end + 4;
		
		//destIP is until ":"
		end = ignoreUntilChar(buf, ':', i);
		String destIP = buf.substring(i, end);
		
		//ignore until after ":"
		i = end + 1;
		
		//destPort is until ","
		end = ignoreUntilChar(buf, ',', i);
		String destPort = buf.substring(i, end);

		//ignore until after ", "
		i = end + 2;
		
		//type is "C" if everything until next " " is "creating", "D" otherwise
		end = i;
		while (buf.charAt(end) != ' ') {
			end++;
		}
		String typeStr = buf.substring(i, end);
		String type = typeStr.equalsIgnoreCase("creating") ? "C" : "D";
		
		//ignore until after "translate to "
		do {
			i++;
		} while (!(buf.charAt(i) == 'o' && (buf.charAt(i-1) == 't') && (buf.charAt(i-2) == ' ') && (buf.charAt(i-3) == 'e') && 
				(buf.charAt(i-4) == 't') && (buf.charAt(i-5) == 'a') && (buf.charAt(i-6) == 'l') && (buf.charAt(i-7) == 's') && 
				(buf.charAt(i-8) == 'n') && (buf.charAt(i-9) == 'a') && (buf.charAt(i-10) == 'r') && (buf.charAt(i-11) == 't')));
		i+= 2; //skip past the space after the 'to' 
		
		//transIP is until ":"
		end = ignoreUntilChar(buf, ':', i);
		String transIP = buf.substring(i, end);

		//ignore until after ":"
		i = end + 1;
		
		//transPort is until end
		String transPort = buf.substring(i);
		
		NATLog natLog = new NATLog(time, type, app, orig, origIP, origPort, destIP, destPort, transIP, transPort);
		
		//overwrite the key with our own uuid
		natLog.setKey(NATLog.makeKey());
		
		return natLog;
	}
	
	public static NATLog fromSyslog(String syslog) throws Exception {
		String time = syslog.substring(syslog.indexOf(")") + 2, syslog.indexOf(" NAT"));
		String type = syslog.toLowerCase().indexOf("creating") != -1 ? "C" : "D";
		int appStart = syslog.indexOf("application: ") + "application: ".length();
		String app = syslog.substring(appStart, syslog.indexOf(",", appStart));
		String orig = syslog.substring(syslog.indexOf(",", appStart) + 2, syslog.indexOf(":", appStart));
		int origIpStart = syslog.indexOf(orig) + orig.length() + 1;
		String origIP = syslog.substring(origIpStart, syslog.indexOf(":", origIpStart));
		String origPort = syslog.substring(syslog.indexOf(origIP) + origIP.length() + 1, syslog.indexOf(" ->"));
		String destIP = syslog.substring(syslog.indexOf(" -> ") + " -> ".length(), syslog.indexOf(":", syslog.indexOf(" -> ")));
		int destPortStart = syslog.indexOf(destIP) + destIP.length() + 1;
		String destPort = syslog.substring(destPortStart, syslog.indexOf(",", destPortStart));
		int transIPStart = syslog.indexOf("translate to ") + "translate to ".length();
		String transIP = syslog.substring(transIPStart, syslog.indexOf(":", transIPStart));
		String transPort = syslog.substring(syslog.lastIndexOf(":") + 1);
		
		NATLog natLog = new NATLog(time, type, app, orig, origIP, origPort, destIP, destPort, transIP, transPort);
		
		//overwrite the key with our own uuid
		natLog.setKey(NATLog.makeKey());
		
		return natLog;
	}
	
	public static NATLog[] parseAsPipeSeparatedJSON(String val) {
		String[] vals = val.split("\\|");
		NATLog[] natLogs = new NATLog[vals.length];
		for (int i = 0; i < vals.length; i++) {
			if (!StringUtils.isEmpty(vals[i])) {
				natLogs[i] = new NATLog(vals[i]);
				natLogs[i].setKey(NATLog.makeKey());
			}
		}
		
		return natLogs;
	}
	
	public static NATLog[] parseAsNewlineSeparatedJSON(String val) {
		val = val.substring(2, val.length() - 2); //strip off first and last char
		String[] vals = val.split("\\n");
		
//		System.out.println("Vals:");
//		for (int i = 0; i < vals.length; i++) {
//			System.out.println(i + "->" + vals[i]);
//		}
		
		NATLog[] natLogs = new NATLog[vals.length];
		for (int i = 0; i < vals.length; i++) {
			if (!StringUtils.isEmpty(vals[i])) {
//				System.out.println("Handling val: " + vals[i]);
				if (vals[i].startsWith(",")) {
//					System.out.println("    starts with comma");
					natLogs[i] = new NATLog(vals[i].substring(1));
				} else {
					natLogs[i] = new NATLog(vals[i]);
				}
				natLogs[i].setKey(NATLog.makeKey());
			}
		}
		
		return natLogs;
	}
	
	public static NATLog[] parseAsPipeSeparatedSyslog(String val) {
		String[] vals = val.split("\\|");
		NATLog[] natLogs = new NATLog[vals.length];
		for (int i = 0; i < vals.length; i++) {
			if (!StringUtils.isEmpty(vals[i])) {
				try {
					natLogs[i] = NATLog.fromSyslog(vals[i]);
				} catch (Exception ex) {
//					System.out.println("Caught " + ex.getClass().getName() + " (" + ex.getMessage() + 
//							") parsing syslog on line " + i + " into NATLog object: '" + vals[i] + "'");
				}
			}
		}
		
		return natLogs;
	}
	
	public static NATLog[] parseAsNewlineSeparatedSyslog(String val) {
		String[] lines = val.split("\\n");
		NATLog[] natLogs = new NATLog[lines.length];
		for (int i = 0; i < lines.length; i++) {
			if (!StringUtils.isEmpty(lines[i]) && !"|".equals(lines[i])) {
//				System.out.println("Line " + i + "=" + lines[i]);
				try {
					natLogs[i] = NATLog.fromSyslog(lines[i]);
				} catch (Exception ex) {
//					System.out.println("Caught " + ex.getClass().getName() + " (" + ex.getMessage() + 
//							") parsing syslog on line " + i + " into NATLog object: '" + lines[i] + "'");
				}
			}
		}
		
		return natLogs;
	}
	
	public Object getValForField(String fieldName) {
		Object itemVal = null;
		
		if ("origIP".equals(fieldName)) {
			itemVal = getOrigIP();
		} else if ("origPort".equals(fieldName)) {
			itemVal = getOrigPort();
		} else if ("destIP".equals(fieldName)) {
			itemVal = getDestIP();
		} else if ("destPort".equals(fieldName)) {
			itemVal = getDestPort();
		} else if ("transIP".equals(fieldName)) {
			itemVal = getTransIP();
		} else if ("transPort".equals(fieldName)) {
			itemVal = getTransPort();
		} else if ("orig".equals(fieldName)) {
			itemVal = getOrig();
		} else if ("type".equals(fieldName)) {
			itemVal = getType();
		} else if ("app".equals(fieldName)) {
			itemVal = getApp();
		}
		
		return itemVal;
	}

	public String getTime() {
		return time;
	}

	public String getType() {
		return type;
	}

	public String getApp() {
		return app;
	}

	public String getOrig() {
		return orig;
	}

	public String getOrigIP() {
		return origIP;
	}

	public String getOrigPort() {
		return origPort;
	}

	public String getDestIP() {
		return destIP;
	}

	public String getDestPort() {
		return destPort;
	}

	public String getTransIP() {
		return transIP;
	}

	public String getTransPort() {
		return transPort;
	}
	
	
}
