package org.jboss.tusk.smartdata.domain.cgnat;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jboss.tusk.smartdata.domain.cgnat.NATLog;
import org.junit.Test;

public class NATLogTest {
	
	static final String[] syslogStrings = {
		"Nov 10 01:07:27 testname1 (FPC Slot 8, PIC Slot 2) 2012-11-10 01:07:27 NAT444{nat444}[FWNAT]: ASP_SFW_DELETE_FLOW: proto 17 (UDP) application: any, ge-1/0/0.120:192.168.0.130:17700 -> 66.129.224.36:55261, deleting  forward or watch flow ; source address and port translate to 192.168.0.230:19700",
		"Nov 10 01:07:27 testname1 (FPC Slot 8, PIC Slot 2) 2012-11-10 01:07:27 NAT444{nat444}[FWNAT]: ASP_SFW_DELETE_FLOW: proto 17 (UDP) application: any, ge-1/0/0.120:192.168.0.130:17700 -> 66.129.224.36:55261, deleting  forward or watch flow ; source address and port translate to 192.168.0.230:19700",
		"Nov 10 01:04:23 testname1 (FPC Slot 8, PIC Slot 2) 2012-11-10 01:04:23 NAT444{nat444}[FWNAT]: ASP_SFW_DELETE_FLOW: proto 17 (UDP) application: any, ge-1/0/0.120:192.168.0.160:17749 -> 66.129.224.36:55261, deleting  forward or watch flow ; source address and port translate to 192.168.1.4:19749",
		"Nov 10 01:06:25 testname1 (FPC Slot 8, PIC Slot 2) 2012-11-10 01:06:25 NAT444{nat444}[FWNAT]: ASP_SFW_CREATE_ACCEPT_FLOW: proto 17 (UDP) application: any, ge-1/0/0.120:192.168.0.102:17062 -> 66.129.224.36:55261, creating forward or watch flow ; source address and port translate to 192.168.0.202:19062",
		"Nov 10 01:09:55 testname1 (FPC Slot 8, PIC Slot 2) 2012-11-10 01:09:55 NAT444{nat444}[FWNAT]: ASP_SFW_DELETE_FLOW: proto 17 (UDP) application: any, ge-1/0/0.120:192.168.0.163:17000 -> 66.129.224.36:55261, deleting  forward or watch flow ; source address and port translate to 192.168.1.7:19000",
		"Nov 10 01:04:12 testname1 (FPC Slot 8, PIC Slot 2) 2012-11-10 01:04:12 NAT444{nat444}[FWNAT]: ASP_SFW_DELETE_FLOW: proto 17 (UDP) application: any, ge-1/0/0.120:192.168.0.136:16925 -> 66.129.224.36:55261, deleting  forward or watch flow ; source address and port translate to 192.168.0.236:18925"
	};
	
	@Test
	public void testFromSyslogOptimized() {
		for (int i = 0; i < syslogStrings.length; i++) {
			try {
				NATLog natLog = NATLog.fromSyslogOptimized(new StringBuffer(syslogStrings[i]));
				//System.out.println("natLog=" + natLog);
				
				NATLog natLog2 = NATLog.fromSyslog(syslogStrings[i]);
				//System.out.println("natLog=" + natLog);
				
				natLog.setKey("test");
				natLog2.setKey("test");
				if (!natLog.toString().equals(natLog2.toString())) {
					throw new Exception ("Two parsing approaches don't match.");
				}
			} catch (Exception ex) {
				System.err.println("Caught exception on 1: " + ex.getMessage());
			}
			System.out.println("");
		}
	}
	
//	@Test
//	public void testSyslogParse() {
//		try {
//			String text = FileUtils.readFileToString(new File("/tmp/syslog.data"));
//			System.out.println("Read syslog file: \n" + text + "\n");
//			NATLog[] natLogs = NATLog.parseAsNewlingSeparatedSyslog(text);
//			System.out.println("NATLogs:");
//			for (int i = 0; i < natLogs.length; i++) {
//				System.out.println("  " + i + "-->" + natLogs[i]);
//			}
//		} catch (Exception ex) {
//			System.err.println(ex.getMessage());
//		}
//	}

	
	//{"time":"2012-11-10 00:47:01","type":"c","app":"any","orig":"ge-1/0/0.120","origIp":"192.168.170.69","origPort":10006,"destIp":"192.168.182.110","destPort":10108,"transIp":"192.168.43.3","transPort":10200
//	@Test
//	public void testJSONParse() {
//		try {
//			String text = FileUtils.readFileToString(new File("/tmp/syslog.json"));
//			System.out.println("Read json file: \n" + text + "\n");
//			NATLog[] natLogs = NATLog.parseAsNewlineSeparatedJSON(text);
//			System.out.println("NATLogs:");
//			for (int i = 0; i < natLogs.length; i++) {
//				System.out.println("  " + i + "-->" + natLogs[i]);
//			}
//		} catch (Exception ex) {
//			System.err.println(ex.getMessage());
//		}
//	}

}
