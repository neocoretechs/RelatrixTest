package com.neocoretechs.relatrix.test.kv;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.neocoretechs.rocksack.Alias;
import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.RelatrixKV;
import com.neocoretechs.relatrix.RelatrixKVTransaction;
import com.neocoretechs.relatrix.key.DBKey;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

/**
 * Test of embedded KV server stream retrieval ops.
 * NOTE: rather than a database, specify only the PATH for the series of databases that will be 
 * designated ALIAS1java.lang.String, ALIAS2java.lang.String and ALIAS3java.lang.String<p>
 * The static constant fields in the class control the key generation for the tests
 * In general, the keys and values are formatted according to uniqKeyFmt to produce
 * a series of canonically correct sort order strings for the DB in the range of min to max vals
 * In general most of the testing relies on checking order against expected values hence the importance of
 * canonical ordering in the sample strings.
 * Of course, you can substitute any class for the Strings here providing its Comparable.
 * This tests the Java 8 streams obtained from the server
 * NOTES:
 * The database aliases define db names from tablespace, alias is prepended for fully qualified tablespace names
 * C:/users/you/Relatrix should be valid path as program arg. C:/users/you/Relatrix/ALIAS1java.lang.String through ALIAS3... will be created.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2023
 *
 */
public class BatteryRelatrixKVStreamAlias {
	public static boolean DEBUG = false;

	static String uniqKeyFmt = "%0100d"; // base + counter formatted with this gives equal length strings for canonical ordering
	static int min = 0;
	static int max = 100000;
	static int numDelete = 100; // for delete test
	static int numLookupByValue = 10; // lookup by value quite slow
	static int i;
	static int j;
	static Alias alias1 = new Alias("ALIAS1");
	static Alias alias2 = new Alias("ALIAS2");
	static Alias alias3 = new Alias("ALIAS3");
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		RelatrixKV.getInstance();
		RelatrixKV.setAlias(alias1,RelatrixKV.getTableSpace()+alias1);
		RelatrixKV.setAlias(alias2,RelatrixKV.getTableSpace()+alias2);
		RelatrixKV.setAlias(alias3,RelatrixKV.getTableSpace()+alias3);
		RelatrixKV.getInstance();
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				if(argv.length > 1 && argv[0].equals("max")) {
					System.out.println("Setting max items to "+argv[1]);
					max = Integer.parseInt(argv[1]);
				} else {
					if(argv.length > 0 && argv[0].equals("init") ) {
						System.out.println("Initialize database to zero items");
						battery1AR17(alias1);
						battery1AR17(alias2);
						battery1AR17(alias3);
						battery18();
					}
				}
		battery1();	// build and store
		battery1AR6();
		battery1AR7();
		battery1AR11();
		battery1AR12();
		battery1AR13();
		battery1AR14();
		battery1AR15();
		battery1AR16();
		battery18();
			} catch(Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("BatteryRelatrixKVStreamAlias TEST BATTERY COMPLETE.");
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
		String fkey = null;
		int j = min;
		j = (int) RelatrixKV.size(alias1, String.class);
		if(j > 0) {
			System.out.println("Cleaning "+alias1+" "+RelatrixKV.getAlias(alias1)+" of "+j+" elements.");
			battery1AR17(alias1);		
		}
		for(int i = min; i < max; i++) {
			fkey = String.format(uniqKeyFmt, i);
			try {
				RelatrixKV.store(alias1, fkey+alias1, Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		//
		j = (int) RelatrixKV.size(alias2, String.class);
		if(j > 0) {
			System.out.println("Cleaning "+alias2+" "+RelatrixKV.getAlias(alias2)+" of "+j+" elements.");
			battery1AR17(alias2);		
		}
		for(int i = min; i < max; i++) {
			fkey = String.format(uniqKeyFmt, i);
			try {
				RelatrixKV.store(alias2, fkey+alias2, Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		//
		j = (int) RelatrixKV.size(alias3, String.class);
		if(j > 0) {
			System.out.println("Cleaning "+alias3+" "+RelatrixKV.getAlias(alias3)+" of "+j+" elements.");
			battery1AR17(alias3);		
		}
		for(int i = min; i < max; i++) {
			fkey = String.format(uniqKeyFmt, i);
			try {
				RelatrixKV.store(alias3, fkey+alias3, Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		System.out.println("KV BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	
	/**
	 * Test the higher level functions in the RelatrixKV.
	 * public Set<Map.Entry<K,V>> entrySet()
	 * Returns a Set view of the mappings contained in this map. 
	 * The set's stream returns the entries in ascending key order. 
	 * The set is backed by the map, so changes to the map are reflected in the set, and vice-versa.
	 *  If the map is modified while an iteration over the set is in progress (except through the stream's 
	 *  own remove operation, or through the setValue operation on a map entry returned by the stream) the results
	 *   of the streaming are undefined. The set supports element removal, which removes the corresponding mapping from the map, 
	 *   via the stream. Remove, Set.remove, removeAll, retainAll and clear operations. 
	 *   It does not support the add or addAll operations.
	 *   from battery1 we should have 0 to max, say 1000 keys of length 100
	 * @throws Exception
	 */
	public static void battery1AR6() throws Exception {
		i = min;
		long tims = System.currentTimeMillis();
		System.out.println("KV Battery1AR6");
		RelatrixKV.entrySetStream(alias1, String.class).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias1.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR6 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR6 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.entrySetStream(alias2, String.class).sorted().collect(Collectors.toList()).forEach(e ->{	
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias2.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR6 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR6 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.entrySetStream(alias3, String.class).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias3.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR6 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR6 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR6 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Testing of Stream<?> its = RelatrixKV.keySet;
	 * @throws Exception
	 */
	public static void battery1AR7() throws Exception {
		i = min;
		long tims = System.currentTimeMillis();
		System.out.println("KV Battery1AR7");
		RelatrixKV.keySetStream(alias1, String.class).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias1.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.keySetStream(alias2, String.class).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias2.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.keySetStream(alias3, String.class).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias3.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR7 unexpected number of keys "+i);
		}
		 System.out.println("KV BATTERY1AR7 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * findMap test, basically tailmap returning keys
	 * @throws Exception
	 */
	public static void battery1AR11() throws Exception {
		long tims = System.currentTimeMillis();
		i = min;
		String fkey = String.format(uniqKeyFmt, i);
		System.out.println("KV Battery1AR11");
		RelatrixKV.findTailMapStream(alias1, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias1.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR11 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR11 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findTailMapStream(alias2, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias2.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR11 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR11 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findTailMapStream(alias3, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias3.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR11 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR11 unexpected number of keys "+i);
		}
		 System.out.println("BATTERY1AR11 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * findMapKV tailmapKV
	 * @throws Exception
	 */
	public static void battery1AR12() throws Exception {
		long tims = System.currentTimeMillis();
		i = min;
		String fkey = String.format(uniqKeyFmt, i);
		System.out.println("KV Battery1AR12");
		RelatrixKV.findTailMapKVStream(alias1, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias1.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR12 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR12 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findTailMapKVStream(alias2, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias2.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR12 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR12 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findTailMapKVStream(alias3, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias3.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR12 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR12 unexpected number of keys "+i);
		}
		 System.out.println("BATTERY1AR12 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	/**
	 * findMapKV findHeadMap - Returns a view of the portion of this map whose keys are strictly less than toKey.
	 * @throws Exception
	 */
	public static void battery1AR13() throws Exception {
		long tims = System.currentTimeMillis();
		i = max;
		String fkey = String.format(uniqKeyFmt, i);
		System.out.println("KV Battery1AR13");
		// with i at max, should catch them all
		i = min;
		RelatrixKV.findHeadMapStream(alias1,fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias1.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR13 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR13 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findHeadMapStream(alias2,fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias2.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR13 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR13 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findHeadMapStream(alias3,fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias3.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR13 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR13 unexpected number of keys "+i);
		}
		 System.out.println("BATTERY1AR13 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	/**
	 * findHeadMapKV
	 * @throws Exception
	 */
	public static void battery1AR14() throws Exception {
		long tims = System.currentTimeMillis();
		i = max;
		String fkey = String.format(uniqKeyFmt, i);
		System.out.println("KV Battery1AR14");
		i = min;
		RelatrixKV.findHeadMapKVStream(alias1, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias1.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR14 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR14 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findHeadMapKVStream(alias2, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias2.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR14 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR14 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findHeadMapKVStream(alias3, fkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias3.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR14 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR14 unexpected number of keys "+i);
		}
		 System.out.println("BATTERY1AR14 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	/**
	 * findSubMap findSubMap - Returns a view of the portion of this map whose keys range from fromKey, inclusive, to toKey, exclusive.
	 * @throws Exception
	 */
	public static void battery1AR15() throws Exception {
		long tims = System.currentTimeMillis();
		i = min;
		j = max;
		String fkey = String.format(uniqKeyFmt, i);
		// with j at max, should get them all since we stored to max -1
		String tkey = String.format(uniqKeyFmt, j);
		System.out.println("KV Battery1AR15");
		// with i at max, should catch them all
		RelatrixKV.findSubMapStream(alias1, fkey, tkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias1.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR15 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR15 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findSubMapStream(alias2, fkey, tkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias2.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR15 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR15 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findSubMapStream(alias3, fkey, tkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(!((String)e).endsWith(alias3.getAlias()) || Integer.parseInt(((String)e).substring(0,100)) != i	) {
				System.out.println("KV RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("KV BATTERY1AR15 unexpected number of keys "+i);
			throw new Exception("KV BATTERY1AR15 unexpected number of keys "+i);
		}
		 System.out.println("BATTERY1AR15 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	/**
	 * findSubMap findSubMapKV - Returns a view of the portion of this map whose keys range from fromKey, inclusive, to toKey, exclusive.
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR16() throws Exception {
		long tims = System.currentTimeMillis();
		i = min;
		j = max;
		String fkey = String.format(uniqKeyFmt, i);
		// with j at max, should get them all since we stored to max -1
		String tkey = String.format(uniqKeyFmt, j);
		System.out.println("KV Battery1AR16");
		// with i at max, should catch them all
		RelatrixKV.findSubMapKVStream(alias1, fkey, tkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias1.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR16 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR16 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findSubMapKVStream(alias2, fkey, tkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias2.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR16 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR16 unexpected number of keys "+i);
		}
		i = min;
		RelatrixKV.findSubMapKVStream(alias3, fkey, tkey).sorted().collect(Collectors.toList()).forEach(e ->{
			if(((Map.Entry<String,Long>)e).getValue() != i || !((Map.Entry<String,Long>)e).getKey().endsWith(alias3.getAlias()) ||
					Integer.parseInt(((Map.Entry<String,Long>)e).getKey().substring(0,100)) != i	) {
				System.out.println("RANGE KEY MISMATCH:"+i+" - "+e);
			} else
				++i;
		});
		if( i != max ) {
			System.out.println("BATTERY1AR16 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR16 unexpected number of keys "+i);
		}
		 System.out.println("BATTERY1AR16 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	public static void battery1AR17(Alias alias) throws Exception {
		long tims = System.currentTimeMillis();
		int j = min;
		long s = RelatrixKV.size(alias,String.class);
		System.out.println("Cleaning "+alias+" DB of "+s+" elements.");
		Iterator<?> it = RelatrixKV.entrySet(alias,String.class);
		long timx = System.currentTimeMillis();
		for(int i = 0; i < s; i++) {
			Map.Entry<String, Object> mkey = (Map.Entry<String, Object>) it.next();
			RelatrixKV.remove(alias,mkey.getKey());
			if((System.currentTimeMillis()-timx) > 5000) {
				System.out.println("DBKey "+i+" "+mkey);
				timx = System.currentTimeMillis();
			}
		}
		 System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Loads up on keys, should be 0 to max-1, or min, to max -1
	 * @param argv
	 * @throws Exception
	 */
	public static void battery18() throws Exception {
		System.out.println("KV Battery18 ");
		int max1 = (int) (max - (RelatrixKV.size(Long.class)/2));
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		for(int i = min; i < max1; i++) {
			fkey = String.format(uniqKeyFmt, i);
			try {
				RelatrixKV.store(alias1, fkey+alias1, Long.valueOf(i));
				RelatrixKV.store(alias2, fkey+alias2, Long.valueOf(i));
				RelatrixKV.store(alias3, fkey+alias3, Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		long s = RelatrixKV.size(alias1, String.class);
		if(s != max1)
			System.out.println("Size at halway point of restore incorrect:"+s+" should be "+max1);
		for(int i = max1; i < max; i++) {
			fkey = String.format(uniqKeyFmt, i);
			try {
				RelatrixKV.store(alias1, fkey+alias1, Long.valueOf(i));
				RelatrixKV.store(alias2, fkey+alias2, Long.valueOf(i));
				RelatrixKV.store(alias3, fkey+alias3, Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		System.out.println("KV BATTERY18 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records in 3 alias, rejected "+dupes+" dupes.");
	}

}
