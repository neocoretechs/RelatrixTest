package com.neocoretechs.relatrix.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.Relatrix;
import com.neocoretechs.relatrix.RelatrixKV;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.Result1;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

/**
 * This series of tests uses classes and concrete object instances in various findStream permutations
 * resulting in streams.
 * NOTES:
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021
 *
 */
public class EmbeddedStreamRetrievalBattery {
	public static boolean DEBUG = true;
	public static int min = 0;
	public static int max = 100;
	static String key = "This is a test"; 
	static String uniqKeyFmt = "%0100d";
	public static int recs = 0;
	private static int SAMPLESIZE = 5;
	/**
	*/
	public static void main(String[] argv) throws Exception {
		//System.out.println("Analysis of all");
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				Relatrix.getInstance();
				AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
				if(argv.length == 1 && argv[0].equals("init")) {
					battery1AR17();
				}
				if(Relatrix.size() == 0) {
					battery0();
				}
				battery1();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("TEST BATTERY COMPLETE.");	
		System.exit(1);
	}
	/**
	 * Loads up on keys
	 * @throws Exception
	 */
	public static void battery0() throws Exception {
		System.out.println("Battery0 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		Relation dmr = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				dmr = Relatrix.store(fkey, "Has unit", Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		 System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}

	/**
	 * @throws Exception
	 */
	public static void battery1() throws Exception {
		System.out.println("Stream Battery1 ");
		long tims = System.currentTimeMillis();
		recs = 0;
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Result> ar = new ArrayList<Result>();
	
		System.out.println("Wildcard queries. Will store samplesize of "+SAMPLESIZE+" for subsequent tests.");
		recs = 0;
		System.out.println("1.) findStream(*,*,*)...");
		Relatrix.findStream('*', '*', '*').parallel().forEach(e->{
			System.out.println(++recs+"="+e);
		});
		recs = 0;
		System.out.println("2.) findStream(*,*,*)...");		
		Relatrix.findStream('*', '*', '*').parallel().forEach(e->{
			System.out.println(++recs+"="+e);
			if(ar.size() < SAMPLESIZE  ) 
				ar.add((Result)e);
		});
	
		System.out.println("Above are all the wildcard permutations. Now retrieve those with object references using the results");
		System.out.println("3 object instances:");
		for(int j = 0; j < ar.size(); j++) {
			recs = 0;
			System.out.println("9."+j+") findStream(<obj>,<obj>,<obj>) using domain="+((Result)ar.get(j)).getDomain()+" map="+((Result)ar.get(j)).getMap()+" range="+((Result)ar.get(j)).getRange());
			Relatrix.findStream(((Result)ar.get(j)).getDomain(), ((Result)ar.get(j)).getMap(), ((Result)ar.get(j)).getRange()).parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
		}
		System.out.println("----------");
		System.out.println("1 object instance with wildcards:");
		for(int j = 0; j < ar.size(); j++) {
			recs = 0;
			System.out.println("10."+j+") findStream(*,*,<obj>) using range="+ar.get(j).getRange());		
			Relatrix.findStream('*', '*', ar.get(j).getRange()).parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs = 0;
			System.out.println("11."+j+") findStream(*,<obj>,*) using map="+ar.get(j).getMap());		
			Relatrix.findStream('*', ar.get(j).getMap(), '*').parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs = 0;
			System.out.println("12."+j+") findStream(<obj>,*,*) using domain="+ar.get(j).getDomain());		
			Relatrix.findStream(ar.get(j).getDomain(), '*', '*').parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
		}
		System.out.println("----------");
		System.out.println("2 object instances with wildcards:");
		for(int j = 0; j < ar.size(); j++) {
			recs = 0;
			System.out.println("13."+j+") findStream(*,<obj>,<obj>) using map="+ar.get(j).getDomain()+" range="+ar.get(j).getMap());		
			Relatrix.findStream('*', ar.get(j).getDomain(), ar.get(j).getMap()).parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs = 0;
			System.out.println("14."+j+") findStream(<obj>,*,<obj>) using domain="+ar.get(j).getDomain()+" range="+ar.get(j).getMap());		
			Relatrix.findStream(ar.get(j).getDomain(), '*', ar.get(j).getMap()).parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs = 0;
			System.out.println("15."+j+") findStream(<obj>,<obj>,*) using domain="+ar.get(j).getDomain()+" map="+ar.get(j).getMap());		
			Relatrix.findStream(ar.get(j).getDomain(), ar.get(j).getMap(), '*').parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
		}
		recs = 0;
		System.out.println("----------");
		System.out.println("1 object instance with 2 wildcard");
		for(int j = 0; j < ar.size(); j++) {
			System.out.println("16."+j+") findStream(*,*,<obj>) using range="+ar.get(j).getRange());		
			Relatrix.findStream('*', '*', ar.get(j).getRange()).parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs =0;
			System.out.println("17."+j+") findStream(*,<obj>,*) using map="+ar.get(j).getMap());		
			Relatrix.findStream('*', ar.get(j).getMap(), '*').parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs =0;
			System.out.println("18."+j+") findStream(<obj>,*,*) using domain="+ar.get(j).getDomain());		
			Relatrix.findStream(ar.get(j).getDomain(), '*', '*').parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
		}
		System.out.println("----------");
		System.out.println("2 object instances with 1 wildcard:");
		for(int j = 0; j < ar.size(); j++) {
			recs = 0;
			System.out.println("19."+j+") findStream(?,<obj>,<obj>) using map="+ar.get(j).getDomain()+" range="+ar.get(j).getMap());		
			Relatrix.findStream('*', ar.get(j).getDomain(), ar.get(j).getMap()).parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs = 0;
			System.out.println("20."+j+") findStream(<obj>,*,<obj>) using domain="+ar.get(j).getDomain()+" range="+ ar.get(j).getMap());		
			Relatrix.findStream(ar.get(j).getDomain(), '*', ar.get(j).getMap()).parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
			recs = 0;
			System.out.println("21."+j+") findStream(<obj>,<obj>,*) using domain="+ar.get(j).getDomain()+" map="+ar.get(j).getMap());		
			Relatrix.findStream(ar.get(j).getDomain(), ar.get(j).getMap(), '*').parallel().forEach(e->{
				System.out.println(++recs+"="+e);
			});
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims));
	}
	/**
	 * remove entries
	 * @throws Exception
	 */
	public static void battery1AR17() throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		Iterator it = Relatrix.findSet('*','*','*');
		long timx = System.currentTimeMillis();
		int i = 0;
		while(it.hasNext()) {
			Object fkey = it.next();
			Relation dmr = (Relation)((Result1)fkey).get();
			Relatrix.remove(dmr);
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		Iterator<?> its = Relatrix.findSet('*','*','*');
		while(its.hasNext()) {
			Object nex = its.next();
			//System.out.println(i+"="+nex);
			System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
		}
		long siz = Relatrix.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(Relation.class);
		while(it.hasNext()) {
			Object nex = it.next();
			System.out.println("Relation:"+nex);
		}
		siz = Relatrix.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(DomainRangeMap.class);
		while(it.hasNext()) {
			Object nex = it.next();
			System.out.println("DomainRangeMap:"+nex);
		}
		siz = Relatrix.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixKV.entrySet(MapDomainRange.class);
		while(it.hasNext()) {
			Object nex = it.next();
			System.out.println("MapDomainRange:"+nex);
		}
		siz = RelatrixKV.size(MapDomainRange.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixKV.entrySet(MapRangeDomain.class);
		while(it.hasNext()) {
			Object nex = it.next();
			System.out.println("MapRangeDomain:"+nex);
		}
		siz = RelatrixKV.size(MapRangeDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(RangeDomainMap.class);
		while(it.hasNext()) {
			Object nex = it.next();
			System.out.println("RangeDomainMap:"+nex);
		}
		siz = RelatrixKV.size(RangeDomainMap.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(RangeMapDomain.class);
		while(it.hasNext()) {
			Object nex = it.next();
			System.out.println("RangeMapDomain:"+nex);
		}
		siz = RelatrixKV.size(RangeMapDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
}
