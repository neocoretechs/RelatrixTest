package com.neocoretechs.relatrix.test.kv.transaction;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.AbstractRelation.displayLevels;
import com.neocoretechs.rocksack.Alias;
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
public class BatteryRelatrixKVTransactionDeleteAlias {
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
	static Alias alias1 = new Alias("ALIAS1");
	static Alias alias2 = new Alias("ALIAS2");
	static Alias alias3 = new Alias("ALIAS3");
	//static Alias alias2 = new Alias("ALIAS2");
	//static Alias alias3 = new Alias("ALIAS3");
	private static int MAX_RETRIES = 10;
	/**
	* Main test fixture driver
	*/
	public static void main(String[] argv) throws Exception {
		System.out.println("Begin BatteryRelatrixKVTransactionDeleteAlias");
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				RelatrixKVTransaction.getInstance();
				RelatrixKVTransaction.setAlias(alias1,RelatrixKVTransaction.getTableSpace()+alias1);
				RelatrixKVTransaction.setAlias(alias2,RelatrixKVTransaction.getTableSpace()+alias2);
				RelatrixKVTransaction.setAlias(alias3,RelatrixKVTransaction.getTableSpace()+alias3);
				xid = RelatrixKVTransaction.getTransactionId();
				xid2 = RelatrixKVTransaction.getTransactionId();
				xid3 = RelatrixKVTransaction.getTransactionId();
				AbstractRelation.displayLevel = displayLevels.VERBOSE;
				if(argv.length > 0 && argv[0].equals("max")) {
					System.out.println("Setting max items to "+argv[1]);
					max = Integer.parseInt(argv[1]);
				} else {
					if(argv.length > 0 && argv[0].equals("init")) {
						System.out.println("Initialize database to zero items, then terminate...");
						battery1AR17(alias1, xid);
						battery1AR17(alias2, xid2);
						battery1AR17(alias3, xid3);
						System.exit(0);
					}
				}
				long siz = RelatrixKVTransaction.size(alias1, xid, String.class);
				if(siz == 0 ) {//&& RelatrixTransaction.size(alias2,xid2) == 0 && RelatrixTransaction.size(alias3,xid3) == 0) {
					if(DEBUG)
						System.out.println("Zero items, Begin insertion test from "+min+" to "+max);
					battery1(alias1, xid);
					battery1(alias2, xid2);
					battery1(alias1, xid3);
					// MUST OBTAIN NEW TRANSACTION ID AFTER INSERT!
					RelatrixKVTransaction.endTransaction(xid);
					RelatrixKVTransaction.endTransaction(xid2);
					RelatrixKVTransaction.endTransaction(xid3);
					xid = RelatrixKVTransaction.getTransactionId();
					xid2 = RelatrixKVTransaction.getTransactionId();
					xid3 = RelatrixKVTransaction.getTransactionId();
					battery1AR6(alias1, xid);
					battery1AR6(alias2, xid2);
					battery1AR6(alias3, xid3);
		
					//if(DEBUG)
					//	System.out.println("Begin duplicate key rejection test from "+min+" to "+max);
					// optional duplicate key rejection
					//battery11(alias1, xid);
					//battery11(alias2, xid);
					//battery11(alias3, xid);
				} else {
					System.out.println("Size is "+siz+" items, proceed to delete...");
					battery1AR6(alias1, xid);
					battery1AR6(alias2, xid2);
					battery1AR6(alias3, xid3);
				}	

				RelatrixKVTransaction.endTransaction(xid);
				RelatrixKVTransaction.endTransaction(xid2);
				RelatrixKVTransaction.endTransaction(xid3);
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
	public static void battery1(Alias alias12, TransactionId xid2) throws Exception {
		System.out.println(xid2+" Battery1 "+alias12);
		long tims = System.currentTimeMillis();
		long timt = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				RelatrixKVTransaction.store(alias12, xid2, fkey, Long.valueOf(i));
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("storing "+recs+" "+fkey);
					tims = System.currentTimeMillis();
				}
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		RelatrixKVTransaction.commit(alias12, xid2);
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
	public static void battery11(Alias alias12, TransactionId xid2) throws Exception {
		System.out.println(xid2+" Battery11 "+alias12);
		long tims = System.currentTimeMillis();
		long timt = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				RelatrixKVTransaction.store(alias12, xid2, fkey, Long.valueOf(i));
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("SHOULD NOT BE storing "+recs+" "+fkey);
					tims = System.currentTimeMillis();
				}
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		if( recs > 0) {
			RelatrixKVTransaction.commit(alias12, xid2);
			throw new DuplicateKeyException(alias12+" BATTERY11 FAIL, stored "+recs+" when zero should have been stored");
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
	public static void battery1AR6x(Alias alias12, TransactionId xid2) throws Exception {
		i = min;
		long tims = System.currentTimeMillis();
		System.out.println(xid2+" Battery1AR6 "+alias12);
		for(int i = min; i < max; i++) {
			String fkey = key + String.format(uniqKeyFmt, i);
			RelatrixKVTransaction.remove(alias12, xid2, fkey);
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
		    RelatrixKVTransaction.commit(alias12, xid2);
		    break;
		  } catch (Exception e) {
		    if ((e.getMessage().contains("Busy") || e.getCause().getMessage().contains("Busy")) && attempts < MAX_RETRIES) {
		    	System.out.println("Retry commit - attempt "+attempts);
		      attempts++;
		      Thread.sleep(50 * attempts); // small backoff
		      continue;
		    } else {
		    	System.out.println("Unhandled commit exception:"+e.getMessage());
		      RelatrixKVTransaction.rollback(alias12, xid2);
		      return;
		    }
		  }
		}
		long siz = RelatrixKVTransaction.size(alias12, xid2, String.class);
		// when finished, all records should theoretically be deleted
		if(siz  > 0) {
			System.out.println("BATTERY1AR6 unexpected number of keys "+siz);
			RelatrixKVTransaction.keySetStream(alias12, xid2, String.class).forEach(e->{
				System.out.println("Del fault:"+e);
			});
			throw new Exception("BATTERY1AR6 unexpected number of keys "+siz);
		}
		 System.out.println("BATTERY1AR6 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

	public static void battery1AR6(Alias alias12, TransactionId xid2) throws Exception {
	    long tims = System.currentTimeMillis();
	    System.out.println("Starting delete phase for " + xid2 + " " + alias12);

	    int attempts = 0;
	    final int MAX_RETRIES_LOCAL = MAX_RETRIES;

	    // We will replay the delete loop into a fresh transaction on each attempt.
	    while (true) {
	        // Use the provided xid2 for the first attempt, otherwise obtain a fresh one
	        TransactionId localXid = (attempts == 0) ? xid2 : RelatrixKVTransaction.getTransactionId();
	        i = min;
	        long loopStart = System.currentTimeMillis();

	        try {
	            for (int i = min; i < max; i++) {
	                String fkey = key + String.format(uniqKeyFmt, i);
	                RelatrixKVTransaction.remove(alias12, localXid, fkey);
	                if ((System.currentTimeMillis() - loopStart) > 1000) {
	                    System.out.println("deleting " + fkey);
	                    loopStart = System.currentTimeMillis();
	                }
	            }

	            // Attempt commit for this transaction context
	            RelatrixKVTransaction.commit(alias12, localXid);

	            // success: break out of retry loop
	            break;

	        } catch (Exception e) {
	            // Robustly detect Busy (guard against null cause/message)
	            String msg = (e.getMessage() == null) ? "" : e.getMessage();
	            String causeMsg = (e.getCause() != null && e.getCause().getMessage() != null) ? e.getCause().getMessage() : "";
	            boolean isBusy = msg.contains("Busy") || causeMsg.contains("Busy");

	            // Ensure we release transaction resources BEFORE sleeping/retrying
	            try { RelatrixKVTransaction.rollback(alias12, localXid); } catch (Exception ignore) {}

	            if (isBusy && attempts < MAX_RETRIES_LOCAL) {
	                attempts++;
	                long backoffMs = Math.min(50L * (1L << (attempts - 1)), 5000L);
	                long jitter = ThreadLocalRandom.current().nextLong(0, 100);
	                long sleepMs = backoffMs + jitter;
	                System.out.println("Commit Busy for " + alias12 + " attempt " + attempts + ", sleeping " + sleepMs + "ms before retry");
	                Thread.sleep(sleepMs);
	                // loop will retry with a fresh transaction id and replay deletes
	                continue;
	            } else {
	                System.out.println("Unhandled commit exception for " + alias12 + ": " + e);
	                // best effort cleanup and return so caller can inspect state
	                try { RelatrixKVTransaction.rollback(alias12, localXid); } catch (Exception ignore) {}
	                return;
	            }
	        }
	    }

	    // Verify deletion result
	    long siz = RelatrixKVTransaction.size(alias12, xid2, String.class);
	    if (siz > 0) {
	        System.out.println("BATTERY1AR6 unexpected number of keys " + siz);
	        RelatrixKVTransaction.keySetStream(alias12, xid2, String.class).forEach(e -> {
	            System.out.println("Del fault:" + e);
	        });
	        throw new Exception("BATTERY1AR6 unexpected number of keys " + siz);
	    }
	    System.out.println("BATTERY1AR6 SUCCESS in " + (System.currentTimeMillis() - tims) + " ms.");
	}

	/**
	 * remove entries
	 * @param argv
	 * @param alias12 
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery1AR17(Alias alias12, TransactionId xid2) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println(xid+" CleanDB DMR size="+RelatrixKVTransaction.size(alias12, xid, String.class));
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		Iterator<?> it = RelatrixKVTransaction.keySet(alias12, xid, String.class);
		timx = System.currentTimeMillis();
		it.forEachRemaining(fkey-> {
			try {
				RelatrixKVTransaction.remove(alias12, xid, (Comparable) it.next());
			} catch (IllegalArgumentException | ClassNotFoundException | IllegalAccessException | IOException e) {
				throw new RuntimeException(e);
			}
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" total, current="+fkey);
				timx = System.currentTimeMillis();
			}
		});
		Iterator<?> its = RelatrixKVTransaction.keySet(alias12, xid, String.class);
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
