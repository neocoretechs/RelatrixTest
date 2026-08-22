package com.neocoretechs.relatrix.test.kv.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.json.JSONObject;

import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.RelatrixKVJsonTransaction;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;
import com.neocoretechs.relatrix.parallel.SynchronizedThreadManager;
import com.neocoretechs.rocksack.TransactionId;


/**
 * The set of tests verifies the lower level {@link DBKey} functions in the {@link  Relatrix}
 * NOTES:
 * A database unique to this test module should be used.
 * tablespace is specified via system cmdl property
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017
 *
 */
public class BatteryDBKeyTx {
	public static boolean DEBUG = false;
	static String x = "{\"timestamp\":1779166000301,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x50k = "{\"timestamp\":1779166050000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x75k = "{\"timestamp\":1779166075000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";

	static int min = 0;
	static int max = 100000;
	static int numDelete = 100; // for delete test
	static int i;
	static TransactionId xid;

	static ArrayList<Comparable> findkeys = new ArrayList<Comparable>();
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		RelatrixKVJsonTransaction.getInstance();
		IndexResolver indexResolver = new IndexResolver(true);
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		SynchronizedThreadManager.getInstance().spinWithContext(new Runnable() {
			@Override
			public void run() {
				try {
					xid = RelatrixKVJsonTransaction.getTransactionId();
					battery1AR17(argv);
					battery1(argv);
					battery1AR4(argv);
					battery1AR7(argv);
					battery1AR8(argv);
					battery1AR9(argv);
					RelatrixKVJsonTransaction.commit(xid);
					//battery1AR17(argv);
				} catch(Exception e) {
					e.printStackTrace();
				}
				System.out.println("BatteryDBKeyTxJson TEST BATTERY COMPLETE.");
				System.exit(0);
			}}, pec);
		SynchronizedThreadManager.startSupervisorThread();	
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
		JSONObject jo = new JSONObject(x);

		for(int i = min; i < max; i++) {
			try {
				RelatrixKVJsonTransaction.store(xid, jo, i);
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
		JSONObject jo = new JSONObject(x);
		Class<?> c = RelatrixKVJsonTransaction.getClassType(jo, xid);
		long siz = RelatrixKVJsonTransaction.size(xid, c);
		// set up previous key as first key, insert to key map
		Object o =  RelatrixKVJsonTransaction.firstKey(xid,c);
		Comparable prev = (Comparable)o;
		System.out.println("first key:"+RelatrixKVJsonTransaction.getData((Comparable) o));
		Object p = RelatrixKVJsonTransaction.lastKey(xid, c);
		Iterator<?> its = RelatrixKVJsonTransaction.findTailMapKV(xid, o);
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
	public static void battery1AR7(String[] argv) throws Exception {
		max = 25000;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x50k);
		JSONObject jo75k = new JSONObject(x75k);
		Iterator<?> its = RelatrixKVJsonTransaction.findSubMap(xid, jo, jo75k);
		Iterator<?> itst = RelatrixKVJsonTransaction.getStringIterator(its);
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
		its = RelatrixKVJsonTransaction.findSubMapKV(xid, jo, jo75k);
		itst = RelatrixKVJsonTransaction.getStringMapIterator(its);
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
		its = RelatrixKVJsonTransaction.findSubMap(xid, jo, jo75k);
		itst = RelatrixKVJsonTransaction.getJsonIterator(its);
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
		Stream<?> sts = RelatrixKVJsonTransaction.findSubMapStream(xid, jo, jo75k);
		Stream<?> stst = RelatrixKVJsonTransaction.getStringStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== String Submap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findSubMapKVStream(xid, jo, jo75k);
		stst = RelatrixKVJsonTransaction.getStringMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Submap Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findSubMapStream(xid, jo, jo75k);
		stst = RelatrixKVJsonTransaction.getJsonStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Submap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findSubMapKVStream(xid, jo, jo75k);
		stst = RelatrixKVJsonTransaction.getJsonMapStream(sts);
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
	public static void battery1AR8(String[] argv) throws Exception {
		max = 49699;
		i = 0;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x50k);
		Iterator<?> its = RelatrixKVJsonTransaction.findHeadMap(xid, jo);
		Iterator<?> itst = RelatrixKVJsonTransaction.getStringIterator(its);
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
		its = RelatrixKVJsonTransaction.findHeadMapKV(xid,jo);
		itst = RelatrixKVJsonTransaction.getStringMapIterator(its);
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
		its = RelatrixKVJsonTransaction.findHeadMap(xid, jo);
		itst = RelatrixKVJsonTransaction.getJsonIterator(its);
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
		Stream<?> sts = RelatrixKVJsonTransaction.findHeadMapStream(xid, jo);
		Stream<?> stst = RelatrixKVJsonTransaction.getStringStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== String Headmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findHeadMapKVStream(xid, jo);
		stst = RelatrixKVJsonTransaction.getStringMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Headmap Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findHeadMapStream(xid, jo);
		stst = RelatrixKVJsonTransaction.getJsonStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR8 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR8 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Headmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findHeadMapKVStream(xid, jo);
		stst = RelatrixKVJsonTransaction.getJsonMapStream(sts);
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
	public static void battery1AR9(String[] argv) throws Exception {
		max = 25301;
		i = 0;
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x75k);
		Iterator<?> its = RelatrixKVJsonTransaction.findTailMap(xid, jo);
		Iterator<?> itst = RelatrixKVJsonTransaction.getStringIterator(its);
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
		its = RelatrixKVJsonTransaction.findTailMapKV(xid, jo);
		itst = RelatrixKVJsonTransaction.getStringMapIterator(its);
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
		its = RelatrixKVJsonTransaction.findTailMap(xid, jo);
		itst = RelatrixKVJsonTransaction.getJsonIterator(its);
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
		Stream<?> sts = RelatrixKVJsonTransaction.findTailMapStream(xid, jo);
		Stream<?> stst = RelatrixKVJsonTransaction.getStringStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== String Tailmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findTailMapKVStream(xid, jo);
		stst = RelatrixKVJsonTransaction.getStringMapStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Tailmap Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findTailMapStream(xid, jo);
		stst = RelatrixKVJsonTransaction.getJsonStream(sts);
		stst.forEachOrdered(e-> {++i;System.out.println(i+".) "+e);});
		if( i != max ) {
			System.out.println("KV BATTERY1AR9 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR9 unexpected number of keys "+i);
		}
		System.out.println("=============== JSONObject Tailmap K/V Stream ======================");
		i = 0;
		sts = RelatrixKVJsonTransaction.findTailMapKVStream(xid, jo);
		stst = RelatrixKVJsonTransaction.getJsonMapStream(sts);
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
	public static void battery1AR17(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		JSONObject jo = new JSONObject(x);
		Class<?> c = RelatrixKVJsonTransaction.getClassType(jo, xid);
		long s = RelatrixKVJsonTransaction.size(xid, c);
		System.out.println("Cleaning DB "+c+" of "+s+" elements.");
		Iterator<?> it = RelatrixKVJsonTransaction.keySet(xid, c);
		long timx = System.currentTimeMillis();
		int i = 0;
		while(it.hasNext()) {	
			++i;
			Object fkey = it.next();
			System.out.println(i+".) "+fkey);
			RelatrixKVJsonTransaction.remove(xid, fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("Key "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
	
		long siz = RelatrixKVJsonTransaction.size(xid, c);
		if(siz > 0) {
			Iterator<?> its = RelatrixKVJsonTransaction.entrySet(xid, c);
			while(its.hasNext()) {
				Comparable nex = (Comparable) its.next();
				//System.out.println(i+"="+nex);
				System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
			}
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		RelatrixKVJsonTransaction.commit(xid);
		 System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	
}
