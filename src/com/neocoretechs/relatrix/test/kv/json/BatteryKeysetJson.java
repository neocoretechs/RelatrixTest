package com.neocoretechs.relatrix.test.kv.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

import com.neocoretechs.rocksack.iterator.Entry;

import com.neocoretechs.relatrix.RelatrixKVJson;
import com.neocoretechs.relatrix.key.DBKey;
import com.neocoretechs.relatrix.key.IndexInstanceTableJson;
import com.neocoretechs.relatrix.key.IndexInstanceTableInterface;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.key.KeySet;
import com.neocoretechs.relatrix.key.PrimaryKeySet;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

/**
 * The set of tests verifies the lower level {@link KeySet} functions in the {@link  RelatrixJson}
 * NOTES:
 * A database unique to this test module should be used.
 * tablespace is specified via system cmdl property
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017,2026
 *
 */
public class BatteryKeysetJson {
	public static boolean DEBUG = false;
	static KeySet keyset;
	static int i;
	static int min = 0;
	static int max = 10000;
	static int numDelete = 100; // for delete test
	static ArrayList<KeySet> keys = new ArrayList<KeySet>();
	static ArrayList<KeySet> findkeys = new ArrayList<KeySet>();
	static IndexInstanceTableInterface indexTable = new IndexInstanceTableJson();
	static String x =     "{\"timestamp\":1779166030000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\"}]}]}";
	static String x50k =  "{\"timestamp\":1779166050000,\"RightImage\":[{\"count\":0, \"affections\":[ {\"name\":\"alligator\"}]}]}";
	static String xfull = "{\"timestamp\":1779166070000,\"LeftImage\":[{ \"count\":1, \"erections\":[ { \"name\":\"toilet\"}]}]}";

	static JSONObject xf = new JSONObject(xfull);
	static JSONObject xo50 = new JSONObject(x50k);
	static JSONObject xo = new JSONObject(x);
	static Class<?> xfClass, xoClass, xo50Class;
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		RelatrixKVJson.getInstance();
		//battery1AR17(argv);
		battery1(argv);
		battery2(argv);
		battery1AR4(argv);
		battery1AR44(argv);
		battery1AR5(argv);
		battery1AR9(argv);
		battery1AR10(argv);
		battery1AR101(argv);
		battery1AR12(argv);
		battery1AR14(argv);
		//battery1AR17(argv);
		 System.out.println("BatteryKeysetJson TEST BATTERY COMPLETE.");
		
	}
	/**
	 * Loads up on keys, should be 0 to max-1, or min, to max -1
	 * Ensure that we start with known baseline number of keys
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1(String[] argv) throws Exception {
		System.out.println("Battery1 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		JSONObject jox = new JSONObject(x);
		JSONObject jo2 = new JSONObject(x50k);
		IndexResolver resolver = null;
		if(ExecutionContextHolder.CONTEXT.isBound()) {
		        ParallelExecutionContext ctx = ExecutionContextHolder.CONTEXT.get();
		        resolver = ctx.resolver();
		} else
			throw new RuntimeException("IndexResolver not bound to context.");
		for(i = min; i < max; i++) {
			long tim = jox.getLong("timestamp");
			++tim;
			jox.put("timestamp",tim);
			tim = jo2.getLong("timestamp");
			++tim;
			jo2.put("timestamp",tim);
			PrimaryKeySet pks = new PrimaryKeySet();
			pks.setDomainKey(resolver.getIndexInstanceTable().getKey(jox));
			pks.setMapKey(resolver.getIndexInstanceTable().getKey(jo2));
			// check for domain/map match
			// Enforce categorical structure; domain->map function uniquely determines range.
			// If the search winds up at the key or the key is empty or the domain->map exists, the key
			// cannot be inserted
			//if(Relatrix.isPrimaryKey(RelatrixKV.nearest(identity), identity)) {
			if(DBKey.isValid(pks.getDomainKey()) && DBKey.isValid(pks.getMapKey()) && RelatrixKVJson.get(pks) != null) {
				//throw new DuplicateKeyException("Duplicate key for relationship:"+identity);
				System.out.println("Duplicate key for relationship:"+pks);
				++dupes;
				continue;
			}
			KeySet identity = new KeySet();
			identity.setDomainKey(DBKey.newKey(indexTable, jox));
			identity.setMapKey(DBKey.newKey(indexTable, jo2));
			//identity.setRangeKey(DBKey.nullDBKey);
			identity.setRangeKey(DBKey.newKey(indexTable,xf)); // form it as template for duplicate key search
			// re-create it, now that we know its valid, in a form that stores the components with DBKeys
			// and maintains the classes stores in IndexInstanceTable for future commit.
			resolver.getIndexInstanceTable().put(identity);
			if( DEBUG  )
				System.out.println("Relatrix.store stored :"+identity);
			++recs;
		}	
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	
	private static void battery2(String[] argv) throws IllegalAccessException, ClassNotFoundException, IOException {
		JSONObject jox = new JSONObject(x);
		JSONObject jo2 = new JSONObject(x50k);
		for(int i = min; i < max; i++) {
			long tim = jox.getLong("timestamp");
			++tim;
			jox.put("timestamp",tim);
			tim = jo2.getLong("timestamp");
			++tim;
			jo2.put("timestamp",tim);
			KeySet identity = new KeySet();
			identity.setDomainKey(indexTable.getKey(jox));
			identity.setMapKey(indexTable.getKey(jo2));
			identity.setRangeKey(DBKey.nullDBKey);
			//PrimaryKeySet pks = new PrimaryKeySet(identity);
			// check for domain/map match
			// Enforce categorical structure; domain->map function uniquely determines range.
			// If the search winds up at the key or the key is empty or the domain->map exists, the key
			// cannot be inserted
			//Object o = RelatrixKV.nearest(identity);
			//if(!Relatrix.isPrimaryKey(o, identity))
				//System.out.println("FAILED to find:"+identity+" found key="+o);
			Iterator<?> it = RelatrixKVJson.findTailMapKV(identity);
			int cnt = 0;
			while(it.hasNext()) {
				Object o = it.next();
				Map.Entry e = (Map.Entry)o;
				KeySet k = ((KeySet)e.getKey());
				if(k.domainKeyEquals(identity) && k.mapKeyEquals(identity)) {
					if(DEBUG)
						System.out.println("Found at "+cnt);
					break;
				}
				cnt++;
			}
		}
	}
	/**
	 * check order of DBKey
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR4(String[] argv) throws Exception {
		int cnt = 0;
		long tims = System.currentTimeMillis();
		KeySet prev = (KeySet) RelatrixKVJson.firstKey(KeySet.class);
		Iterator<?> its = RelatrixKVJson.findTailMapKV((Comparable) prev);
		System.out.println("Battery1AR4");
		KeySet first = ((Map.Entry<KeySet,DBKey>)its.next()).getKey();
		findkeys.add(first); // skip first key we just got
		keys.add(first);
		while(its.hasNext()) {
			Comparable nex = (Comparable) its.next();
			Map.Entry<KeySet, DBKey> nexe = (Map.Entry<KeySet,DBKey>)nex;
			if(nexe.getKey().compareTo(prev) <= 0) { // should always be >
			// Map.Entry
				System.out.println("RANGE KEY MISMATCH: "+nex);
				throw new Exception("RANGE KEY MISMATCH: "+nex);
			}
			prev = nexe.getKey();
			findkeys.add(nexe.getKey());
			keys.add(nexe.getKey());
			if(DEBUG)
				System.out.println("1AR4 "+(cnt++)+"="+nex);
		}
		 System.out.println("BATTERY1AR4 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	public static void battery1AR44(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("Battery1AR44");
		while(!findkeys.isEmpty()) {
			int rnd = new Random().nextInt(findkeys.size());
			KeySet ident = findkeys.get(rnd);
			findkeys.remove(rnd);

			// check for domain/map match
			// Enforce categorical structure; domain->map function uniquely determines range.
			// If the search winds up at the key or the key is empty or the domain->map exists, the key
			// cannot be inserted
			if(RelatrixKVJson.nearest(ident) == null) {
				if(DEBUG)
					System.out.println("Didnt find "+ident);
				else
					throw new Exception("Didnt find "+ident);
			} else {
				if(DEBUG)
					System.out.println("FOUND "+ident);
			}
			
		}
		 System.out.println("BATTERY1AR44 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Testing of Iterator<?> its = RelatrixKV.keySet;
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR5(String[] argv) throws Exception {
		int cnt = 0;
		Object i;
		long tims = System.currentTimeMillis();
		Iterator<?> its = RelatrixKVJson.entrySet(KeySet.class);
		System.out.println("Battery1AR5");
		while(its.hasNext()) {
			Entry nex = (Entry) its.next();
			i =  indexTable.get((DBKey) nex.getValue()); 
			if(((Comparable)i).compareTo(nex.getKey()) != 0) {
				System.out.println("RANGE KEY MISMATCH: "+nex);
				throw new Exception("RANGE KEY MISMATCH: "+nex);
			}
			if(DEBUG)
				System.out.println("1AR5 "+(cnt++)+"="+nex);
		}
		 System.out.println("BATTERY1AR5 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * 
	 * Testing of first(), and firstValue
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR9(String[] argv) throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		Comparable k = (Comparable) RelatrixKVJson.firstKey(KeySet.class); // first key
		((KeySet)k).getDomainKey();
		((KeySet)k).getMapKey();
		((KeySet)k).getRangeKey();
		System.out.println("Battery1AR9 firstKey");
		if(!keys.contains(k)) {
			System.out.println("BATTERY1A9 cant find contains key "+i);
			throw new Exception("BATTERY1AR9 unexpected cant find contains of key "+i);
		}
		System.out.println(k);
		System.out.println("BATTERY1AR9 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * test last and lastKey
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR10(String[] argv) throws Exception {
		int i = max-1;
		long tims = System.currentTimeMillis();
		Comparable k = (Comparable) RelatrixKVJson.lastKey(KeySet.class); // key
		((KeySet)k).getDomainKey();
		((KeySet)k).getMapKey();
		((KeySet)k).getRangeKey();
		System.out.println("Battery1AR10 lastKey");
		if(!keys.contains(k)) {
			System.out.println("BATTERY1AR10 cant find last key "+i);
			throw new Exception("BATTERY1AR10 unexpected cant find last of key "+i);
		}
		System.out.println(k);
		System.out.println("BATTERY1AR10 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	* test size
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR101(String[] argv) throws Exception {
		int i = max;
		long tims = System.currentTimeMillis();
		long bits = RelatrixKVJson.size(KeySet.class);
		System.out.println("Battery1AR101 Size="+bits);
		if( bits != keys.size() ) {
			System.out.println("BATTERY1AR101 size mismatch "+bits+" should be:"+i);
			throw new Exception("BATTERY1AR101 size mismatch "+bits+" should be "+i);
		}
		System.out.println("BATTERY1AR101 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * findMapKV tailmapKV
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR12(String[] argv) throws Exception {
		int cnt = 0;
		long tims = System.currentTimeMillis();
		Comparable c = (Comparable) RelatrixKVJson.firstKey(KeySet.class);
		if( c != null ) {
			Iterator<?> its = RelatrixKVJson.findTailMapKV(c);
			System.out.println("Battery1AR12");
			i = 0;
			while(its.hasNext()) {
				Comparable nex = (Comparable) its.next();
				Map.Entry<KeySet, DBKey> nexe = (Map.Entry<KeySet,DBKey>)nex;
				DBKey db = indexTable.getKey(nexe.getKey()); // get the DBKey for this instance integer
				KeySet keyset = (KeySet) indexTable.get(nexe.getValue());
				if(nexe.getKey().compareTo(keyset) != 0 || nexe.getValue().compareTo(db) != 0) {
					// Map.Entry
					System.out.println("RANGE KEY MISMATCH:"+nex);
					throw new Exception("RANGE KEY MISMATCH:"+nex);
				}
				if(DEBUG)
					System.out.println("1AR12 "+(cnt++)+"="+nexe);
			}
		}
		System.out.println("BATTERY1AR12 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	
	/**
	 * findHeadMapKV
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR14(String[] argv) throws Exception {
		int cnt = 0;
		long tims = System.currentTimeMillis();
		Comparable c = (Comparable) RelatrixKVJson.lastKey(KeySet.class);
		if(c != null) {
			Iterator<?> its = RelatrixKVJson.findHeadMapKV(c);
			System.out.println("Battery1AR14");
			while(its.hasNext()) {
				Comparable nex = (Comparable) its.next();
				Map.Entry<KeySet,DBKey> nexe = (Map.Entry<KeySet,DBKey>)nex;
				DBKey db = indexTable.getKey(nexe.getKey()); // get the DBKey for this instance 
				if(nexe.getValue().compareTo(db) != 0) {
					// Map.Entry
					System.out.println("RANGE KEY MISMATCH:"+nex);
					throw new Exception("RANGE KEY MISMATCH:"+nex);
				}
				if(DEBUG)
					System.out.println("1AR14 "+(cnt++)+"="+nexe);
			}
		}
		System.out.println("BATTERY1AR14 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	
	/**
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		long s = RelatrixKVJson.size(DBKey.class);
		System.out.println("Cleaning DB of "+s+" elements.");
		Iterator it = RelatrixKVJson.keySet(DBKey.class);
		long timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			RelatrixKVJson.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("DBKey remove "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		// remove payload reverse index
		s = RelatrixKVJson.size(KeySet.class);
		it = RelatrixKVJson.keySet(KeySet.class);
		timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			RelatrixKVJson.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("KeySet remove "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		s = RelatrixKVJson.size(String.class);
		it = RelatrixKVJson.keySet(String.class);
		timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Object fkey = it.next();
			RelatrixKVJson.remove((Comparable) fkey);
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("String remove "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		long siz = RelatrixKVJson.size(DBKey.class);
		if(siz > 0) {
			Iterator<?> its = RelatrixKVJson.entrySet(DBKey.class);
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
