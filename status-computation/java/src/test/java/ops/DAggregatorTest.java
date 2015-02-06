package ops;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

public class DAggregatorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Assert that files are present
		assertNotNull("Test file missing", DAggregatorTest.class.getResource("/ops/EGI-algorithm.json"));
	}
	
	@Test
	public void test() throws URISyntaxException, FileNotFoundException, ParseException {
		
		URL resJsonFile = OpsManagerTest.class.getResource("/ops/EGI-algorithm.json");
		File jsonFile = new File(resJsonFile.toURI());
		
		DAggregator dAgg = new DAggregator();
		OpsManager opsMgr = new OpsManager();
		
		opsMgr.openFile(jsonFile);
		
		//Create 3 Timelines
		DTimeline t1 = new DTimeline();
		DTimeline t2 = new DTimeline();
		DTimeline t3 = new DTimeline();
		
		t1.setSampling(1440, 120);
		t2.setSampling(1440, 120);
		t3.setSampling(1440, 120);
		
		dAgg.aggregation.setSampling(1440, 120);
		
		//Set First States
		t1.setStartState(opsMgr.getIntStatus("OK"));
		t2.setStartState(opsMgr.getIntStatus("UNKNOWN"));
		t3.setStartState(opsMgr.getIntStatus("OK"));
		
		//Add some timestamps int timeline 1
		t1.insert("2014-01-15T01:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		t1.insert("2014-01-15T05:33:01Z", opsMgr.getIntStatus("OK"));
		t1.insert("2014-01-15T12:50:42Z", opsMgr.getIntStatus("WARNING"));
		t1.insert("2014-01-15T15:33:44Z", opsMgr.getIntStatus("OK"));
		
		//Add some timestamps int timeline 2
		t2.insert("2014-01-15T05:33:44Z", opsMgr.getIntStatus("OK"));
		t2.insert("2014-01-15T08:33:01Z", opsMgr.getIntStatus("MISSING"));
		t2.insert("2014-01-15T12:50:42Z", opsMgr.getIntStatus("CRITICAL"));
		t2.insert("2014-01-15T19:33:44Z", opsMgr.getIntStatus("UNKNOWN"));
		
		//Add some timestamps int timeline 2
		t3.insert("2014-01-15T04:00:44Z", opsMgr.getIntStatus("WARNING"));
		t3.insert("2014-01-15T09:33:01Z", opsMgr.getIntStatus("CRITICAL"));
		t3.insert("2014-01-15T12:50:42Z", opsMgr.getIntStatus("OK"));
		t3.insert("2014-01-15T16:33:44Z", opsMgr.getIntStatus("WARNING"));
		
		t1.finalize();
		t2.finalize();
		t3.finalize();
		
		dAgg.timelines.put("timeline1", t1);
		dAgg.timelines.put("timeline2", t2);
		dAgg.timelines.put("timeline3", t3);
		
		
		dAgg.aggregate("OR",opsMgr);
		
		int[] expected = {0,1,0,0,0,0,0,0,0,0,0,0};
		// Check the arrays
		assertArrayEquals("Aggregation check",expected,dAgg.aggregation.samples);
		
		
		
		
		
		
	}
	
	@Test
	public void test2() throws URISyntaxException, FileNotFoundException, ParseException {
		
		URL resJsonFile = OpsManagerTest.class.getResource("/ops/EGI-algorithm.json");
		File jsonFile = new File(resJsonFile.toURI());
		
		DAggregator dAgg = new DAggregator();
		dAgg.loadOpsFile(jsonFile);
		
		OpsManager opsMgr = new OpsManager();
		opsMgr.openFile(jsonFile);
		
		dAgg.setStartState("m1", opsMgr.getIntStatus("OK"));
		dAgg.setStartState("m2", opsMgr.getIntStatus("OK"));
		dAgg.setStartState("m3", opsMgr.getIntStatus("OK"));
		dAgg.setStartState("m4", opsMgr.getIntStatus("OK"));
		dAgg.insert("m1", "2014-01-15T00:00:00Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m1", "2014-01-15T04:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m1", "2014-01-15T06:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m1", "2014-01-15T12:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m1", "2014-01-15T22:11:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m2", "2014-01-15T01:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m2", "2014-01-15T05:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m2", "2014-01-15T06:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m2", "2014-01-15T22:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m3", "2014-01-15T01:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m3", "2014-01-15T05:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m3", "2014-01-15T11:33:44Z", opsMgr.getIntStatus("CRITICAL"));
		dAgg.insert("m4", "2014-01-15T01:33:44Z", opsMgr.getIntStatus("WARNING"));
		dAgg.insert("m4", "2014-01-15T02:33:44Z", opsMgr.getIntStatus("OK"));
		dAgg.insert("m4", "2014-01-15T24:59:59Z", opsMgr.getIntStatus("CRITICAL"));
		
	
		
		dAgg.finalizeAll();
		dAgg.aggregate("AND",opsMgr);
		
		System.out.println(Arrays.toString(dAgg.aggregation.samples));
		
		
		
		
		
		
		
	}
}
