package com.neocoretechs.relatrix.test.kv.json.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.json.JSONObject;

import com.neocoretechs.rocksack.iterator.Entry;
import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.client.json.RelatrixKVClientJson;

import com.neocoretechs.relatrix.key.DBKey;
import com.neocoretechs.relatrix.key.KeySet;

/**
 * The set of tests verifies the lower level {@link KeySet} functions in the {@link  RelatrixKVClientJson}
 * NOTES:
 * A database unique to this test module should be used.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017,2026
 *
 */
public class BatteryKeysetJsonClient {
	public static boolean DEBUG = true;
	static KeySet keyset;
	static int i;
	static int min = 0;
	static int max = 10000;
	static int numDelete = 100; // for delete test
	static ArrayList<Comparable> keys = new ArrayList<Comparable>();
	static ArrayList<Comparable> findkeys = new ArrayList<Comparable>();
	static String x =     "{\"timestamp\":1779166030000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\"}]}]}";
	static String x50k =  "{\"timestamp\":1779166050000,\"RightImage\":[{\"count\":0, \"affections\":[ {\"name\":\"alligator\"}]}]}";
	static String xfull = "{\"timestamp\":1779166070000,\"LeftImage\":[{ \"count\":1, \"erections\":[ { \"name\":\"toilet\"}]}]}";

	static JSONObject xf = new JSONObject(xfull);
	static JSONObject xo50 = new JSONObject(x50k);
	static JSONObject xo = new JSONObject(x);
	static Class<?> xfClass, xoClass, x50Class, xClass;
	
	static JSONObject jox = new JSONObject(x);
	static JSONObject jo2 = new JSONObject(x50k);
	
	static RelatrixKVClientJson rc;
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		rc = new RelatrixKVClientJson(argv[0], Integer.parseInt(argv[1]));
		//battery1AR17();
		xClass = rc.createClass(jox);
		x50Class = rc.createClass(jo2);
		xfClass = rc.createClass(xf);
		if(rc.size(xfClass) == 0)
			battery1();
		battery2();
	
		//battery1AR17();
		 System.out.println("BatteryKeysetJson TEST BATTERY COMPLETE.");
		
	}
	/**
	 * Loads up on keys, should be 0 to max-1, or min, to max -1
	 * Ensure that we start with known baseline number of keys
	 * @throws Exception
	 */
	public static void battery1() throws Exception {
		System.out.println("Battery1 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		for(i = min; i < max; i++) {
			long tim = jox.getLong("timestamp");
			++tim;
			jox.put("timestamp",tim);
			tim = jo2.getLong("timestamp");
			++tim;
			jo2.put("timestamp",tim);
			rc.store(jox,UUID.randomUUID());
			rc.store(jo2,UUID.randomUUID());
			rc.store(xf,UUID.randomUUID());
			if(DEBUG)
				if(recs % 1000 == 0)
					System.out.println("Relatrix.store stored :"+recs);
			++recs;
		}	
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	
	private static void battery2() throws IllegalAccessException, ClassNotFoundException, IOException {
		JSONObject jox = new JSONObject(x);
		JSONObject jo2 = new JSONObject(x50k);
		for(int i = min; i < max; i++) {
			long tim = jox.getLong("timestamp");
			++tim;
			jox.put("timestamp",tim);
			tim = jo2.getLong("timestamp");
			++tim;
			jo2.put("timestamp",tim);
			Iterator<?> it = rc.findTailMapKV(jo2);
			int cnt = 0;
			while(it.hasNext()) {
				Object o = it.next();
				Map.Entry e = (Map.Entry)o;
				cnt++;
			}
		}
	}
	
	
	
	/**
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17() throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		long s = rc.size(DBKey.class);
		System.out.println("Cleaning DB of "+s+" elements.");
		Iterator it = rc.keySet(DBKey.class);
		long timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			rc.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("DBKey remove "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		// remove payload reverse index
		s = rc.size(KeySet.class);
		it = rc.keySet(KeySet.class);
		timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			rc.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("KeySet remove "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		s = rc.size(String.class);
		it = rc.keySet(String.class);
		timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			rc.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("String remove "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		long siz = rc.size(DBKey.class);
		if(siz > 0) {
			Iterator<?> its = rc.entrySet(DBKey.class);
			while(its.hasNext()) {
				Comparable nex = (Comparable) its.next();
				//System.out.println(i+"="+nex);
				System.out.println("RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
			}
			System.out.println("RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		 System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

}
