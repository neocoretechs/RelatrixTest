package com.neocoretechs.relatrix.test.kv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.RelatrixKV;
import com.neocoretechs.relatrix.key.DBKey;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

/**
 * The set of tests verifies the lower level {@link DBKey} functions in the {@link  Relatrix}
 * NOTES:
 * A database unique to this test module should be used.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017
 *
 */
public class BatteryDBKey {
	public static boolean DEBUG = false;
	static DBKey dbkey;
	static int min = 0;
	static int max = 100000;
	static int numDelete = 100; // for delete test

	static ArrayList<DBKey> findkeys = new ArrayList<DBKey>();
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		IndexResolver indexResolver = new IndexResolver();
		indexResolver.setLocal();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				RelatrixKV.getInstance();
				battery1AR17();
				battery1();
				battery1AR4();
				battery1AR7();
				battery1AR17();
				System.out.println("BatteryDBKey TEST BATTERY COMPLETE.");
			} catch(Exception e) {
				e.printStackTrace();
			}
		});
	}
	/**
	 * Loads up on keys, should be 0 to max-1, or min, to max -1
	 * Ensure that we start with known baseline number of keys
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1() throws Exception {
		System.out.println("KV Battery1 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		DBKey fkey = null;
		//Integer payload = 0;
		IndexResolver resolver = null;
		if(ExecutionContextHolder.CONTEXT.isBound()) {
		        ParallelExecutionContext ctx = ExecutionContextHolder.CONTEXT.get();
		        resolver = ctx.resolver();
		} else
			throw new RuntimeException("IndexResolver not bound to context.");
		for(int i = min; i < max; i++) {
			//try {
			Relation r = new Relation(i,i,i);
			fkey = DBKey.newKey(resolver.getIndexInstanceTable(), r); // puts to index and instance
			RelatrixKV.store(fkey, new MapRangeDomain(r));
			++recs;
			//} catch(DuplicateKeyException dke) { ++dupes; }
		}
		System.out.println("KV BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	/**
	 * check order of DBKey
	 * @throws Exception
	 */
	public static void battery1AR4() throws Exception {
		long tims = System.currentTimeMillis();
		DBKey prev = null;
		Iterator<?> its = RelatrixKV.findTailMapKV((Comparable) RelatrixKV.firstKey(DBKey.class));
		System.out.println("KV Battery1AR4");
		// set up previous key as first key, insert to key map
		prev = (DBKey) RelatrixKV.firstKey(DBKey.class);
		Comparable nex = (Comparable) its.next();
		Map.Entry<DBKey, Relation> nexe = (Map.Entry<DBKey,Relation>)nex;
		findkeys.add(nexe.getKey());
		while(its.hasNext()) {
			nex = (Comparable) its.next();
			nexe = (Map.Entry<DBKey,Relation>)nex;
			if(nexe.getKey().compareTo(prev) <= 0) { // should always be >
			// Map.Entry
				System.out.println("KV RANGE KEY MISMATCH: prev:"+prev+" nex:"+nexe.getKey()+" cmpr:"+nexe.getKey().compareTo(prev));
				throw new Exception("KV RANGE KEY MISMATCH: prev:"+prev+" nex:"+nexe.getKey()+" cmpr:"+nexe.getKey().compareTo(prev));
			}
			prev = nexe.getKey();
			findkeys.add(nexe.getKey());
		}
		 System.out.println("BATTERY1AR4 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * Testing of Iterator<?> its = RelatrixKV.keySet;
	 * @throws Exception
	 */
	public static void battery1AR7() throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		Iterator<?> its = RelatrixKV.keySet(Relation.class);
		System.out.println("KV Battery1AR7");
		while(its.hasNext()) {
			Relation nex = (Relation) its.next();
			// Map.Entry
			//if(Integer.parseInt(nex) != i)
				//System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+nex);
			//else
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
	 * @throws Exception
	 */
	public static void battery1AR17() throws Exception {
		long tims = System.currentTimeMillis();
		int j = min;
		long s = RelatrixKV.size(DBKey.class);
		System.out.println("Cleaning DB of "+s+" elements.");
		Iterator<?> it = RelatrixKV.keySet(DBKey.class);
		long timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			RelatrixKV.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("DBKey "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		// remove payload reverse index
		s = RelatrixKV.size(Relation.class);
		it = RelatrixKV.keySet(Relation.class);
		timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			RelatrixKV.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println(fkey.getClass().getName()+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		s = RelatrixKV.size(MapRangeDomain.class);
		it = RelatrixKV.keySet(MapRangeDomain.class);
		timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			RelatrixKV.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println(fkey.getClass().getName()+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		long siz = RelatrixKV.size(DBKey.class);
		if(siz > 0) {
			Iterator<?> its = RelatrixKV.entrySet(DBKey.class);
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
