package com.neocoretechs.relatrix.test.kv.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.json.JSONObject;

import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.RelatrixJson;
import com.neocoretechs.relatrix.RelatrixKVJson;

import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;


/**
 * The set of tests verifies the lower level {@link DBKey} functions in the {@link  Relatrix}
 * NOTES:
 * A database unique to this test module should be used.
 * tablespace is specified via system cmdl property
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017
 *
 */
public class BatteryDBKey {
	public static boolean DEBUG = false;
	static String x = "{\"timestamp\":1779166000301,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x50k = "{\"timestamp\":1779166050000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x75k = "{\"timestamp\":1779166075000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";

	static int min = 0;
	static int max = 100000;
	static int numDelete = 100; // for delete test
	static int i;

	static ArrayList<Comparable> findkeys = new ArrayList<Comparable>();
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		RelatrixKVJson.getInstance();
		if(argv.length > 1 && argv[0].equals("max")) {
			System.out.println("Setting max items to "+argv[1]);
			max = Integer.parseInt(argv[1]);
		}
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				if(argv.length == 1 && argv[0].equals("init")) {
					System.out.println("Initialize database to zero items, then terminate...");
					battery1AR17();
					System.exit(0);
				}
				long siz = RelatrixJson.size();
				if(siz == 0) {
					if(DEBUG)
						System.out.println("Zero items, Begin insertion from "+min+" to "+max);
					battery1();
				}
				battery1AR4();
				battery1AR7();
				battery1AR8();
				battery1AR9();
			} catch(Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("BatteryDBKey TEST BATTERY COMPLETE.");

	}
	/**
	 * Loads up on keys, should be 0 to max-1, or min, to max -1
	 * Ensure that we start with known baseline number of keys
	 * @throws Exception
	 */
	public static void battery1() throws Exception {
		System.out.println("KV Battery1 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		JSONObject jo = new JSONObject(x);

		for(int i = min; i < max; i++) {
			try {
				RelatrixKVJson.store(jo, i);
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
	public static void battery1AR4() throws Exception {
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x);
		Class<?> c = RelatrixKVJson.getClassType(jo);
		long siz = RelatrixKVJson.size(c);
		// set up previous key as first key, insert to key map
		Object o =  RelatrixKVJson.firstKey(c);
		Comparable prev = (Comparable) o;
		System.out.println(o);
		Object p = RelatrixKVJson.lastKey(c);
		System.out.println(p);
		Iterator<?> its = RelatrixKVJson.findTailMapKV((Comparable<?>) o);
		System.out.println("KV Battery1AR4");
		while(its.hasNext()) {
			Map.Entry<Comparable,Object> nex = (Map.Entry<Comparable,Object>) its.next();
			findkeys.add(nex.getKey());
			if(((Comparable)o).compareTo(nex.getKey()) != 0 && nex.getKey().compareTo(prev) <= 0) { // should always be >
			// Map.Entry
				System.out.println("KV RANGE KEY MISMATCH: prev:"+prev+" nex:"+nex.getKey()+" cmpr:"+nex.getKey().compareTo(prev));
				throw new Exception("KV RANGE KEY MISMATCH: prev:"+prev+" nex:"+nex.getKey()+" cmpr:"+nex.getKey().compareTo(prev));
			}
			prev = nex.getKey();
		}
		if(findkeys.size() != siz) {
			System.out.println("KV SIZE MISMATCH "+siz+" to "+findkeys.size());
			throw new Exception("KV SIZE MISMATCH "+siz+" to "+findkeys.size());
		}
		if(((Comparable)o).compareTo(findkeys.get(0)) != 0 || ((Comparable)p).compareTo(findkeys.get(findkeys.size()-1)) != 0) {
			System.out.println("KEY 0 MISMATCH "+o+" to "+findkeys.get(0)+" or "+p+" to "+findkeys.get(findkeys.size()-1));
			throw new Exception("KEY 0 MISMATCH "+o+" to "+findkeys.get(0)+" or "+p+" to "+findkeys.get(findkeys.size()-1));
		}
		 System.out.println("BATTERY1AR4 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * Testing of Iterator and Stream submap and transformations
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR7() throws Exception {
		max = 25000;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x50k);
		JSONObject jo75k = new JSONObject(x75k);
		Comparable<?> jkey = RelatrixKVJson.getObject(jo);
		Comparable<?> jto = RelatrixKVJson.getObject(jo75k);
		Iterator<?> its = RelatrixKVJson.findSubMap(jkey, jto);
		Iterator<?> itst = RelatrixKVJson.getStringIterator(its);
		System.out.println("KV Battery1AR7");
		System.out.println("=============== String Submap Iterator ======================");
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== String Submap K/V Iterator ======================");
		i = 0;
		its = RelatrixKVJson.findSubMapKV(jkey, jto);
		itst = RelatrixKVJson.getStringMapIterator(its);
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Submap Iterator ======================");
		i = 0;
		its = RelatrixKVJson.findSubMap(jkey, jto);
		itst = RelatrixKVJson.getJsonIterator(its);
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== String Submap Stream ======================");
		i = 0;
		Stream<?> sts = RelatrixKVJson.findSubMapStream(jkey, jto);
		Stream<?> stst = RelatrixKVJson.getStringStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== String Submap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findSubMapKVStream(jkey, jto);
		stst = RelatrixKVJson.getStringMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Submap Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findSubMapStream(jkey, jto);
		stst = RelatrixKVJson.getJsonStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Submap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findSubMapKVStream(jkey, jto);
		stst = RelatrixKVJson.getJsonMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("KV BATTERY1AR7 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * Testing of Iterator and Stream headmap and transformations
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR8() throws Exception {
		max = 49699;
		i = 0;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x50k);
		Comparable<?> jkey = RelatrixKVJson.getObject(jo);
		Iterator<?> its = RelatrixKVJson.findHeadMap(jkey);
		Iterator<?> itst = RelatrixKVJson.getStringIterator(its);
		System.out.println("KV Battery1AR8");
		System.out.println("=============== String Headmap Iterator ======================");
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== String Headmap K/V Iterator ======================");
		i = 0;
		its = RelatrixKVJson.findHeadMapKV(jkey);
		itst = RelatrixKVJson.getStringMapIterator(its);
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Headmap Iterator ======================");
		i = 0;
		its = RelatrixKVJson.findHeadMap(jkey);
		itst = RelatrixKVJson.getJsonIterator(its);
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== String Headmap Stream ======================");
		i = 0;
		Stream<?> sts = RelatrixKVJson.findHeadMapStream(jkey);
		Stream<?> stst = RelatrixKVJson.getStringStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== String Headmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findHeadMapKVStream(jkey);
		stst = RelatrixKVJson.getStringMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Headmap Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findHeadMapStream(jkey);
		stst = RelatrixKVJson.getJsonStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Headmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findHeadMapKVStream(jkey);
		stst = RelatrixKVJson.getJsonMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("KV BATTERY1AR8 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	/**
	 * Testing of Iterator and Stream tailmap and transformations
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR9() throws Exception {
		max = 25301;
		i = 0;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x75k);
		Comparable<?> jkey = RelatrixKVJson.getObject(jo);
		Iterator<?> its = RelatrixKVJson.findTailMap(jkey);
		Iterator<?> itst = RelatrixKVJson.getStringIterator(its);
		System.out.println("KV Battery1AR9");
		System.out.println("=============== String Tailmap Iterator ======================");
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== String Tailmap K/V Iterator ======================");
		i = 0;
		its = RelatrixKVJson.findTailMapKV(jkey);
		itst = RelatrixKVJson.getStringMapIterator(its);
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Tailmap Iterator ======================");
		i = 0;
		its = RelatrixKVJson.findTailMap(jkey);
		itst = RelatrixKVJson.getJsonIterator(its);
		while(itst.hasNext()) {
			Object y = itst.next();
			++i;
			System.out.println(i+".) "+y);
		}
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== String Tailmap Stream ======================");
		i = 0;
		Stream<?> sts = RelatrixKVJson.findTailMapStream(jkey);
		Stream<?> stst = RelatrixKVJson.getStringStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== String Tailmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findTailMapKVStream(jkey);
		stst = RelatrixKVJson.getStringMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Tailmap Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findTailMapStream(jkey);
		stst = RelatrixKVJson.getJsonStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Tailmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJson.findTailMapKVStream(jkey);
		stst = RelatrixKVJson.getJsonMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("KV BATTERY1AR9 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17() throws Exception {
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x);
		Class<?> c = RelatrixKVJson.getClassType(jo);
		int j = min;
		long s = RelatrixKVJson.size(c);
		System.out.println("Cleaning DB "+c+" of "+s+" elements.");
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
