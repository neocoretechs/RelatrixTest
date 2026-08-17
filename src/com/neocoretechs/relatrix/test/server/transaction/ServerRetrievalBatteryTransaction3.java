package com.neocoretechs.relatrix.test.server.transaction;

import java.util.ArrayList;
import java.util.Iterator;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.Result1;
import com.neocoretechs.relatrix.client.RelatrixClientTransaction;
import com.neocoretechs.rocksack.TransactionId;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findTailSet retrieval using the client to a remote {@link com.neocoretechs.relatrix.server.RelatrixTransactionServer}.
 * NOTES:
 * program arguments are  remote_node remote_port_for_database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 */
public class ServerRetrievalBatteryTransaction3 {
	public static boolean DEBUG = false;
	private static RelatrixClientTransaction rkvc ;
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
		private static TransactionId xid;
		/**
		*/
		public static void main(String[] argv) throws Exception {
			if(argv.length < 2) {
				System.out.println("Usage: <remoteNode> <remotePort> [init]");
			}
			rkvc = new RelatrixClientTransaction(argv[0], Integer.parseInt(argv[1]));
			xid = rkvc.getTransactionId();
			AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
			if(argv.length == 4 && argv[3].equals("init")) {
				battery1AR17(argv);
			}
			if(rkvc.size(xid) == 0) {
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
			Relation dmr = null;
			for(int i = min; i < max; i++) {
				fkey = key + String.format(uniqKeyFmt, i);
				dmr = rkvc.store(xid,fkey, "Has unit", Long.valueOf(i));
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
			String fmap;
			long tims = System.currentTimeMillis();
			int recs = 0;
			// this list will store an object used to test subsequent queries where a named object is needed
			// it will be extracted from the wildcard queries
			ArrayList<Result> ar = new ArrayList<Result>();

			Iterator<?> it = null;
			System.out.println("Wildcard queries:");
			displayLine = 0;
			System.out.println("1.) findTailSet(*,*,*,String.class, String.class, Long.class)...");
			it =  rkvc.findTailSet(xid, '*', '*', '*',String.class, String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
				ar.add(c);
			}
			//-----------------
			System.out.println("Above are the wildcard permutations. Now retrieve those with object references using the "+ar.size());
			System.out.println("wildcard results. NOTE: Concrete object references in findTailSet typically produce null sets.");
			it = null;
			for(int j = 0; j < ar.size(); j++) {
				displayLine =0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();	
				System.out.println("2.) findTailSet(<obj>,<obj>,<obj>) using ="+
						arel[0]+",("+arel[0].getClass().getName()+"),"+
						arel[1]+",("+arel[1].getClass().getName()+"),"+
						arel[2]+",("+arel[2].getClass().getName());
				if(it != null)
					rkvc.setIterator(it);
				it = rkvc.findTailSet(xid, arel[0],arel[1],arel[2]);
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY)
						System.out.println(displayLine+"="+c);
				}
				displayLine=0;
				//RelatrixHeadsetIterator.DEBUG = true;
				System.out.println("3.) findTailSet(*,*,<obj>,String.class, String.class) using range="+arel[2]);
				if(it != null)
					rkvc.setIterator(it);
				it = rkvc.findTailSet(xid, '*', '*', arel[2], String.class, String.class);
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY)
						System.out.println(displayLine+"="+c);
				}
				displayLine = 0;
				//RelatrixHeadsetIterator.DEBUG = true;
				System.out.println("4.) findTailSet(*,<obj>,*, String.class, Long.class) using map="+arel[1]);
				if(it != null)
					rkvc.setIterator(it);
				it = rkvc.findTailSet(xid, '*', arel[1], '*',String.class, Long.class);
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY)
						System.out.println(displayLine+"="+c);
				}
				displayLine =0;
				System.out.println("5.) FindTailset(<obj>,*,*,String.class, Long.class) using domain="+arel[0]);
				if(it != null)
					rkvc.setIterator(it);
				it = rkvc.findTailSet(xid, arel[0], '*', '*', String.class, Long.class);
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY)
						System.out.println(displayLine+"="+c);
				}
				displayLine = 0;
				System.out.println("6.) findTailSet(*,<obj>,<obj>,String.class) using map="+arel[1]+" range="+arel[2]);
				if(it != null)
					rkvc.setIterator(it);
				it = rkvc.findTailSet(xid, '*', arel[1], arel[2], String.class);
				//ar = new ArrayList<Comparable>();
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY)
						System.out.println(displayLine+"="+c);
					//if(ar2.size() == 0) ar2.add(c);
				}
				displayLine = 0;
				System.out.println("7.) findTailSet(<obj>,*,<obj>,String.class) using domain="+arel[0]+", range="+arel[2]);	
				if(it != null)
					rkvc.setIterator(it);
				it = rkvc.findTailSet(xid, arel[0], '*', arel[2], String.class);
				//ar = new ArrayList<Comparable>();
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY)
						System.out.println(displayLine+"="+c);
					//if(ar2.size() == 1) ar2.add(c);
				}
				displayLine =0;
				System.out.println("8.) findTailSet(<obj>,<obj>,*, Long.class) using domain="+arel[0]+", map="+arel[1]);
				if(it != null)
					rkvc.setIterator(it);
				it = rkvc.findTailSet(xid, arel[0], arel[1], '*',Long.class);
				//ar = new ArrayList<Comparable>();
				while(it.hasNext()) {
					Object o = it.next();
					Result c = (Result)o;
					displayCtrl();
					if(DISPLAY)
						System.out.println(displayLine+"="+c);
					//if(ar2.size() == 2) ar2.add(c);
				}
			}
			//
			// ---------- range test
			Long hi = (long)(max/div);
			displayLine =0;
			String fkey2 = key + String.format(uniqKeyFmt, hi);
			System.out.println("9.) findTailSet(*,*,*,<obj>,String.class,<obj>) using domain from "+fkey2+" map=String.class "+" range from "+hi);		
			it = rkvc.findTailSet(xid, '*', '*', '*',fkey2,String.class,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			}
			hi+=(long)div;
			displayLine = 0;
			if(it != null)
				rkvc.setIterator(it);
			fkey2 = key + String.format(uniqKeyFmt, hi);
			System.out.println("10.) findTailSet(*,*,*,<obj>,String.class,<obj>) using domain from "+fkey2+" map=String.class"+" range from "+hi);		
			it = rkvc.findTailSet(xid, '*', '*', '*',fkey2,String.class,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			}
			hi+=(long)div;
			displayLine =0;
			if(it != null)
				rkvc.setIterator(it);
			fkey2 = key + String.format(uniqKeyFmt, hi);
			System.out.println("11.) findTailSet(*,*,*,<obj>,String.class,<obj>) using domain from "+fkey2+" map=String.class"+" range from "+hi);		
			it = rkvc.findTailSet(xid, '*', '*', '*',fkey2,String.class,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			}	
			hi = (long)(max/div);
			displayLine =0;
			System.out.println("12.) findTailSet(*,*,*,String.class,String.class,<obj>) using domain=String.class map=String.class "+" range from "+hi);		
			it = rkvc.findTailSet(xid, '*', '*', '*',String.class,String.class,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			}
			hi+=(long)div;
			displayLine =0;
			if(it != null)
				rkvc.setIterator(it);
			System.out.println("13.) findTailSet(*,*,*,String.class,String.class,<obj>) using domain=String.class map=String.class"+" range from "+hi);		
			it = rkvc.findTailSet(xid, '*', '*', '*',String.class,String.class,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			}
			hi+=(long)div;
			displayLine =0;
			if(it != null)
				rkvc.setIterator(it);
			System.out.println("14.) findTailSet(*,*,*,<obj>,<obj>,<obj>) using domain=String.class map=String.class"+" range from "+hi);		
			it = rkvc.findTailSet(xid, '*', '*', '*',String.class,String.class,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			}
			System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims));
		}
		/**
		 * remove entries
		 * @param argv
		 * @throws Exception
		 */
		public static void battery1AR17(String[] argv) throws Exception {
			long tims = System.currentTimeMillis();
			System.out.println("CleanDB");
			Iterator it = rkvc.findSet(xid,'*','*','*');
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
			Iterator<?> its = rkvc.findSet(xid, '*','*','*');
			while(its.hasNext()) {
				Result nex = (Result) its.next();
				//System.out.println(i+"="+nex);
				System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
			}
			long siz = rkvc.size(xid);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySet(xid, Relation.class);
			while(it.hasNext()) {
				Comparable nex = (Comparable) it.next();
				System.out.println("Relation:"+nex);
			}
			siz = rkvc.size(xid);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySet(xid, DomainRangeMap.class);
			while(it.hasNext()) {
				Comparable nex = (Comparable) it.next();
				System.out.println("DomainRangeMap:"+nex);
			}
			siz = rkvc.size(xid);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
			}

			it = rkvc.entrySet(xid, MapDomainRange.class);
			while(it.hasNext()) {
				Comparable nex = (Comparable) it.next();
				System.out.println("MapDomainRange:"+nex);
			}
			siz = rkvc.size(xid, MapDomainRange.class);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
			}

			it = rkvc.entrySet(xid, MapRangeDomain.class);
			while(it.hasNext()) {
				Comparable nex = (Comparable) it.next();
				System.out.println("MapRangeDomain:"+nex);
			}
			siz = rkvc.size(xid, MapRangeDomain.class);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySet(xid, RangeDomainMap.class);
			while(it.hasNext()) {
				Comparable nex = (Comparable) it.next();
				System.out.println("RangeDomainMap:"+nex);
			}
			siz = rkvc.size(xid, RangeDomainMap.class);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySet(xid, RangeMapDomain.class);
			while(it.hasNext()) {
				Comparable nex = (Comparable) it.next();
				System.out.println("RangeMapDomain:"+nex);
			}
			siz = rkvc.size(xid, RangeMapDomain.class);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after delete/commit");
			}/*
				it = RelatrixKV.entrySet(DBKey.class);
				while(it.hasNext()) {
					Comparable nex = (Comparable) it.next();
					System.out.println("DBKey:"+nex);
				}
				siz = RelatrixKV.size(DBKey.class);
				if(siz > 0) {
					System.out.println("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after all deleted and committed");
					throw new Exception("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after delete/commit");
				}
				it = RelatrixKV.entrySet(Long.class);
				while(it.hasNext()) {
					Comparable nex = (Comparable) it.next();
					System.out.println("Long:"+nex);
				}
				siz = RelatrixKV.size(Long.class);
				if(siz > 0) {
					System.out.println("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after all deleted and committed");
					throw new Exception("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after delete/commit");
				}
				it = RelatrixKV.entrySet(String.class);
				while(it.hasNext()) {
					Comparable nex = (Comparable) it.next();
					System.out.println("String:"+nex);
				}
				siz = RelatrixKV.size(String.class);
				if(siz > 0) {
					System.out.println("KV RANGE 1AR17 String MISMATCH:"+siz+" > 0 after all deleted and committed");
					throw new Exception("KV RANGE 1AR17 String MISMATCH:"+siz+" > 0 after delete/commit");
				}
			 */
			rkvc.commit(xid);
			System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
		}

	}

	