package com.neocoretechs.relatrix.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.AbstractRelation.displayLevels;
import com.neocoretechs.relatrix.key.DBKey;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.key.PrimaryKeySet;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;
import com.neocoretechs.rocksack.Alias;
import com.neocoretechs.relatrix.Relatrix;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.type.RelationList;
/**
 * Stream version of BatteryRelatrixAlias.<p>
 * The set of tests verifies the higher level 'findSet' functions in the {@link Relatrix}, which can be used
 * as examples of Relatrix processing.
 * In general the tests compare the number of items retrieved 
 * against expected value since findSet retrieves items in no particular order.
 * We use a {@link ParallelExecutionContext}
 * to provide access to the {@link IndexResolver} for each thread created by the parallel streams which
 * requires one line of additional tooling after the various findStream calls. We also need to have AtomicInteger
 * counters to function properly in a parallel context.
 * NOTES:
 * program argument is [ [init] [max nnn] ]
 * a series of databases prefixed by ALIAS1, ALIAS2, ALIAS3 will be created
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2016,2017,2024
 *
 */
public class BatteryRelatrixStreamAlias {
	public static boolean DEBUG = false;
	static String key = "This is a test"; // holds the base random key string for tests
	static String val = "Of a Relatrix element!"; // holds base random value string
	static String uniqKeyFmt = "%0100d"; // base + counter formatted with this gives equal length strings for canonical ordering
	static AtomicInteger min = new AtomicInteger(0);
	static AtomicInteger max = new AtomicInteger(1000);
	static int numDelete = 100; // for delete test
	static AtomicInteger i = new AtomicInteger(0);
	private static long timx;
	static Alias alias1 = new Alias("ALIAS1");
	static Alias alias2 = new Alias("ALIAS2");
	static Alias alias3 = new Alias("ALIAS3");
	static RelationList res;
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				Relatrix.getInstance();
				AbstractRelation.displayLevel = displayLevels.VERBOSE;
				Relatrix.setAlias(alias1,Relatrix.getTableSpace()+alias1);
				Relatrix.setAlias(alias2,Relatrix.getTableSpace()+alias2);
				Relatrix.setAlias(alias3,Relatrix.getTableSpace()+alias3);
				System.out.println("BatteryRelatrixStreamAlias");
				if(argv.length > 0 && argv[0].equals("max")) {
					System.out.println("Setting max items to "+argv[1]);
					max = new AtomicInteger(Integer.parseInt(argv[1]));
				} else {
					if(argv.length > 0 && argv[0].equals("init")) {
						System.out.println("Initialize database to zero items, then terminate...");
						battery1AR17(alias1);
						battery1AR17(alias2);
						battery1AR17(alias3);
						System.exit(0);
					}
				}
				Relatrix.flushAndCompactDB(alias1,Relation.class);
				Relatrix.flushAndCompactDB(alias2,Relation.class);
				Relatrix.flushAndCompactDB(alias3,Relation.class);
				Relatrix.flushAndCompactDB(alias1,DBKey.class);
				Relatrix.flushAndCompactDB(alias2,DBKey.class);
				Relatrix.flushAndCompactDB(alias3,DBKey.class);
				Relatrix.flushAndCompactDB(alias1,PrimaryKeySet.class);
				Relatrix.flushAndCompactDB(alias2,PrimaryKeySet.class);
				Relatrix.flushAndCompactDB(alias3,PrimaryKeySet.class);
				Relatrix.flushAndCompactDB(alias1,Long.class);
				Relatrix.flushAndCompactDB(alias2,Long.class);
				Relatrix.flushAndCompactDB(alias3,Long.class);
				Relatrix.flushAndCompactDB(alias1,String.class);
				Relatrix.flushAndCompactDB(alias2,String.class);
				Relatrix.flushAndCompactDB(alias3,String.class);
				if(Relatrix.size(alias1) == 0) {
					if(DEBUG)
						System.out.println("Zero items, Begin insertion from "+min+" to "+max);
					battery1(alias1);
					battery1(alias2);
					battery1(alias3);
					if(DEBUG)
						System.out.println("Begin duplicate key rejection test from "+min+" to "+max);
					battery11(alias1);
					battery11(alias2);
					battery11(alias3);
				}
				
				if(DEBUG)
					System.out.println("Begin test battery 1AR6");
				battery1AR6(pec,alias1);
				battery1AR6(pec,alias2);
				battery1AR6(pec,alias3);
				if(DEBUG)
					System.out.println("Begin test battery 1AR6A");
				battery1AR6A(pec,alias1);
				battery1AR6A(pec,alias2);
				battery1AR6A(pec,alias3);
				if(DEBUG)
					System.out.println("Begin test battery 1AR6B");
				battery1AR6B(pec,alias1);
				battery1AR6B(pec,alias2);
				battery1AR6B(pec,alias3);
				if(DEBUG)
					System.out.println("Begin test battery 1AR7");
				battery1AR7(pec,alias1);
				battery1AR7(pec,alias2);
				battery1AR7(pec,alias3);
				if(DEBUG)
					System.out.println("Begin test battery 1AR8");
				battery1AR8(pec,alias1);
				battery1AR8(pec,alias2);
				battery1AR8(pec,alias3);
				if(DEBUG)
					System.out.println("Begin test battery 1AR9");
				battery1AR9(pec,alias1);
				battery1AR9(pec,alias2);
				battery1AR9(pec,alias3);
				
				if(DEBUG)
					System.out.println("Begin test battery 1AR10");
				battery1AR10(pec,alias1);
				battery1AR10(pec,alias2);
				battery1AR10(pec,alias3);
				if(DEBUG)
					System.out.println("Begin test battery 1AR101");
				battery1AR101(pec,alias1);
				battery1AR101(pec,alias2);
				battery1AR101(pec,alias3);
				if(DEBUG)
					System.out.println("Begin test battery 1AR11");
				battery1AR11(pec,alias1);
				battery1AR11(pec,alias2);
				battery1AR11(pec,alias3);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});	
		System.out.println("TEST BATTERY COMPLETE.");
		System.exit(0);
	}
	/**
	 * Loads up on keys
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1(Alias alias12) throws Exception {
		System.out.println(alias12+" Battery1 ");
		long tims = System.currentTimeMillis();
		long timt = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		for(int i = min.get(); i < max.get(); i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				Relatrix.store(alias12, fkey, "Has unit "+alias12, Long.valueOf(i));
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("storing "+recs+" "+fkey);
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
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery11(Alias alias12) throws Exception {
		System.out.println(alias12+" Battery11 ");
		long timt = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		for(int i = min.get(); i < max.get(); i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				Relation dmr = Relatrix.store(alias12, fkey, "Has unit "+alias12, Long.valueOf(99999));
				++recs;
				System.out.println("SHOULD NOT BE storing "+recs+" "+fkey+" dmr:"+dmr);
				//if((System.currentTimeMillis()-tims) > 1000) {
				//	System.out.println("storing "+recs+" "+fkey);
				//	tims = System.currentTimeMillis();
				//}
			} catch(DuplicateKeyException d) {++dupes;}
		}
		if( recs > 0) {
			throw new DuplicateKeyException("BATTERY11 FAIL, stored "+recs+" when zero should have been stored");
		} else {
			System.out.println("BATTERY11 SUCCESS in "+(System.currentTimeMillis()-timt)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
		}
	}
	
	/**
	 * Test the higher level functions in the Relatrix. Use the 'findStream' permutations to
	 * verify the previously inserted data
	 * @param pec 
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR6(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR6");
		Relatrix.findStream(alias12,'*', '*', '*').parallel().forEach(e->
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			Result nex = (Result)e;
			nex.get();
			// 3 question marks = dimension 3 in return array
			if( DEBUG ) System.out.println("1AR6:"+i+" "+nex);
			// no guarantee of ordering with unqualified findSet/findStream
			if((Long)((Relation)nex.get()).getRange() < min.get() || (Long)((Relation)nex.get()).getRange() > max.get()) {
				System.out.println("RANGE KEY MISMATCH:"+(i.get())+" range not between min and max:"+nex.get(2));
				throw new RuntimeException("RANGE KEY MISMATCH:"+(i.get())+" range not between min and max:"+nex.get(2));
			}
			if(!((String)((Relation)nex.get()).getDomain()).startsWith(key) || !((String)((Relation)nex.get()).getMap()).equals("Has unit "+alias12) || nex.length() != 1) {
				System.out.println("MAP KEY MISMATCH:"+(i)+" Has unit "+alias12+" - "+((String)((Relation)nex.get()).getMap())+" length:"+nex.length());
				throw new RuntimeException("MAP KEY MISMATCH:"+(i)+" Has unit "+alias12+" - "+((String)((Relation)nex.get()).getMap())+" length:"+nex.length());
			}
			i.getAndIncrement();
		}));
		if( i.get() != max.get() ) {
			System.out.println("BATTERY1AR6 unexpected number of keys "+i.get());
			throw new Exception("BATTERY1AR6 unexpected number of keys "+i.get());
		}
		System.out.println("BATTERY1AR6 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	public static void battery1AR6A(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR6A");
		Relatrix.findStream(alias12,'*', '*', '*').parallel().forEach(e->
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			Result nex = (Result)e;
			Relation r = (Relation) nex.get();
			// 3 question marks = dimension 3 in return array
			if( DEBUG ) System.out.println("1AR6A:"+i+" "+nex);
			// no guarantee of ordering with unqualified findSet/findStream
			if(!((String)r.getDomain()).startsWith(key) ) {
				System.out.println("DOMAIN KEY MISMATCH:"+(i.get())+":"+r.getDomain());
				throw new RuntimeException("DOMAIN KEY MISMATCH:"+(i.get())+":"+r.getDomain());
			}
			if((Long)r.getRange() < min.get() || (Long)r.getRange() > max.get()) {
				System.out.println("RANGE KEY MISMATCH:"+(i.get())+" range not between min and max:"+r.getRange());
				throw new RuntimeException("RANGE KEY MISMATCH:"+(i.get())+" range not between min and max:"+r.getRange());
			}
			if(!((String)r.getMap()).equals("Has unit "+alias12)) {
				System.out.println("MAP KEY MISMATCH:"+(i.get())+" Has unit "+alias12+" - "+r.getMap());
				throw new RuntimeException("MAP KEY MISMATCH:"+(i.get())+" Has unit "+alias12+" - "+r.getMap());
			}
			i.getAndIncrement();
		}));
		if( i.get() != max.get() ) {
			System.out.println("BATTERY1AR6A unexpected number of keys "+i.get());
			throw new Exception("BATTERY1AR6A unexpected number of keys "+i.get());
		}
		System.out.println("BATTERY1AR6A SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	public static void battery1AR6B(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR6B");
		ArrayList<Object> iq = new ArrayList<Object>();
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
		for(int j = i.get()+1000; j < i.get()+(max.get()/10); j++) {
			String fkey = key + String.format(uniqKeyFmt, j);
			iq.add(fkey);
		}
		res = (RelationList) Relatrix.findSetParallel(alias12, iq, '*', '*');
		});
		System.out.println((System.currentTimeMillis()-tims)+" ms. findSetParallel for "+iq.size());
		if(res.size() != iq.size())
			throw new Exception("Result set does not match query set for findSetParallel:"+res.size()+" vs query size:"+iq.size());
		for(Comparable r: res) {
			if(!iq.contains(((AbstractRelation)r).getDomain()))
				throw new Exception("Cannot find query item in result set:"+r);
		}
		System.out.println("BATTERY1AR6B SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Testing of Stream Relatrix.findStream('*', '*', '*');
	 * @param pec 
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR7(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR7");
		Relatrix.findStream(alias12, '*', '*', '*').parallel().forEach(e->
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			Result nex = (Result)e;
			// one '?' in findStream gives us one element returned
			if(DEBUG ) System.out.println("1AR7:"+i+" "+nex);
			// no guarantee of ordering with unqualified findSet/findStream
			if(!((String)((Relation)nex.get()).getDomain()).startsWith(key) || nex.length() != 1) {
				System.out.println("DOMAIN KEY MISMATCH:"+(i.get())+"  "+nex+" length:"+nex.length());
				throw new RuntimeException("DOMAIN KEY MISMATCH:"+(i.get())+"  "+nex+" length:"+nex.length());
			}
			i.getAndIncrement();
		}));
		if( i.get() != max.get()) {
			System.out.println("BATTERY1AR7 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR7 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR7 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Testing of Stream Relatrix.findStream('?', '?', '*');
	 * @param pec 
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR8(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR8");
		Relatrix.findStream(alias12, '*', '*', '*').parallel().forEach(e ->
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			Result nex = (Result)e;
			// two '?' in findStream gives use 2 element result, the domain and map
			if( DEBUG ) System.out.println("1AR8:"+i+" "+nex);
			//String skey = key + String.format(uniqKeyFmt, i);
			// no guarantee of ordering with unqualified findSet/findStream
			if(!((String)((Relation)nex.get()).getDomain()).startsWith(key) || !((String)((Relation)nex.get()).getMap()).equals("Has unit "+alias12) || nex.length() != 1) {
				System.out.println("KEY MISMATCH:"+(i)+" "+nex.get()+" Has unit "+alias12+" - "+nex.get()+" length:"+nex.length());
				throw new RuntimeException("KEY MISMATCH:"+(i)+" Has unit "+alias12+" - "+nex.get()+" length:"+nex.length());
			}
			i.getAndIncrement();
		}));
		if( i.get() != max.get() ) {
			System.out.println("BATTERY1AR8 unexpected number of keys "+i.get());
			throw new Exception("BATTERY1AR8 unexpected number of keys "+i.get());
		}
		System.out.println("BATTERY1AR8 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * 
	 * Testing of Relatrix.findStream('*', '*', '*');
	 * @param pec 
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR9(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR9");
		Relatrix.findStream(alias12, '*', '*', '*').parallel().forEach(e->
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			Result nex = (Result)e;
			// the returned array has 1 element, the identity AbstractRelation Relation
			if( DEBUG ) System.out.println("1AR9:"+i+" "+nex.get(0));
			//String skey = key + String.format(uniqKeyFmt, i);
			// no guarantee of ordering with unqualified findSet/findStream
			if(!((String) ((Relation)nex.get(0)).getDomain() ).startsWith(key) )
				throw new RuntimeException("DOMAIN KEY MISMATCH:"+(i)+" - "+nex.get(0));
			if(!((Relation)nex.get(0)).getMap().equals("Has unit "+alias12))
				throw new RuntimeException("MAP KEY MISMATCH:"+(i)+" Has unit "+alias12+" - "+nex.get(0));
			i.getAndIncrement();
		}));
		if( i.get() != max.get() ) {
			System.out.println("BATTERY1AR9 unexpected number of keys "+i.get());
			//throw new Exception("BATTERY1AR9 unexpected number of keys "+i);
		}
		 System.out.println("BATTERY1AR9 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * Relatrix.findSetStream(fkey, "Has unit", '*');
	 * Should return 1 element of which 'fkey' and "Has unit" are primary key
	 * @param pec 
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR10(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR10");
		String fkey = key + String.format(uniqKeyFmt, min.get());
		Relatrix.findStream(alias12, fkey, "Has unit "+alias12, '*').parallel().forEach(e-> 
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
		// return all identities with the given key for all ranges, should be 1
			// In this case, the set of identities of type Long that have stated domain and map should be returned
			// since we supply a fixed domain and map object with a wildcard range, we should get one element back; the identity
			Result nex = (Result)e;
			if( nex.length() != 1)
				throw new RuntimeException("RETURNED ARRAY TUPLE LENGTH INCORRECT, SHOULD BE 1, is "+nex.length());
			if(DEBUG) System.out.println("1AR10:"+i+" "+nex.get(0));
			//String skey = key + String.format(uniqKeyFmt, i);
			// no guarantee of ordering with unqualified findSet/findStream
			if(!((String) ((Relation)nex.get(0)).getDomain() ).startsWith(key) )
				throw new RuntimeException("DOMAIN KEY MISMATCH:"+(i)+" "+key+" - "+nex.get(0));
			if(!((Relation)nex.get(0)).getMap().equals("Has unit "+alias12))
				throw new RuntimeException("MAP KEY MISMATCH:"+(i)+" Has unit "+alias12+" - "+nex.get(0));
			i.getAndIncrement();
		}));
		if( i.get() != 1 ) {
			System.out.println("BATTERY1AR10 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR10 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR10 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * Negative assertion test
	 * Relatrix.findStream(fkey, "Has unit", Long.valueOf(max));
	 * Range value is max, so zero keys should be retrieved since we insert 0 to max-1
	 * @param pec 
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR101(ParallelExecutionContext pec, Alias alias12) throws Exception {
		i = new AtomicInteger(min.get());
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR101");
		String fkey = key + String.format(uniqKeyFmt, max.get());
		// Range value is max, so zero keys should be retrieved since we insert 0 to max-1
		Relatrix.findStream(alias12, fkey, "Has unit "+alias12, Long.valueOf(max.get())).parallel().forEach(e->
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			// In this case, the set of identities of type Long that have stated domain and map should be returned
			// since we supply a fixed domain and map object with a wildcard range, we should get one element back; the identity
			Result nex = (Result) e;
			if( nex.length() != 1)
				throw new RuntimeException("RETURNED ARRAY TUPLE LENGTH INCORRECT, SHOULD BE 1, is "+nex.length());
			if(DEBUG) System.out.println("1AR101:"+i+" "+nex.get(0));
			//String skey = key + String.format(uniqKeyFmt, i);
			// no guarantee of ordering with unqualified findSet/findStream
			if(!( (String)((Relation)nex.get(0)).getDomain() ).startsWith(key) )
				throw new RuntimeException("DOMAIN KEY MISMATCH:"+(i)+" "+key+" - "+nex.get(0));
			if(!((Relation)nex.get(0)).getMap().equals("Has unit "+alias12))
				throw new RuntimeException("MAP KEY MISMATCH:"+(i)+" Has unit "+alias12+" - "+nex.get(0));
			//Long unit = Long.valueOf(i);
			//if(!((Relation)nex[0]).getRange().equals(unit))
				//System.out.println("RANGE KEY MISMATCH:"+(i)+" "+i+" - "+nex[0]);
			i.getAndIncrement();
		}));
		if( i.get() != 0 ) {
			System.out.println("BATTERY1AR101 unexpected number of keys "+i);
			throw new Exception("BATTERY1AR101 unexpected number of keys "+i);
		}
		System.out.println("BATTERY1AR101 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	/**
	 * negative assertion test
	 * Relatrix.findStream(fkey, "Has time", '*');
	 * map is 'Has time', which we never inserted, so no elements should come back
	 * @param pec 
	 * @param session
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR11(ParallelExecutionContext pec, Alias alias12) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" Battery1AR11");
		String fkey = key + String.format(uniqKeyFmt, min.get());
		Relatrix.findStream(alias12, fkey, "Has time", '*').parallel().forEach(e->
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			Result nex = (Result)e;
			if( DEBUG ) System.out.println("1AR11: SHOULD NOT HAVE ENCOUNTERED:"+nex.get(0));
			throw new RuntimeException("1AR11: SHOULD NOT HAVE ENCOUNTERED:"+nex.get(0));
		}));
		System.out.println("BATTERY1AR11 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * remove entries, all relationships should be recursively deleted
	 * @param argv
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR17(Alias alias12) throws Exception {
		long tims = System.currentTimeMillis();
		i = new AtomicInteger(min.get());
		System.out.println(alias12+" CleanDB DMR size="+Relatrix.size(alias12,Relation.class));
		System.out.println("CleanDB DRM size="+Relatrix.size(alias12,DomainRangeMap.class));
		System.out.println("CleanDB MDR size="+Relatrix.size(alias12,MapDomainRange.class));
		System.out.println("CleanDB MDR size="+Relatrix.size(alias12,MapRangeDomain.class));
		System.out.println("CleanDB RDM size="+Relatrix.size(alias12,RangeDomainMap.class));
		System.out.println("CleanDB RMD size="+Relatrix.size(alias12,RangeMapDomain.class));
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		Iterator<?> it = Relatrix.findSet(alias12,'*','*','*');
		timx = System.currentTimeMillis();
		it.forEachRemaining(fkey-> {
			Relation dmr = (Relation)((Result)fkey).get(0);
			try {
				Relatrix.remove(alias12,dmr);
			} catch (IllegalArgumentException | ClassNotFoundException | IllegalAccessException | IOException e) {
				throw new RuntimeException(e);
			}
			i.getAndIncrement();
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" total, current="+fkey);
				timx = System.currentTimeMillis();
			}
		});
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
	
}
