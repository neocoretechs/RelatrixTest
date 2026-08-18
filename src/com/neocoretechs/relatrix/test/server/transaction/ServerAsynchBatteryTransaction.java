package com.neocoretechs.relatrix.test.server.transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.Result;

import com.neocoretechs.relatrix.client.asynch.AsynchRelatrixClientTransaction;
import com.neocoretechs.rocksack.TransactionId;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findSet retrieval using the client to a remote {@link com.neocoretechs.relatrix.server.RelatrixTransactionServer}.
 * We also test the asynchronous client and parallel query function therein.
 * NOTES:
 * program arguments are remote_node remote_port_for_database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 *
 */
public class ServerAsynchBatteryTransaction {
	public static boolean DEBUG = false;
	private static AsynchRelatrixClientTransaction rkvc ;
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
		private static TransactionId xid;
		/**
		*/
		public static void main(String[] argv) throws Exception {
			 //System.out.println("Analysis of all");
			if(argv.length < 3) {
				System.out.println("Usage: <remoteNode> <remotePort> [init]");
			}
			rkvc = new AsynchRelatrixClientTransaction(argv[0], Integer.parseInt(argv[1]) );
			AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
			xid = rkvc.getTransactionId();
			if(argv.length == 3 && argv[3].equals("init")) {
					battery1AR17(argv);
			}
			CompletableFuture<Long> siz = rkvc.size(xid);
			if(siz.get() == 0) {
				battery0(argv);
			}
			battery1(argv);
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
				dmr = rkvc.store(xid, fkey, "Has unit", Long.valueOf(i));
				dmr.get();
				++recs;
			}
			rkvc.commit(xid);
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
			CompletableFuture<Iterator> itc = rkvc.findSet(xid, '*', '*', '*');
			it =  itc.get();
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
				if(it != null)
					rkvc.setIterator(it);
				itc = rkvc.findSet(xid, arel[0], arel[1], arel[2]);
				it = itc.get();
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY || DISPLAYALL)
						System.out.println("(2."+j+" of "+ar.size()+") "+displayLine+"="+c);
				}
			}
			it = null;
			for(int j = 0; j < ar.size(); j++) {
				displayLine=0;
				//RelatrixHeadsetIterator.DEBUG = true;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("3."+j+") findSet(*,*,<obj>) using range="+arel[2]);
				if(it != null)
					rkvc.setIterator(it);
				itc = rkvc.findSet(xid, '*', '*', arel[2]);
				it = itc.get();
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY || DISPLAYALL)
						System.out.println("(3."+j+" of "+ar.size()+") "+displayLine+"="+c);
				}
			}
			ArrayList<Object> clist = new ArrayList<Object>();
			for(int j = 0; j < ar.size(); j++) {
				//RelatrixHeadsetIterator.DEBUG = true;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				clist.add(arel[1]);
			}
			List<Result> res = queryParallelMap(rkvc,clist);
			System.out.println("4.) findSetParallel(*,<obj>,*) using map list size:"+clist.size()+" returning size:"+res.size());
			displayLine = 0;
			for(int j = 0; j < res.size(); j++) {
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(4."+j+" of "+res.size()+") "+displayLine+"="+res.get(j));
			}
			it = null;
			for(int j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("5."+j+") FindSet(<obj>,*,*) using domain="+arel[0]);
				if(it != null)
					rkvc.setIterator(it);
				itc = rkvc.findSet(xid, arel[0], '*', '*');
				it = itc.get();
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
				System.out.println("6."+j+") findSet(*,<obj>,<obj>) using map="+arel[1]+" range="+arel[2]);
				if(it != null)
					rkvc.setIterator(it);
				itc = rkvc.findSet(xid, '*', arel[1], arel[2]);
				it = itc.get();
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
				System.out.println("7."+j+") findSet(<obj>,*,<obj>) using ="+arel[0]+", "+arel[2]);
				if(it != null)
					rkvc.setIterator(it);
				itc = rkvc.findSet(xid, arel[0], '*', arel[2]);
				it = itc.get();
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
				System.out.println("8."+j+") findSet(<obj>,<obj>,*) using domain="+arel[0]+", map="+arel[1]);
				if(it != null)
					rkvc.setIterator(it);
				itc = rkvc.findSet(xid, arel[0], arel[1], '*');
				it = itc.get();
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
		
		public static List<Result> queryParallelMap(AsynchRelatrixClientTransaction client, List<Object> query) throws IllegalArgumentException, ClassNotFoundException, IllegalAccessException, IOException, InterruptedException, ExecutionException {
			List<Result> res = null;
			//try (var _ = Timer.log("Querying combined hash for List of "+query.size())) {
				CompletableFuture<List> cres = client.findSetParallel(xid, '*', query, '*');
				res = cres.get();
				//if(DEBUG)
				//	for(Result r: res)
				//		System.out.println(((TimestampRole)(r.get(0))).getTimestamp()+" "+r);
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
			CompletableFuture<Iterator> itc = rkvc.findSet(xid, '*','*','*');
			Iterator it = itc.get();
			long timx = System.currentTimeMillis();
			int i = 0;
			while(it.hasNext()) {
				Object fkey = it.next();
				Relation dmr = (Relation)((Result)fkey).get(0);
				rkvc.remove(xid, dmr.getDomain(), dmr.getMap());
				++i;
				if((System.currentTimeMillis()-timx) > 1000) {
					System.out.println("deleting "+i+" "+fkey);
					timx = System.currentTimeMillis();
				}
			}
			System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
		}
	
}
