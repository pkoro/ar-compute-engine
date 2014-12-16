package myudf;


import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.DefaultDataBag;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import utils.Aggregator;
import utils.AvProfileManager;
import utils.MetricProfileManager;
import utils.Slot;



public class SiteStatus extends EvalFunc<Tuple> {
	
	private static String mongo_host;
	private static int mongo_port;
		
	private static TupleFactory tupFactory = TupleFactory.getInstance();
    private static BagFactory bagFactory = BagFactory.getInstance();
	
    private static AvProfileManager ap_mgr;
	
    public SiteStatus(String inp_mongo_host, String inp_mongo_port) throws UnknownHostException{
		mongo_host = inp_mongo_host;
		mongo_port = Integer.parseInt(inp_mongo_port);
		
		// Initialize Metric Profile Manager
		ap_mgr = new AvProfileManager();
		ap_mgr.loadFromMongo(mongo_host,mongo_port);
	}
	
	

	
	@Override
	public Tuple exec(Tuple input) throws IOException {
		

	    String profile = (String) input.get(0);		
		String roc = (String) input.get(1);
		String site = (String) input.get(2);
		DefaultDataBag bag =  (DefaultDataBag) input.get(3);
		Iterator<Tuple> it_bag = bag.iterator();
		
		//Create Group Aggregators
		Map<String,Aggregator> all = new HashMap<String,Aggregator>();
		
		//Create ultimate aggregator for whole site
		Aggregator aggr = new Aggregator();
		
	    //Populate with groups available
		 for (String group_item : ap_mgr.getProfileGroups(profile))
		 {
		    	all.put(group_item, new Aggregator());
		 }
	
	    while (it_bag.hasNext()){
	    	Tuple cur_item = it_bag.next();
	    	
	    	//get hostname of cur_item
	    	String cur_service = (String)cur_item.get(0);
	    	String cur_timestamp = (String)cur_item.get(1);
	    	String cur_status = (String)cur_item.get(2);
	    	String cur_prevstate = (String)cur_item.get(3);
	    	int cur_date_int = (Integer)cur_item.get(4);
	    	int cur_time_int = (Integer)cur_item.get(5);
	    	
	    	//get the correct group
	    	String cur_group = ap_mgr.getServiceGroup(profile, cur_service);
	    	all.get(cur_group).insert(cur_service, cur_time_int, cur_date_int, cur_timestamp, cur_status, cur_prevstate);
	    	
	    }
	    
	    // aggregate for all groups 
	    
	    for (String group_item: all.keySet())  {
	    	all.get(group_item).optimize();
			all.get(group_item).project();
			all.get(group_item).aggregateAND();
			all.get(group_item).aggrPrevState();
			   
			for (Entry<Integer, Slot> item: all.get(group_item).aggr_tline.entrySet())
			{
			   Slot item_value = item.getValue();
			   aggr.insert(group_item, item_value.time_int, item_value.date_int, item_value.timestamp, item_value.status, item_value.prev_status);
			
			}
			
		}
	    
	    
	    // aggregate for the whole site at last	    
	    aggr.optimize();
	    aggr.project();
	    aggr.aggregateAND();
	    aggr.aggrPrevState();
	    
	    //Create output Tuple
	    Tuple output = tupFactory.newTuple();
	    DataBag outBag = bagFactory.newDefaultBag();
	    
	    output.append(profile);
	    output.append(roc);
	    output.append(site);
	    
	   	     
	    // Iterate aggregator
	    
		for (Entry<Integer, Slot> item: aggr.aggr_tline.entrySet()) {
			
			Slot item_value = item.getValue();
			Tuple cur_tupl = tupFactory.newTuple();
			cur_tupl.append(item.getValue().timestamp);
			cur_tupl.append(item.getValue().status);
			cur_tupl.append(item.getValue().prev_status);
			cur_tupl.append(item_value.date_int);
			cur_tupl.append(item_value.time_int);
			outBag.add(cur_tupl);
		}
		
		
	    
	    output.append(outBag);
		
	    if (outBag.size()==0) return null;
	    
		return output;
		
	}
	
	
	
	@Override
    public Schema outputSchema(Schema input) {
   
		
		Schema.FieldSchema profile = new Schema.FieldSchema("profile", DataType.CHARARRAY);
		Schema.FieldSchema roc = new Schema.FieldSchema("roc", DataType.CHARARRAY);
		Schema.FieldSchema site = new Schema.FieldSchema("site", DataType.CHARARRAY);
			 
        
      
        Schema.FieldSchema timestamp = new Schema.FieldSchema("timestamp", DataType.CHARARRAY);
        Schema.FieldSchema status = new Schema.FieldSchema("status", DataType.CHARARRAY);
        Schema.FieldSchema prev_state = new Schema.FieldSchema("prev_state", DataType.CHARARRAY);
        Schema.FieldSchema date_int = new Schema.FieldSchema("date_int", DataType.INTEGER);
        Schema.FieldSchema time_int = new Schema.FieldSchema("time_int", DataType.INTEGER);
        
        Schema service_host = new Schema();
        Schema timeline = new Schema();
        
        service_host.add(profile);
        service_host.add(roc);
        service_host.add(site);
             
        
     
        timeline.add(timestamp);
        timeline.add(status);
        timeline.add(prev_state);
        timeline.add(date_int);
        timeline.add(time_int);
        
        Schema.FieldSchema timelines = null;
        try {
            timelines = new Schema.FieldSchema("timelines", timeline, DataType.BAG);
        } catch (FrontendException ex) {
            Logger.getLogger(AddTopology.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        service_host.add(timelines);
        
        
        try {
            return new Schema(new Schema.FieldSchema("service_host", service_host, DataType.TUPLE));
        } catch (FrontendException ex) {
           
        }
        
        return null;
    }
	
	
	

}