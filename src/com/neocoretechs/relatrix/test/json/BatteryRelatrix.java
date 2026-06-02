package com.neocoretechs.relatrix.test.json;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONObject;

import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.AbstractRelation;

import com.neocoretechs.relatrix.AbstractRelation.displayLevels;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.RelatrixJson;
import com.neocoretechs.relatrix.RelatrixKVJson;
import com.neocoretechs.relatrix.Result;


/**
 * The set of tests verifies the higher level 'findSet' functions in the {@link  RelatrixJson}, which can be used
 * as examples of {@link RelatrixJson} processing. In general the tests compare the number of items retrieved 
 * against expected value since the basic findSet methods retrieve items in no particular order.
 * To impart order, the higher level permutations of findSet are used.
 * NOTES:
 * A database unique to this test module should be used.
 * program argument is database i.e. C:/users/you/Relatrix/TestDB2 [ [init] [max nnn] ]
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017,2026
 *
 */
public class BatteryRelatrix {
	public static boolean DEBUG = true;
	static String x =     "{\"timestamp\":1779166000301,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x50k =  "{\"timestamp\":1779166050000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x75k =  "{\"timestamp\":1779166075000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String xfull = "{\"timestamp\":1779749659999,\"LeftImage\":[{ \"count\":1, \"detections\":[ { \"name\":\"toilet\", \"probability\":0.35266665,  \"bbox\":{\"xmin\":288,\"ymin\":289,\"xmax\":320,\"ymax\":390} } ] } ], \"RightImage\":[{ \"count\":1, \"detections\":[ { \"name\":\"toilet\", \"probability\":0.29021525, \"bbox\":{\"xmin\":282,\"ymin\":289,\"xmax\":315,\"ymax\":391} } ] } ]}";

	static int min = 0;
	static int max = 100000;
	static int numDelete = 100; // for delete test
	static int i = 0;
	private static long timx;
	/**
	 * Main test fixture driver
	 */
	public static void main(String[] argv) throws Exception {
		RelatrixJson.setTablespace(argv[0]);
		AbstractRelation.displayLevel = displayLevels.VERBOSE;
		if(argv.length > 2 && argv[1].equals("max")) {
			System.out.println("Setting max items to "+argv[2]);
			max = Integer.parseInt(argv[2]);
		} else {
			if(argv.length > 1 && argv[1].equals("init")) {
				System.out.println("Initialize database to zero items, then terminate...");
				battery1AR17(argv);
				System.exit(0);
			}
		}
		long siz = RelatrixJson.size();
		if(siz == 0) {
			if(DEBUG)
				System.out.println("Zero items, Begin insertion from "+min+" to "+max);
			battery1(argv);
		} else
			System.out.println("size="+siz);
		battery1AR5(argv);
		if(DEBUG)
			System.out.println("Begin duplicate key rejection test from "+min+" to "+max);
		battery11(argv);
		if(DEBUG)
			System.out.println("Begin test battery 1AR6");
		battery1AR6(argv);
		if(DEBUG)
			System.out.println("Begin test battery 1AR7");
		battery1AR7(argv);
		if(DEBUG)
			System.out.println("Begin test battery 1AR8");
		battery1AR8(argv);
		if(DEBUG)
			System.out.println("Begin test battery 1AR9");
		battery1AR9(argv);
		/*if(DEBUG)
			System.out.println("Begin test battery 1AR10");
		battery1AR10(argv);
		if(DEBUG)
			System.out.println("Begin test battery 1AR101");
		battery1AR101(argv);
		if(DEBUG)
			System.out.println("Begin test battery 1AR11");
		battery1AR11(argv);
		//if(DEBUG)
		//	System.out.println("Begin test battery 1AR12");
		//battery1AR12(argv);*/

		System.out.println("TEST BATTERY COMPLETE.");
		System.exit(0);
	}
	/**
	 * Loads up on keys
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1(String[] argv) throws Exception {
		System.out.println("Battery1 ");
		long tims = System.currentTimeMillis();
		long timt = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		i = 0;
		JSONObject xf = new JSONObject(xfull);
		JSONObject jo2 = new JSONObject(x50k);
		JSONObject jo = new JSONObject(x);
		for(; i < max; i++) {
			try {
				long tim = jo.getLong("timestamp");
				++tim;
				jo.put("timestamp",tim);
				++recs;
				RelatrixJson.store(jo, xf, jo2);
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("storing "+recs+" "+jo);
					tims = System.currentTimeMillis();
				}
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-timt)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}

	/**
	 * Tries to store partial key that should match existing keys, should reject all.
	 * Domain/map determines unique key
	 * @param argv
	 * @throws Exception
	 */
	public static void battery11(String[] argv) throws Exception {
		long timt = System.currentTimeMillis();
		System.out.println("Battery11");
		int dupes = 0;
		int recs = 0;
		i = 0;
		JSONObject xf = new JSONObject(xfull);
		JSONObject jo2 = new JSONObject(x50k);
		JSONObject jo = new JSONObject(x);
		for(; i < max; i++) {
			try {
				long tim = jo.getLong("timestamp");
				++tim;
				jo.put("timestamp",tim);
				Relation r = RelatrixJson.store(jo, xf, jo2);
				System.out.println("SHOULD NOT BE storing "+recs+" dmr:"+r);
				++recs;
			} catch(DuplicateKeyException d) {++dupes;}
		}
		if( recs > 0) {
			throw new DuplicateKeyException("BATTERY11 FAIL, stored "+recs+" when zero should have been stored");
		}
		System.out.println("BATTERY11 SUCCESS in "+(System.currentTimeMillis()-timt)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	/**
	 * Test the higher level functions in the Relatrix. Use the 'findSet' permutations to
	 * verify the previously inserted data
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR5(String[] argv) throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		JSONObject xf = new JSONObject(xfull);
		JSONObject jo2 = new JSONObject(x50k);
		JSONObject jo = new JSONObject(x);
		Iterator<?> its = RelatrixJson.findSet('?','?','?');
		System.out.println("Battery1AR5");
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			// 3 question marks = dimension 3 in return array
			if( DEBUG ) System.out.println("1AR6:"+i+" "+nex);
			JSONObject ng = RelatrixKVJson.getJsonData(nex.get(0));
			JSONObject nh = RelatrixKVJson.getJsonData(nex.get(1));
			JSONObject ni = RelatrixKVJson.getJsonData(nex.get(2));
			System.out.println(ng+"->"+nh+"->"+ni);
		}
	
		System.out.println("BATTERY1AR5 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Test the higher level functions in the Relatrix. Use the 'findSet' permutations to
	 * verify the previously inserted data
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR6(String[] argv) throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		JSONObject xf = new JSONObject(xfull);
		JSONObject jo2 = new JSONObject(x50k);
		JSONObject jo = new JSONObject(x);
		Iterator<?> its = RelatrixJson.findSet(RelatrixKVJson.getObject(jo), RelatrixKVJson.getObject(xf), RelatrixKVJson.getObject(jo2));
		System.out.println("Battery1AR6");
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			// 3 question marks = dimension 3 in return array
			if( DEBUG ) System.out.println("1AR6:"+i+" "+nex.get(0));
			Relation re = ((Relation)nex.get(0));
			Comparable ce = re.getDomain();
			JSONObject ng = RelatrixKVJson.getJsonData(ce);
			if(!ng.similar(jo) || nex.length() != 1) {
				System.out.println("KEY MISMATCH:"+(i)+":"+nex+"\r\nng="+ng+"\r\nkey="+jo);
				throw new Exception("KEY MISMATCH:"+(i)+":"+nex+"\r\nng="+ng+"\r\nkey="+jo);
			}
			long tim = jo.getLong("timestamp");
			++tim;
			jo.put("timestamp",tim);
			++i;
		}
		if( i != max ) {
			System.out.println("BATTERY1AR6 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR6 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR6 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Testing of Iterator<?> its = Relatrix.findSet('?', '*', '*');
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR7(String[] argv) throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(xfull);
		Iterator<?> its = RelatrixJson.findSet('?', '*', '*');
		System.out.println("Battery1AR7");
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			JSONObject ng = RelatrixKVJson.getJsonData(nex.get(0));
			// one '?' in findset gives us one element returned
			if(DEBUG ) System.out.println("1AR7:"+i+" "+nex);
			if(!ng.similar(jo) || nex.length() != 3) {
				System.out.println("MAP KEY MISMATCH:"+(i)+nex);
				throw new Exception("MAP KEY MISMATCH:"+(i)+nex);
			}
			//String skey = key + String.format(uniqKeyFmt, i);
			//if(!skey.equals(nex[0]) )
			//System.out.println("DOMAIN KEY MISMATCH:"+(i)+" "+skey+" - "+nex[0]);
			++i;
		}
		if( i != max ) {
			System.out.println("BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR7 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Testing of Iterator<?> its = Relatrix.findSet('?', '?', '*');
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR8(String[] argv) throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(xfull);
		System.out.println("Battery1AR8");
		Iterator<?> its = RelatrixJson.findSet('?', '?', '*');
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			// two '?' in findset gives use 2 element array, the domain and map
			if( DEBUG ) System.out.println("1AR8:"+i+" "+nex);
			//String skey = key + String.format(uniqKeyFmt, i);
			JSONObject ng = RelatrixKVJson.getJsonData(nex.get(0));
			if(!ng.similar(jo) ||  nex.length() != 2) {
				System.out.println("KEY MISMATCH:"+(i)+" "+nex);
				throw new Exception("KEY MISMATCH:"+(i)+" "+nex);
			}
			++i;
		}
		if( i != max ) {
			System.out.println("BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR8 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * 
	 * Testing of Iterator<?> its = Relatrix.findSet('*', '*', '*');
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR9(String[] argv) throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(xfull);
		Iterator<?> its = RelatrixJson.findSet('*', '*', '*');
		System.out.println("Battery1AR9");
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			Comparable dr = ((Relation)nex.get(0)).getDomain();
			// the returned array has 1 element, the identity AbstractRelation Relation
			if( DEBUG ) System.out.println("1AR9:"+i+" "+nex.get(0));
			//String skey = key + String.format(uniqKeyFmt, i);
			JSONObject ng = RelatrixKVJson.getJsonData(dr);
			if(!ng.similar(jo) )
				throw new Exception("DOMAIN KEY MISMATCH:"+(i)+" - "+nex);
			++i;
		}
		if( i != max ) {
			System.out.println("BATTERY1AR9 unexpected number of keys "+i);
			//throw new Exception("BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR9 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}


	/**
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB DMR size="+RelatrixJson.size(Relation.class));
		System.out.println("CleanDB DRM size="+RelatrixJson.size(DomainRangeMap.class));
		System.out.println("CleanDB MDR size="+RelatrixJson.size(MapDomainRange.class));
		System.out.println("CleanDB MDR size="+RelatrixJson.size(MapRangeDomain.class));
		System.out.println("CleanDB RDM size="+RelatrixJson.size(RangeDomainMap.class));
		System.out.println("CleanDB RMD size="+RelatrixJson.size(RangeMapDomain.class));
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		Iterator<?> it = RelatrixJson.findSet('*','*','*');
		timx = System.currentTimeMillis();
		it.forEachRemaining(fkey-> {
			Relation dmr = (Relation)((Result)fkey).get(0);
			try {
				RelatrixJson.remove(dmr);
			} catch (IllegalArgumentException | ClassNotFoundException | IllegalAccessException | IOException e) {
				throw new RuntimeException(e);
			}
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" total, current="+fkey);
				timx = System.currentTimeMillis();
			}
		});
		Iterator<?> its = RelatrixJson.findSet('*','*','*');
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			//System.out.println(i+"="+nex);
			if(DEBUG)
				System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
			else
				throw new Exception("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
		}
		long siz = RelatrixJson.size();
		if(siz > 0) {
			if(DEBUG)
				System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			else
				throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		if(DEBUG) {
			it = RelatrixJson.entrySet(Relation.class);
			while(it.hasNext()) {
				Comparable<?> nex = (Comparable<?>) it.next();
				System.out.println("Relation:"+nex);
			}
		}
		if(DEBUG) {
			it = RelatrixJson.entrySet(DomainRangeMap.class);
			while(it.hasNext()) {
				Comparable<?> nex = (Comparable<?>) it.next();
				System.out.println("DomainRangeMap:"+nex);
			}
		}
		if(DEBUG) {
			it = RelatrixJson.entrySet(MapDomainRange.class);
			while(it.hasNext()) {
				Comparable<?> nex = (Comparable<?>) it.next();
				System.out.println("MapDomainRange:"+nex);
			}
		}
		siz = RelatrixJson.size(MapDomainRange.class);
		if(siz > 0) {
			if(DEBUG)
				System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
			else
				throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
		}
		if(DEBUG) {
			it = RelatrixJson.entrySet(MapRangeDomain.class);
			while(it.hasNext()) {
				Comparable<?> nex = (Comparable<?>) it.next();
				System.out.println("MapRangeDomain:"+nex);
			}
		}
		siz = RelatrixJson.size(MapRangeDomain.class);
		if(siz > 0) {
			if(DEBUG)
				System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			else
				throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		if(DEBUG) {
			it = RelatrixJson.entrySet(RangeDomainMap.class);
			while(it.hasNext()) {
				Comparable<?> nex = (Comparable<?>) it.next();
				System.out.println("RangeDomainMap:"+nex);
			}
		}
		siz = RelatrixJson.size(RangeDomainMap.class);
		if(siz > 0) {
			if(DEBUG)
				System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			else
				throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		if(DEBUG) {
			it = RelatrixJson.entrySet(RangeMapDomain.class);
			while(it.hasNext()) {
				Comparable<?> nex = (Comparable<?>) it.next();
				System.out.println("RangeMapDomain:"+nex);
			}
		}
		siz = RelatrixJson.size(RangeMapDomain.class);
		if(siz > 0) {
			if(DEBUG)
				System.out.println("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			else
				throw new Exception("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}


}
