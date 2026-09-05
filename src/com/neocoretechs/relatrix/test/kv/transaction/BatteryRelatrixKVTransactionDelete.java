package com.neocoretechs.relatrix.test.kv.transaction;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.AbstractRelation.displayLevels;

import com.neocoretechs.relatrix.RelatrixKVTransaction;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.rocksack.TransactionId;

import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

/**
 * The set of tests verifies the delete functions in the {@link  RelatrixKVTransaction}<p>
 * Create a series of nested relations and then verify that they are properly deleted when a reference to them was previously deleted.<p>
 * This represents sets deeply nested relations introducing a heavy demand on a series of aliased databases. 
 * NOTES:
 * optional arguments are [ [init] [max nnn] ]
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 *
 */
public class BatteryRelatrixKVTransactionDelete {
	public static boolean DEBUG = false;
	static String key = "This is a test"; // holds the base random key string for tests
	static String val = "Of a Relatrix element!"; // holds base random value string
	static String uniqKeyFmt = "%0100d"; // base + counter formatted with this gives equal length strings for canonical ordering
	static int min = 0;
	static int max = 100;
	static int numDelete = 100; // for delete test
	static int i = 0;
	private static long timx;
	private static TransactionId xid,xid2,xid3;
	//static Alias alias2 = new Alias("ALIAS2");
	//static Alias alias3 = new Alias("ALIAS3");
	private static int MAX_RETRIES = 10;
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				RelatrixKVTransaction.getInstance();
				//RelatrixTransaction.setAlias(alias2,RelatrixTransaction.getTableSpace()+alias2);
				//RelatrixTransaction.setAlias(alias3,RelatrixTransaction.getTableSpace()+alias3);
				xid = RelatrixKVTransaction.getTransactionId();
				//xid2 = RelatrixTransaction.getTransactionId();
				//xid3 = RelatrixTransaction.getTransactionId();
				AbstractRelation.displayLevel = displayLevels.VERBOSE;
				if(argv.length > 0 && argv[0].equals("max")) {
					System.out.println("Setting max items to "+argv[1]);
					max = Integer.parseInt(argv[1]);
				} else {
					if(argv.length > 0 && argv[0].equals("init")) {
						System.out.println("Initialize database to zero items, then terminate...");
						battery1AR17(xid);
						//battery1AR17(alias2, xid2);
						//battery1AR17(alias3, xid3);
						System.exit(0);
					}
				}
				long siz = RelatrixKVTransaction.size(xid, String.class);
				if( siz == 0 ) {//&& RelatrixTransaction.size(alias2,xid2) == 0 && RelatrixTransaction.size(alias3,xid3) == 0) {
					if(DEBUG)
						System.out.println("Zero items, Begin insertion test from "+min+" to "+max);
					battery1(xid);
					battery1AR6(xid);
					//battery1(alias2, xid2);
					//battery1AR6(alias2, xid2);
					//battery1(alias3, xid3);
					//battery1AR6(alias3, xid3);
		
					//if(DEBUG)
					//	System.out.println("Begin duplicate key rejection test from "+min+" to "+max);
					// optional duplicate key rejection
					//battery11(alias1, xid);
					//battery11(alias2, xid);
					//battery11(alias3, xid);
				}else {
					System.out.println("Size is "+siz+" items, proceed to delete...");
					battery1AR6(xid);
				}		

				RelatrixKVTransaction.commit(xid);
				//RelatrixTransaction.commit(alias2,xid2);
				//RelatrixTransaction.commit(alias3,xid3);
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
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery1(TransactionId xid2) throws Exception {
		System.out.println(xid2+" Battery1 ");
		long tims = System.currentTimeMillis();
		long timt = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				RelatrixKVTransaction.store(xid2, fkey, Long.valueOf(i));
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("storing "+recs+" "+fkey);
					tims = System.currentTimeMillis();
				}
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		RelatrixKVTransaction.commit( xid2);
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-timt)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	
	/**
	 * Tries to store partial key that should match existing keys, should reject all.
	 * Domain/map determines unique key
	 * @param argv
	 * @param alias12 
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery11(TransactionId xid2) throws Exception {
		System.out.println(xid2+" Battery11 ");
		long tims = System.currentTimeMillis();
		long timt = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				RelatrixKVTransaction.store(xid2, fkey, Long.valueOf(i));
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("SHOULD NOT BE storing "+recs+" "+fkey);
					tims = System.currentTimeMillis();
				}
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		if( recs > 0) {
			RelatrixKVTransaction.commit(xid2);
			throw new DuplicateKeyException(" BATTERY11 FAIL, stored "+recs+" when zero should have been stored");
		} else {
			System.out.println("BATTERY11 SUCCESS in "+(System.currentTimeMillis()-timt)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
		}
	}
	
	/**
	 * Test the higher level functions in the Relatrix. Use the 'findSet' permutations to
	 * verify the previously inserted data
	 * @param argv
	 * @param alias12 
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery1AR6(TransactionId xid2) throws Exception {
		i = min;
		long tims = System.currentTimeMillis();
		System.out.println(xid2+" Battery1AR6 ");
		for(int i = min; i < max; i++) {
			String fkey = key + String.format(uniqKeyFmt, i);
			RelatrixKVTransaction.remove(xid2, fkey);
			if((System.currentTimeMillis()-tims) > 1000) {
				System.out.println("deleting "+fkey);
				tims = System.currentTimeMillis();
			}
			/*
			RelatrixTransaction.findStream(alias12, xid2,"*", "*", irec).forEach(e->{
				Result nex = (Result)e;
				System.out.println("KEY MISMATCH:"+nex);
				throw new RuntimeException("MAP KEY MISMATCH:"+nex);
			});
			*/
		}
		//RelatrixTransaction.commit(alias12, xid2);
		/*RelatrixTransaction.flushAndCompactDB(alias12, xid2, Long.class );
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, String.class );
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, Relation.class );
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, DomainRangeMap.class);
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, MapDomainRange.class);
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, MapRangeDomain.class);
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, RangeDomainMap.class);
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, RangeMapDomain.class);
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, DBKey.class );
		RelatrixTransaction.flushAndCompactDB(alias12, xid2, PrimaryKeySet.class );*/
		int attempts = 0;
		while (true) {
		  try {
		    RelatrixKVTransaction.commit(xid2);
		    break;
		  } catch (Exception e) {
		    if ((e.getMessage().contains("Busy") || e.getCause().getMessage().contains("Busy")) && attempts < MAX_RETRIES) {
		    	System.out.println("Retry commit - attempt "+attempts);
		      attempts++;
		      Thread.sleep(50 * attempts); // small backoff
		      continue;
		    } else {
		    	System.out.println("Unhandled commit exception:"+e.getMessage());
		      RelatrixKVTransaction.rollback(xid2);
		      return;
		    }
		  }
		}
		long siz = RelatrixKVTransaction.size(xid2, String.class);
		// when finished, all records should theoretically be deleted
		if(siz  > 0) {
			System.out.println("BATTERY1AR6 unexpected number of keys "+siz);
			RelatrixKVTransaction.keySetStream(xid2, String.class).forEach(e->{
				System.out.println("Del fault:"+e);
			});
			throw new Exception("BATTERY1AR6 unexpected number of keys "+siz);
		}
		 System.out.println("BATTERY1AR6 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	/**
	 * remove entries
	 * @param argv
	 * @param alias12 
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery1AR17(TransactionId xid2) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println(xid+" CleanDB DMR size="+RelatrixKVTransaction.size(xid, String.class));
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		Iterator<?> it = RelatrixKVTransaction.keySet(xid, String.class);
		timx = System.currentTimeMillis();
		it.forEachRemaining(fkey-> {
			try {
				RelatrixKVTransaction.remove(xid, (Comparable) it.next());
			} catch (IllegalArgumentException | ClassNotFoundException | IllegalAccessException | IOException e) {
				throw new RuntimeException(e);
			}
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" total, current="+fkey);
				timx = System.currentTimeMillis();
			}
		});
		Iterator<?> its = RelatrixKVTransaction.keySet(xid, String.class);
		while(its.hasNext()) {
			Comparable c = (Comparable) its.next();
			//System.out.println(i+"="+nex);
			if(DEBUG)
				System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+c);
			else
				throw new Exception("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+c);
		}
	
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	
}
