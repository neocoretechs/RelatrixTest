package com.neocoretechs.relatrix.test.kv.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

import com.neocoretechs.rocksack.iterator.Entry;

import com.neocoretechs.relatrix.client.json.RelatrixKVClientJson;

import com.neocoretechs.relatrix.key.DBKey;

import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.key.KeySet;
import com.neocoretechs.relatrix.key.PrimaryKeySet;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

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
	static ArrayList<KeySet> keys = new ArrayList<KeySet>();
	static ArrayList<KeySet> findkeys = new ArrayList<KeySet>();
	static String x =     "{\"timestamp\":1779166030000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\"}]}]}";
	static String x50k =  "{\"timestamp\":1779166050000,\"RightImage\":[{\"count\":0, \"affections\":[ {\"name\":\"alligator\"}]}]}";
	static String xfull = "{\"timestamp\":1779166070000,\"LeftImage\":[{ \"count\":1, \"erections\":[ { \"name\":\"toilet\"}]}]}";

	static JSONObject xf = new JSONObject(xfull);
	static JSONObject xo50 = new JSONObject(x50k);
	static JSONObject xo = new JSONObject(x);
	static Class<?> xfClass, xoClass, xo50Class;
	
	static RelatrixKVClientJson rc;
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		rc = new RelatrixKVClientJson(argv[0], Integer.parseInt(argv[1]));
		//battery1AR17();
		battery1();
		battery2();
		battery1AR4();
		battery1AR44();
		battery1AR5();
		battery1AR9();
		battery1AR10();
		battery1AR101();
		battery1AR12();
		battery1AR14();
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
		JSONObject jox = new JSONObject(x);
		JSONObject jo2 = new JSONObject(x50k);
		/*String className = RelatrixTypeSynthesizer.generateMorphicClassName(jox,RelatrixTypeSynthesizer.morphicClassPrefix);
       	byte[] b = JsonRecordClassGenerator.buildJsonRecordClassBytes(className);   	
		HandlerClassLoader.setBytesInRepository(className, b);*/
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
			pks.setDomainKey(resolver.getIndexInstanceTable().getKey(jox.toString()));
			pks.setMapKey(resolver.getIndexInstanceTable().getKey(jo2.toString()));
			// check for domain/map match
			// Enforce categorical structure; domain->map function uniquely determines range.
			// If the search winds up at the key or the key is empty or the domain->map exists, the key
			// cannot be inserted
			//if(Relatrix.isPrimaryKey(RelatrixKV.nearest(identity), identity)) {
			if(DBKey.isValid(pks.getDomainKey()) && DBKey.isValid(pks.getMapKey()) && rc.get(pks) != null) {
				//throw new DuplicateKeyException("Duplicate key for relationship:"+identity);
				System.out.println("Duplicate key for relationship:"+pks);
				++dupes;
				continue;
			}
			KeySet identity = new KeySet();
			identity.setDomainKey(DBKey.newKey(resolver.getIndexInstanceTable(), jox.toString()));
			identity.setMapKey(DBKey.newKey(resolver.getIndexInstanceTable(), jo2.toString()));
			//identity.setRangeKey(DBKey.nullDBKey);
			identity.setRangeKey(DBKey.newKey(resolver.getIndexInstanceTable(),xf.toString())); // form it as template for duplicate key search
			// re-create it, now that we know its valid, in a form that stores the components with DBKeys
			// and maintains the classes stores in IndexInstanceTable for future commit.
			resolver.getIndexInstanceTable().put(identity);
			if( DEBUG  )
				System.out.println("Relatrix.store stored :"+identity);
			++recs;
		}	
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	
	private static void battery2() throws IllegalAccessException, ClassNotFoundException, IOException {
		JSONObject jox = new JSONObject(x);
		JSONObject jo2 = new JSONObject(x50k);
		IndexResolver resolver = null;
		if(ExecutionContextHolder.CONTEXT.isBound()) {
		        ParallelExecutionContext ctx = ExecutionContextHolder.CONTEXT.get();
		        resolver = ctx.resolver();
		} else
			throw new RuntimeException("IndexResolver not bound to context.");
		for(int i = min; i < max; i++) {
			long tim = jox.getLong("timestamp");
			++tim;
			jox.put("timestamp",tim);
			tim = jo2.getLong("timestamp");
			++tim;
			jo2.put("timestamp",tim);
			KeySet identity = new KeySet();
			identity.setDomainKey(resolver.getIndexInstanceTable().getKey(jox.toString()));
			identity.setMapKey(resolver.getIndexInstanceTable().getKey(jo2.toString()));
			identity.setRangeKey(DBKey.nullDBKey);
			//PrimaryKeySet pks = new PrimaryKeySet(identity);
			// check for domain/map match
			// Enforce categorical structure; domain->map function uniquely determines range.
			// If the search winds up at the key or the key is empty or the domain->map exists, the key
			// cannot be inserted
			//Object o = RelatrixKV.nearest(identity);
			//if(!Relatrix.isPrimaryKey(o, identity))
				//System.out.println("FAILED to find:"+identity+" found key="+o);
			Iterator<?> it = rc.findTailMapKV(identity);
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
	public static void battery1AR4() throws Exception {
		int cnt = 0;
		long tims = System.currentTimeMillis();
		KeySet prev = (KeySet) rc.firstKey(KeySet.class);
		Iterator<?> its = rc.findTailMapKV((Comparable) prev);
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
	public static void battery1AR44() throws Exception {
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
			if(rc.nearest(ident) == null) {
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
	public static void battery1AR5() throws Exception {
		int cnt = 0;
		Object i;
		long tims = System.currentTimeMillis();
		Iterator<?> its = rc.entrySet(KeySet.class);
		System.out.println("Battery1AR5");
		IndexResolver resolver = null;
		if(ExecutionContextHolder.CONTEXT.isBound()) {
		        ParallelExecutionContext ctx = ExecutionContextHolder.CONTEXT.get();
		        resolver = ctx.resolver();
		} else
			throw new RuntimeException("IndexResolver not bound to context.");
		while(its.hasNext()) {
			Entry nex = (Entry) its.next();
			i =  resolver.getIndexInstanceTable().get((DBKey) nex.getValue()); 
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
	public static void battery1AR9() throws Exception {
		int i = min;
		long tims = System.currentTimeMillis();
		Comparable k = (Comparable) rc.firstKey(KeySet.class); // first key
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
	public static void battery1AR10() throws Exception {
		int i = max-1;
		long tims = System.currentTimeMillis();
		Comparable k = (Comparable) rc.lastKey(KeySet.class); // key
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
	public static void battery1AR101() throws Exception {
		int i = max;
		long tims = System.currentTimeMillis();
		long bits = rc.size(KeySet.class);
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
	public static void battery1AR12() throws Exception {
		int cnt = 0;
		long tims = System.currentTimeMillis();
		Comparable c = (Comparable) rc.firstKey(KeySet.class);
		if( c != null ) {
			Iterator<?> its = rc.findTailMapKV(c);
			System.out.println("Battery1AR12");
			i = 0;
			IndexResolver resolver = null;
			if(ExecutionContextHolder.CONTEXT.isBound()) {
			        ParallelExecutionContext ctx = ExecutionContextHolder.CONTEXT.get();
			        resolver = ctx.resolver();
			} else
				throw new RuntimeException("IndexResolver not bound to context.");
			while(its.hasNext()) {
				Comparable nex = (Comparable) its.next();
				Map.Entry<KeySet, DBKey> nexe = (Map.Entry<KeySet,DBKey>)nex;
				DBKey db = resolver.getIndexInstanceTable().getKey(nexe.getKey()); // get the DBKey for this instance integer
				KeySet keyset = (KeySet) resolver.getIndexInstanceTable().get(nexe.getValue());
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
	public static void battery1AR14() throws Exception {
		int cnt = 0;
		long tims = System.currentTimeMillis();
		Comparable c = (Comparable) rc.lastKey(KeySet.class);
		if(c != null) {
			Iterator<?> its = rc.findHeadMapKV(c);
			System.out.println("Battery1AR14");
			IndexResolver resolver = null;
			if(ExecutionContextHolder.CONTEXT.isBound()) {
			        ParallelExecutionContext ctx = ExecutionContextHolder.CONTEXT.get();
			        resolver = ctx.resolver();
			} else
				throw new RuntimeException("IndexResolver not bound to context.");
			while(its.hasNext()) {
				Comparable nex = (Comparable) its.next();
				Map.Entry<KeySet,DBKey> nexe = (Map.Entry<KeySet,DBKey>)nex;
				DBKey db = resolver.getIndexInstanceTable().getKey(nexe.getKey()); // get the DBKey for this instance 
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
