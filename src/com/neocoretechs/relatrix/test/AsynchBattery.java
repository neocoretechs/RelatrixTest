package com.neocoretechs.relatrix.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.Relatrix;
import com.neocoretechs.relatrix.RelatrixKV;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;
import com.neocoretechs.relatrix.type.RelationList;


/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findSet retrieval using the client to a remote {@link com.neocoretechs.relatrix.server.Relatrix}.
 * We also test the asynchronous client and parallel query function therein.
 * NOTES:
 * program arguments are remote_node remote_port_for_database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 *
 */
public class AsynchBattery {
	public static boolean DEBUG = false;
	
		public static int displayLinesOn[]= {0,1000,5000,9990,15000,20000,30000,40000,50000,60000,70000,80000,90000,99000};
		public static int displayLinesOff[]= {100,1100,5100,9999,15999,20999,30999,40999,50999,60999,70999,80999,90999,100000};
		public static int displayLine = 0;
		public static int displayLineCtr = 0;
		public static long displayTimer = 0;
		public static int min = 0;
		public static int max = 100;
		public static int div = 10;
		static String key = "This is a test"; 
		static String uniqKeyFmt = "%0100d";
		private static boolean DISPLAY = false;
		private static boolean DISPLAYALL = true;
	
		/**
		*/
		public static void main(String[] argv) throws Exception {
			 //System.out.println("Analysis of all");
			IndexResolver indexResolver = new IndexResolver();
			ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
			ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
				Relatrix.getInstance();
				try {
			AbstractRelation.displayLevel = AbstractRelation.displayLevels.VERBOSE;
		
			if(argv.length == 3 && argv[3].equals("init")) {
					battery1AR17(argv);
			}
			long siz = Relatrix.size();
			if(siz == 0) {
				battery0(argv);
			}
			battery1(argv);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
			System.out.println("TEST BATTERY COMPLETE.");	
			System.exit(1);
		}
		
		public static void displayCtrl() {
			if(displayLine == 0)
				displayLineCtr = 0;
			if(displayLine >= displayLinesOn[displayLineCtr] && displayLine <= displayLinesOff[displayLineCtr]) {
				if(!DISPLAY)
					displayTimer = System.currentTimeMillis();
				DISPLAY  = true;
			} else {
				if(DISPLAY)
					System.out.println("Time between lines:"+displayLinesOn[displayLineCtr]+" and "+displayLinesOff[displayLineCtr]+" is "+(System.currentTimeMillis()-displayTimer)+" ms.");
				DISPLAY = false;
				if(displayLine > displayLinesOff[displayLineCtr] && displayLineCtr < displayLinesOff.length-1)
					++displayLineCtr;
			}
			++displayLine;
		}
		/**
		 * Loads up on keys
		 * @param argv
		 * @throws Exception
		 */
		public static void battery0(String[] argv) throws Exception {
			System.out.println("Battery0 ");
			long tims = System.currentTimeMillis();

			int recs = 0;
			String fkey = null;
			CompletableFuture<Relation> dmr = null;
			for(int i = min; i < max; i++) {
				fkey = key + String.format(uniqKeyFmt, i);
				Relatrix.store(fkey, "Has unit", Long.valueOf(i));
				++recs;
			}
		
			 System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records");
		}

		/**
		 * @param argv
		 * @throws Exception
		 */
		public static void battery1(String[] argv) throws Exception {
			System.out.println("Iterator Battery1 ");
			long tims = System.currentTimeMillis();
			// this list will store an object used to test subsequent queries where a named object is needed
			// it will be extracted from the wildcard queries
			ArrayList<Result> ar = new ArrayList<Result>(); // range
		
			Iterator<?> it = null;
			System.out.println("Wildcard queries:");
			displayLine = 0;
			System.out.println("1.) findSet(*,*,*)...");
			it = Relatrix.findSet('*', '*', '*');
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				ar.add(c);
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine = 0;
	
			System.out.println("----------");
			System.out.println("Above are all the wildcard permutations. Now retrieve those with object references from array size:"+ar.size());
			it = null;
			for(int j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("2."+j+") findSet(<obj>,<obj>,<obj>) using ="+
						arel[0]+",("+arel[0].getClass().getName()+"),"+
						arel[1]+",("+arel[1].getClass().getName()+"),"+
						arel[2]+",("+arel[2].getClass().getName());
				it = Relatrix.findSet(arel[0], arel[1], arel[2]);
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY || DISPLAYALL)
						System.out.println("(2."+j+" of "+ar.size()+") "+displayLine+"="+c);
				}
			}
			it = null;
			ArrayList<Object> clist = new ArrayList<Object>();
			for(int j = 0; j < ar.size(); j++) {
				//RelatrixHeadsetIterator.DEBUG = true;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				clist.add(arel[0]);
			}
			RelationList res = queryParallelDomain(clist);
			System.out.println("3.) findSetParallel(<obj>,*,*) using domain list size:"+clist.size()+" returning size:"+res.size());
			displayLine = 0;
			for(int j = 0; j < res.size(); j++) {
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(3."+j+" of "+res.size()+") "+displayLine+"="+res.get(j));
			}
			//
			clist = new ArrayList<Object>();
			for(int j = 0; j < ar.size(); j++) {
				//RelatrixHeadsetIterator.DEBUG = true;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				clist.add(arel[1]);
			}
			res = queryParallelMap(clist);
			System.out.println("4.) findSetParallel(*,<obj>,*) using map list size:"+clist.size()+" returning size:"+res.size());
			displayLine = 0;
			for(int j = 0; j < res.size(); j++) {
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(4."+j+" of "+res.size()+") "+displayLine+"="+res.get(j));
			}
			clist = new ArrayList<Object>();
			for(int j = 0; j < ar.size(); j++) {
				//RelatrixHeadsetIterator.DEBUG = true;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				clist.add(arel[2]);
			}
			res = queryParallelRange(clist);
			System.out.println("5.) findSetParallel(*,*,<obj>) using range list size:"+clist.size()+" returning size:"+res.size());
			displayLine = 0;
			for(int j = 0; j < res.size(); j++) {
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(5."+j+" of "+res.size()+") "+displayLine+"="+res.get(j));
			}
			//-------------------------
			for(int j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("6."+j+") FindSet(<obj>,*,*) using domain="+arel[0]);
				it = Relatrix.findSet(arel[0], '*', '*');	
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY || DISPLAYALL)
						System.out.println("(5."+j+" of "+ar.size()+") "+displayLine+"="+c);
				}
			}
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			it = null;
			for(int j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("7."+j+") findSet(*,<obj>,<obj>) using map="+arel[1]+" class:"+arel[1].getClass().getName()+" range="+arel[2]+" class:"+arel[2].getClass().getName());
				it = Relatrix.findSet('*', arel[1], arel[2]);
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY || DISPLAYALL)
						System.out.println("(6."+j+" of "+ar.size()+") "+displayLine+"="+c);
				}
			}
			it = null;
			for(int j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("8."+j+") findSet(<obj>,*,<obj>) using domain="+arel[0]+arel[1].getClass().getName()+" range:"+arel[2]+" class:"+arel[1].getClass().getName());
				it = Relatrix.findSet(arel[0], '*', arel[2]);
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY || DISPLAYALL)
						System.out.println("(7."+j+" of "+ar.size()+") "+displayLine+"="+c);
				}
			}
			it = null;
			for(int j = 0; j < ar.size(); j++) {
				displayLine=0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("9."+j+") findSet(<obj>,<obj>,*) using domain="+arel[0]+", map="+arel[1]);
				it = Relatrix.findSet(arel[0], arel[1], '*');
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY || DISPLAYALL)
						System.out.println("(8."+j+" of "+ar.size()+") "+displayLine+"="+c);
				}
			}

			System.out.println("ServerRetrievalBattery0 SUCCESS in "+(System.currentTimeMillis()-tims));
		}
		public static RelationList queryParallelDomain(List<Object> query) throws IllegalArgumentException, ClassNotFoundException, IllegalAccessException, IOException, InterruptedException, ExecutionException {
			RelationList res = null;
			//try (var _ = Timer.log("Querying combined hash for List of "+query.size())) {
				res = (RelationList) Relatrix.findSetParallel(query, '*', '*');
			
			//}
			return res;
		}	
		public static RelationList queryParallelMap(List<Object> query) throws IllegalArgumentException, ClassNotFoundException, IllegalAccessException, IOException, InterruptedException, ExecutionException {
			RelationList res = null;
			//try (var _ = Timer.log("Querying combined hash for List of "+query.size())) {
				res = (RelationList) Relatrix.findSetParallel('*', query, '*');
			//}
			return res;
		}
		public static RelationList queryParallelRange(List<Object> query) throws IllegalArgumentException, ClassNotFoundException, IllegalAccessException, IOException, InterruptedException, ExecutionException {
			RelationList res = null;
			//try (var _ = Timer.log("Querying combined hash for List of "+query.size())) {
				res = (RelationList) Relatrix.findSetParallel('*', '*', query);
			//}
			return res;
		}
		
		/**
		 * remove entries
		 * @param argv
		 * @throws Exception
		 */
		public static void battery1AR17(String[] argv) throws Exception {
			long tims = System.currentTimeMillis();
			System.out.println("CleanDB");
			Iterator it = Relatrix.findSet('*','*','*');
			long timx = System.currentTimeMillis();
			int i = 0;
			while(it.hasNext()) {
				Object fkey = it.next();
				Relation dmr = (Relation)((Result)fkey).get(0);
				Relatrix.remove(dmr.getDomain(), dmr.getMap());
				++i;
				if((System.currentTimeMillis()-timx) > 1000) {
					System.out.println("deleting "+i+" "+fkey);
					timx = System.currentTimeMillis();
				}
			}
			System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
		}
	
}
