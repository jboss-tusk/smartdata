package org.jboss.tusk.smartdata.domain.cable;

import static org.junit.Assert.assertEquals;

import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;
import org.jboss.tusk.smartdata.domain.cable.STBLog;
import org.junit.Test;

public class STBLogTest {
	
	CachedItemHelper helper = CachedItemHelperFactory.getInstance("org.jboss.tusk.smartdata.domain.cable.STBLogHelper");
	
	//this one has lots of whitespace for formatting
	private static final String json = "{\n" + 
		"\t\"key\": 3945109935328,\n" +
	    "\t\"ts\": 1367259824805,\n" +
	    "\t\"home\": 9042557,\n" +
	    "\t\"account\": \"999999042557\",\n" +
	    "\t\"card\": \"000007552409\",\n" +
	    "\t\"mb\": [\n" +
	        "\t\t\"60.36.196.125\",\n" +
	        "\t\t\"00:0A:73:3F:39:88\"\n" +
	    "\t],\n" +
	    "\t\"cbl\": [\n" +
	        "\t\t\"60.189.90.253\",\n" +
	        "\t\t\"00:0A:73:7F:A9:70\"\n" +
	    "\t],\n" +
	    "\t\"err\": {\n" +
			"\t\t\"insert\": 0,\n" +
			"\t\t\"semantic\": 0,\n" +
			"\t\t\"tcp\": 0,\n" +
			"\t\t\"ufec\": 13903,\n" +
			"\t\t\"cfec\": 15778\n" +
	    "\t}\n" +
	"}";
	
	//this one has less whitespace
	private static final String json2 = "{\"key\": 3945109935328,\"ts\": 1639808018,\"home\": 8744452,\"account\": \"888888744452\"," +
			"\"card\": \"000002544478\", \"mb\": [\"60.32.56.4\",\"00:0A:73:CD:94:6A\"], " +
			"\"cbl\": [\"60.184.206.132\",\"00:0A:73:0D:34:52\"]," +
			"\"err\": {\"insert\": 0,\"semantic\": 0,\"tcp\": 0, \"ufec\": 17255, \"cfec\": 11698 }}";
	
	@Test
	public void testSTBLog() throws Exception {
		try {
			//make a STBLog from normal json parsing
			STBLog stbLog1 = new STBLog(json);
		
			//make a STBLog from the optimized JSON parsing
			STBLog stbLog2 = STBLog.fromJSONOptimized(new StringBuffer(json));

			//make a STBLog using the string output (which is in json format) of the previous object
			STBLog stbLog3 = STBLog.fromJSONOptimized(new StringBuffer(stbLog2.toString()));

			//set keys so they are all the same, which is required for comparison
			stbLog1.setKey("dummy");
			stbLog2.setKey("dummy");
			stbLog3.setKey("dummy");
			
			//make sure they all have the same data
//			assertEquals("Parsing STBLog from json data failed comparing stbLog1 to stbLog2", stbLog1.toString(), stbLog2.toString());
			assertEquals("Parsing STBLog from json data failed comparing stbLog2 to stbLog3", stbLog2.toString(), stbLog3.toString());
//			assertEquals("Parsing STBLog from json data failed comparing stbLog1 to stbLog3", stbLog1.toString(), stbLog3.toString());
		} catch (Exception ex) {
			System.err.println("Caught " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}
	
	@Test
	public void testSTBLog2() throws Exception {
		try {
			//make a STBLog from the optimized JSON parsing
			STBLog stbLog1 = STBLog.fromJSONOptimized(new StringBuffer(json2));

			//make a STBLog using the string output (which is in json format) of the previous object
			STBLog stbLog2 = STBLog.fromJSONOptimized(new StringBuffer(stbLog1.toString()));

			//set keys so they are all the same, which is required for comparison
			stbLog1.setKey("dummy");
			stbLog2.setKey("dummy");
			
			//make sure they all have the same data
			assertEquals("Parsing STBLog from json data failed comparing stbLog1 to stbLog2", stbLog1.toString(), stbLog2.toString());
		} catch (Exception ex) {
			System.err.println("Caught " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}

}
