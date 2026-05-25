package com.neocoretechs.relatrix.test.kv.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.neocoretechs.relatrix.DuplicateKeyException;

import com.neocoretechs.relatrix.RelatrixKVJson;

import com.neocoretechs.relatrix.key.IndexResolver;

/**
 * The set of tests verifies the lower level {@link DBKey} functions in the {@link  Relatrix}
 * NOTES:
 * A database unique to this test module should be used.
 * program argument is database i.e. C:/users/you/Relatrix/TestDB2
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017
 *
 */
public class BatteryDBKey {
	public static boolean DEBUG = false;
	static String x = "{\"timestamp\":1779166000301,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";

	static int min = 0;
	static int max = 100000;
	static int numDelete = 100; // for delete test

	static ArrayList<String> findkeys = new ArrayList<String>();
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		if(argv.length < 1) {
			System.out.println("Usage: java com.neocoretechs.relatrix.test.kv.BatteryDBKey <directory_tablespace_path>");
			System.exit(1);
		}
		RelatrixKVJson.getInstance().setTablespace(argv[0]);
		IndexResolver.setLocal();
		battery1AR17(argv);
		battery1(argv);
		battery1AR4(argv);
		battery1AR7(argv);
		battery1AR17(argv);
		 System.out.println("BatteryDBKey TEST BATTERY COMPLETE.");
		
	}
	/**
	 * Loads up on keys, should be 0 to max-1, or min, to max -1
	 * Ensure that we start with known baseline number of keys
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1(String[] argv) throws Exception {
		System.out.println("KV Battery1 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String xj = x;
		JSONObject jo = new JSONObject(xj);
		//Integer payload = 0;

		for(int i = min; i < max; i++) {
			try {
			RelatrixKVJson.store(jo.toString(), argv);
			long tim = jo.getLong("timestamp");
			++tim;
			jo.put("timestamp",tim);
			++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		System.out.println("KV BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	/**
	 * check order
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR4(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		String prev = x;
		JSONObject jo = new JSONObject(x);
		Class<?> c = RelatrixKVJson.getJsonClass(jo);
		// set up previous key as first key, insert to key map
		prev = (String) RelatrixKVJson.firstKey(c);
		Iterator<?> its = RelatrixKVJson.findTailMapKV(prev);
		System.out.println("KV Battery1AR4");
		while(its.hasNext()) {
			Map.Entry<String,Object> nex = (Map.Entry<String,Object>) its.next();
			findkeys.add(nex.getKey());
			if(nex.getKey().compareTo(prev) <= 0) { // should always be >
			// Map.Entry
				System.out.println("KV RANGE KEY MISMATCH: prev:"+prev+" nex:"+nex.getKey()+" cmpr:"+nex.getKey().compareTo(prev));
				throw new Exception("KV RANGE KEY MISMATCH: prev:"+prev+" nex:"+nex.getKey()+" cmpr:"+nex.getKey().compareTo(prev));
			}
			prev = nex.getKey();
		}
		 System.out.println("BATTERY1AR4 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * Testing of Iterator<?> its = RelatrixKV.keySet;
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR7(String[] argv) throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x);
		Class<?> c = RelatrixKVJson.getJsonClass(jo);
		Iterator<?> its = RelatrixKVJson.keySet(c);
		System.out.println("KV Battery1AR7");
		while(its.hasNext()) {
			String y = (String) its.next();
			++i;
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		 System.out.println("KV BATTERY1AR7 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x);
		Class<?> c = RelatrixKVJson.getJsonClass(jo);
		int j = min;
		long s = RelatrixKVJson.size(c);
		System.out.println("Cleaning DB of "+s+" elements.");
		Iterator<?> it = RelatrixKVJson.keySet(c);
		long timx = System.currentTimeMillis();
		int i = 0;
		while(it.hasNext()) {	
			++i;
			Object fkey = it.next();
			System.out.println(i+".) "+fkey);
			RelatrixKVJson.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("Key "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
	
		long siz = RelatrixKVJson.size(c);
		if(siz > 0) {
			Iterator<?> its = RelatrixKVJson.entrySet(c);
			while(its.hasNext()) {
				Comparable nex = (Comparable) its.next();
				//System.out.println(i+"="+nex);
				System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
			}
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		 System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	
}
