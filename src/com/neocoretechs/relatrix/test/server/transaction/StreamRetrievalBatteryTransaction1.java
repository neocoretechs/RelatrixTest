package com.neocoretechs.relatrix.test.server.transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

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
 * and verifying findSubStream retrieval using the client to a remote {@link com.neocoretechs.relatrix.server.RelatrixTransactionServer}.
 * NOTES:
 * program arguments are remote_node remote_port_for_database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 *
 */
public class StreamRetrievalBatteryTransaction1 {
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
		private static int j;
		private static long timx;
		private static int i;
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
			if(argv.length == 3 && argv[2].equals("init")) {
					battery1AR17(argv);
			}
			if(rkvc.size(xid) == 0) {
				battery0(argv);
			}
			battery1(argv);
			System.out.println("StreamRetrievalBattery1 COMPLETE.");	
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
			for(int i = min; i < max; i++) {
				fkey = key + String.format(uniqKeyFmt, i);
				rkvc.store(xid, fkey, "Has unit", Long.valueOf(i));
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
			System.out.println("Stream Battery1 ");
			String fmap;
			long tims = System.currentTimeMillis();
			int recs = 0;
			// this list will store an object used to test subsequent queries where a named object is needed
			// it will be extracted from the wildcard queries
			ArrayList<Result> ar = new ArrayList<Result>();

			Stream<?> it = null;
			System.out.println("Wildcard queries:");
			displayLine = 0;
			System.out.println("1.) findSubStream(*,*,*,String.class, String.class, Long.class)...");
			it =  rkvc.findSubStream(xid, '*', '*', '*',String.class, String.class, Long.class);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
				ar.add(c);
			});
			//-----------------
			System.out.println("Above are the wildcard permutations. Now retrieve those with object references using the "+ar.size());
			System.out.println("wildcard results. NOTE: Concrete object references in findSubStream typically produce null sets.");
			it = null;
			for(j = 0; j < ar.size(); j++) {
				displayLine =0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();	
				System.out.println("2.) findSubStream(<obj>,<obj>,<obj>) using ="+
						arel[0]+",("+arel[0].getClass().getName()+"),"+
						arel[1]+",("+arel[1].getClass().getName()+"),"+
						arel[2]+",("+arel[2].getClass().getName());
				if(it != null)
					rkvc.setStream(it);
				it = rkvc.findSubStream(xid, arel[0],arel[1],arel[2]);
				it.parallel().forEach(e-> {
					Result c = (Result)e;
					displayCtrl();
					if(DISPLAY)
						System.out.println("(2."+j+" of "+ar.size()+") "+displayLine+"="+c);
				});
			}
			for(j = 0; j < ar.size(); j++) {
				displayLine=0;
				//RelatrixHeadsetStream.DEBUG = true;
				Comparable[] arel = ((Result)ar.get(j)).toArray();	
				System.out.println("3.) findSubStream(*,*,<obj>,String.class, String.class) using range="+arel[2]);
				if(it != null)
					rkvc.setStream(it);
				it = rkvc.findSubStream(xid, '*', '*', arel[2], String.class, String.class);
				it.parallel().forEach(e-> {
					Result c = (Result)e;
					displayCtrl();
					if(DISPLAY)
						System.out.println("(3."+j+" of "+ar.size()+") "+displayLine+"="+c);
				});
			}
			for(j = 0; j < ar.size(); j++) {
				displayLine = 0;
				//RelatrixHeadsetStream.DEBUG = true;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("4.) findSubStream(*,<obj>,*, String.class, Long.class) using map="+arel[1]);
				if(it != null)
					rkvc.setStream(it);
				it = rkvc.findSubStream(xid, '*', arel[1], '*',String.class, Long.class);
				it.parallel().forEach(e-> {
					Result c = (Result)e;
					displayCtrl();
					if(DISPLAY)
						System.out.println("(4."+j+" of "+ar.size()+") "+displayLine+"="+c);
				});
			}
			for(j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("5.) findSubStream(<obj>,*,*,String.class, Long.class) using domain="+arel[0]);
				if(it != null)
					rkvc.setStream(it);
				it = rkvc.findSubStream(xid, arel[0], '*', '*', String.class, Long.class);
				it.parallel().forEach(e-> {
					Result c = (Result)e;
					displayCtrl();
					if(DISPLAY)
						System.out.println("(5."+j+" of "+ar.size()+") "+displayLine+"="+c);
				});
			}
			for(j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("6.) findSubStream(*,<obj>,<obj>,String.class) using map="+arel[1]+" range="+arel[2]);
				if(it != null)
					rkvc.setStream(it);
				it = rkvc.findSubStream(xid, '*', arel[1], arel[2], String.class);
				//ar = new ArrayList<Comparable>();
				it.parallel().forEach(e-> {
					Result c = (Result)e;
					displayCtrl();
					if(DISPLAY)
						System.out.println("(6."+j+" of "+ar.size()+") "+displayLine+"="+c);
					//if(ar.size() == 0) ar2.add(c);
				});
			}
			for(j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("7.) findSubStream(<obj>,*,<obj>,String.class) using domain="+arel[0]+", range="+arel[2]);	
				if(it != null)
					rkvc.setStream(it);
				it = rkvc.findSubStream(xid, arel[0], '*', arel[2], String.class);
				//ar = new ArrayList<Comparable>();
				it.parallel().forEach(e-> {
					Result c = (Result)e;
					displayCtrl();
					if(DISPLAY)
						System.out.println("(7."+j+" of "+ar.size()+") "+displayLine+"="+c);
					//if(ar.size() == 1) ar2.add(c);
				});
			}
			for(j = 0; j < ar.size(); j++) {
				displayLine = 0;
				Comparable[] arel = ((Result)ar.get(j)).toArray();
				System.out.println("8.) findSubStream(<obj>,<obj>,*, Long.class) using domain="+arel[0]+", map="+arel[1]);
				if(it != null)
					rkvc.setStream(it);
				it = rkvc.findSubStream(xid, arel[0], arel[1], '*',Long.class);
				it.parallel().forEach(e-> {
					Result c = (Result)e;
					displayCtrl();
					if(DISPLAY)
						System.out.println("(8."+j+" of "+ar.size()+") "+displayLine+"="+c);
					//if(ar.size() == 2) ar2.add(c);
				});
			}
			//
			// ---------- hi/lo test
			it = null;	
			Long lo = (long)(max/(div*2));
			Long hi = (long)(max/div);
			displayLine =0;
			String fkey1 = key + String.format(uniqKeyFmt, lo);
			String fkey2 = key + String.format(uniqKeyFmt, hi);
			System.out.println("9.) findSubStream(*,*,*,<obj>,<obj>,String.class,<obj>,<obj>) using domain="+fkey1+" to "+fkey2+" map=String.class "+" range="+lo+" to "+hi);		
			it = rkvc.findSubStream(xid, '*', '*', '*',fkey1,fkey2,String.class,lo,hi);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			});
			lo+=(long)div;
			hi+=(long)div;
			displayLine = 0;
			if(it != null)
				rkvc.setStream(it);
			fkey1 = key + String.format(uniqKeyFmt, lo);
			fkey2 = key + String.format(uniqKeyFmt, hi);
			System.out.println("10.) findSubStream(*,*,*,<obj>,<obj>,String.class,<obj>,<obj>) using domain="+fkey1+" to "+fkey2+" map=String.class"+" range="+lo+" to "+hi);		
			it = rkvc.findSubStream(xid, '*', '*', '*',fkey1,fkey2,String.class,lo,hi);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			});
			lo+=(long)div;
			hi+=(long)div;
			displayLine = 0;
			if(it != null)
				rkvc.setStream(it);
			fkey1 = key + String.format(uniqKeyFmt, lo);
			fkey2 = key + String.format(uniqKeyFmt, hi);
			System.out.println("11.) findSubStream(*,*,*,<obj>,<obj>,String.class,<obj>,<obj>) using domain="+fkey1+" to "+fkey2+" map=String.class"+" range="+lo+" to "+hi);		
			it = rkvc.findSubStream(xid, '*', '*', '*',fkey1,fkey2,String.class,lo,hi);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			});	
			lo = (long)(max/(div*2));
			hi = (long)(max/div);
			displayLine =0;
			System.out.println("12.) findSubStream(*,*,*,String.class,String.class,<obj>,<obj>) using domain=String.class map=String.class "+" range="+lo+" to "+hi);		
			it = rkvc.findSubStream(xid, '*', '*', '*',String.class,String.class,lo,hi);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			});
			lo+=(long)div;
			hi+=(long)div;
			displayLine = 0;
			if(it != null)
				rkvc.setStream(it);
			System.out.println("13.) findSubStream(*,*,*,String.class,String.class,<obj>,<obj>) using domain=String.class map=String.class"+" range="+lo+" to "+hi);		
			it = rkvc.findSubStream(xid, '*', '*', '*',String.class,String.class,lo,hi);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			});
			lo+=(long)div;
			hi+=(long)div;
			displayLine = 0;
			if(it != null)
				rkvc.setStream(it);
			System.out.println("14.) findSubStream(*,*,*,<obj>,<obj>,<obj>,<obj>) using domain=String.class map=String.class"+" range="+lo+" to "+hi);		
			it = rkvc.findSubStream(xid, '*', '*', '*',String.class,String.class,lo,hi);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			});
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
			Stream it = rkvc.findStream(xid, '*','*','*');
			timx = System.currentTimeMillis();
			i = 0;
			it.forEach(e-> {
				Result c = (Result)e;
				Relation dmr = (Relation)((Result)e).get(0);
				try {
					rkvc.remove(xid, dmr.getDomain(), dmr.getMap());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				++i;
				if((System.currentTimeMillis()-timx) > 1000) {
					System.out.println("deleting "+i+" "+e);
					timx = System.currentTimeMillis();
				}
			});
			Stream<?> its = rkvc.findStream(xid, '*','*','*');
			it.forEach(e-> {
				Result c = (Result)e;
				//System.out.println(i+"="+nex);
				System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+e);
			});
			long siz = rkvc.size(xid);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySetStream(xid, Relation.class);
			it.forEach(e-> {
				Result nex = (Result)e;
				System.out.println("Relation:"+nex);
			});
			siz = rkvc.size(xid);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySetStream(xid, DomainRangeMap.class);
			it.forEach(e-> {
				Result c = (Result)e;
				System.out.println("DomainRangeMap:"+e);
			});
			siz = rkvc.size(xid);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
			}

			it = rkvc.entrySetStream(xid, MapDomainRange.class);
			it.forEach(e-> {
				Result c = (Result)e;
				System.out.println("MapDomainRange:"+e);
			});
			siz = rkvc.size(xid, MapDomainRange.class);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
			}

			it = rkvc.entrySetStream(xid, MapRangeDomain.class);
			it.forEach(e-> {
				Result c = (Result)e;
				System.out.println("MapRangeDomain:"+e);
			});
			siz = rkvc.size(xid, MapRangeDomain.class);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySetStream(xid, RangeDomainMap.class);
			it.forEach(e-> {
				Result c = (Result)e;
				System.out.println("RangeDomainMap:"+e);
			});
			siz = rkvc.size(xid, RangeDomainMap.class);
			if(siz > 0) {
				System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
				throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
			}
			it = rkvc.entrySetStream(xid, RangeMapDomain.class);
			it.forEach(e-> {
				Result c = (Result)e;
				System.out.println("RangeMapDomain:"+e);
			});
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
			System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
		}

	
}
