package com.neocoretechs.relatrix.test.server.transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.client.RelatrixClientTransaction;
import com.neocoretechs.rocksack.TransactionId;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findStream retrieval using the client to a remote {@link com.neocoretechs.relatrix.server.RelatrixTransactionServer}.
 * NOTES:
 * program arguments are remote_node remote_port_for_database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 *
 */
public class StreamRetrievalBatteryTransaction0 {
	public static boolean DEBUG = false;
	private static RelatrixClientTransaction rkvc;
	public static int displayLinesOn[]= {0,1000,5000,9990,15000,20000,30000,40000,50000,60000,70000,80000,90000,99000};
	public static int displayLinesOff[]= {100,1100,5100,9999,15999,20999,30999,40999,50999,60999,70999,80999,90999,100000};
	public static int displayLine = 0;
	public static int displayLineCtr = 0;
	public static long displayTimer = 0;
	public static int min = 0;
	public static int max = 100;
	static String key = "This is a test"; 
	static String uniqKeyFmt = "%0100d";
	private static boolean DISPLAY = false;
	private static boolean DISPLAYALL = true;
	private static long timx;
	private static int i,j;
	private static TransactionId xid;

	/**
	*/
	public static void main(String[] argv) throws Exception {
		if(argv.length < 3) {
			System.out.println("Usage: <remoteNode> <remotePort> [init]");
		}
		rkvc = new RelatrixClientTransaction(argv[0], Integer.parseInt(argv[1]));
		xid = rkvc.getTransactionId();
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		if(argv.length == 3 && argv[3].equals("init")) {
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
			dmr = rkvc.store(xid, fkey, "Has unit", Long.valueOf(i));
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
		ArrayList<Result> ar = new ArrayList<Result>(); // range
	
		Stream<?> it = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;
		System.out.println("1.) findStream(*,*,*)...");
		it = rkvc.findStream(xid, '*', '*', '*');
		it.parallel().forEach(e-> {
			Result c = (Result)e;
			ar.add(c);
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println("(1.) "+displayLine+"="+c);
		});
		displayLine = 0;

		System.out.println("----------");
		System.out.println("Above are all the wildcard permutations. Now retrieve those with object references from array size:"+ar.size());
		it = null;
		for(j = 0; j < ar.size(); j++) {
			displayLine = 0;
			Comparable[] arel = ((Result)ar.get(j)).toArray();
			System.out.println("2."+j+") findStream(<obj>,<obj>,<obj>) using ="+
					arel[0]+",("+arel[0].getClass().getName()+"),"+
					arel[1]+",("+arel[1].getClass().getName()+"),"+
					arel[2]+",("+arel[2].getClass().getName());
			if(it != null)
				rkvc.setStream(it);
			it = rkvc.findStream(xid, arel[0], arel[1], arel[2]);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(2."+j+" of "+ar.size()+") "+displayLine+"="+c);
			});
		}
		it = null;
		for(j = 0; j < ar.size(); j++) {
			displayLine=0;
			Comparable[] arel = ((Result)ar.get(j)).toArray();
			System.out.println("3."+j+") findStream(*,*,<obj>) using range="+arel[2]);
			if(it != null)
				rkvc.setStream(it);
			it = rkvc.findStream(xid, '*', '*', arel[2]);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(3."+j+" of "+ar.size()+") "+displayLine+"="+c);
			});
		}
		it = null;
		for(j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetStream.DEBUG = true;
			Comparable[] arel = ((Result)ar.get(j)).toArray();
			System.out.println("4."+j+") findStream(*,<obj>,*) using map="+arel[1]);
			if(it != null)
				rkvc.setStream(it);
			it = rkvc.findStream(xid, '*', arel[1], '*');
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(4."+j+" of "+ar.size()+") "+displayLine+"="+c);
			});
		}
		
		it = null;
		for(j = 0; j < ar.size(); j++) {
			displayLine = 0;
			Comparable[] arel = ((Result)ar.get(j)).toArray();
			System.out.println("5."+j+") findStream(<obj>,*,*) using domain="+arel[0]);
			if(it != null)
				rkvc.setStream(it);
			it = rkvc.findStream(xid, arel[0], '*', '*');
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(5."+j+" of "+ar.size()+") "+displayLine+"="+c);
			});
		}
		// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
		it = null;
		for(j = 0; j < ar.size(); j++) {
			displayLine = 0;
			Comparable[] arel = ((Result)ar.get(j)).toArray();
			System.out.println("6."+j+") findStream(*,<obj>,<obj>) using map="+arel[1]+" range="+arel[2]);
			if(it != null)
				rkvc.setStream(it);
			it = rkvc.findStream(xid, '*', arel[1], arel[2]);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(6."+j+" of "+ar.size()+") "+displayLine+"="+c);
			});
		}
		it = null;
		for(j = 0; j < ar.size(); j++) {
			displayLine = 0;
			Comparable[] arel = ((Result)ar.get(j)).toArray();
			System.out.println("7."+j+") findStream(<obj>,*,<obj>) using ="+arel[0]+", "+arel[2]);
			if(it != null)
				rkvc.setStream(it);
			it = rkvc.findStream(xid, arel[0], '*', arel[2]);
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(7."+j+" of "+ar.size()+") "+displayLine+"="+c);
			});
		}
		it = null;
		for(j = 0; j < ar.size(); j++) {
			displayLine=0;
			Comparable[] arel = ((Result)ar.get(j)).toArray();
			System.out.println("8."+j+") findStream(<obj>,<obj>,*) using domain="+arel[0]+", map="+arel[1]);
			if(it != null)
				rkvc.setStream(it);
			it = rkvc.findStream(xid, arel[0], arel[1], '*');
			it.parallel().forEach(e-> {
				Result c = (Result)e;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println("(8."+j+" of "+ar.size()+") "+displayLine+"="+c);
			});
		}

		System.out.println("ServerRetrievalBattery0 SUCCESS in "+(System.currentTimeMillis()-tims));
	}
	/**
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		Stream<?> it = rkvc.findStream(xid, '*','*','*');
		timx = System.currentTimeMillis();
		it.parallel().forEach(e-> {
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
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}

}

