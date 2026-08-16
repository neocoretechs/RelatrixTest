package com.neocoretechs.relatrix.test.server.transaction;

import java.util.Optional;
import java.util.stream.Stream;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.rocksack.TransactionId;
import com.neocoretechs.relatrix.client.RelatrixClientTransaction;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findStream retrieval using the client to a remote {@link com.neocoretechs.relatrix.server.RelatrixTransactionServer}.
 * NOTES:
 * program arguments are local_node remote_node remote_port_for_database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 */
public class TransactionBatteryRelatrix {
	public static boolean DEBUG = false;
	static String key = "This is a test"; // holds the base random key string for tests
	static String uniqKeyFmt = "%0100d"; // base + counter formatted with this gives equal length strings for canonical ordering
	static int min = 0;
	static int max = 2000;
	public static String DATABASE;
	//static RelatrixTransaction session = RelatrixTransaction.getInstance();
	static RelatrixClientTransaction session = null;

	/**
	* Analysis test fixture
	*/
	public static void main(String[] argv) throws Exception {
		DATABASE = argv[0];
		if(argv.length > 2 && argv[2].equals("max")) {
			System.out.println("Setting max items to "+argv[3]);
			max = Integer.parseInt(argv[3]);
		}
		session = new RelatrixClientTransaction(argv[0], Integer.parseInt(argv[1]) );
		TransactionId xid = session.getTransactionId();
		System.out.println("Test battery got trans Id:"+xid);
		if(session.size(xid) == 0) {
			battery1(xid);
			session.commit(xid);
		}	
		battery2(xid);
		session.endTransaction(xid);
		System.out.println("TEST BATTERY COMPLETE.");	
		System.exit(0);
	}
	/**
	 * Loads up on keys
	 */
	public static void battery1(TransactionId xid) throws Exception {
		System.out.println("Battery1 "+xid);
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		int i = min;
		for(; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			Relation dmr = session.store(xid, fkey, "Has unit", Long.valueOf(i));
			System.out.println(i+".)"+dmr);
			Relation dmr2 = session.store(xid, dmr ,"has identity",Long.valueOf(i));
			System.out.println(i+".)"+dmr2);
			++recs;
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	
	public static void battery2(TransactionId xid) throws Exception {
		System.out.println("Battery2 "+xid);
		long tims = System.currentTimeMillis();
		int recs = 0;
		String fkey = null;
		Stream<?> s1 = null;
		Stream<?> s2 = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
				if(s1 != null)
					session.setStream(s1);
				s1 = session.findStream(xid, fkey, "Has unit", Long.valueOf(i));
				Optional<?> o =  s1.findFirst();
				if(o.isPresent()) {
					if(s2 != null)
						session.setStream(s2);
					s2 = session.findStream(xid, ((Result)o.get()).get(), '*', '*');
					Optional<?> p = s2.findFirst();
					if(p.isPresent()) {
						Result c = (Result) p.get();
						if(!(c.get() instanceof AbstractRelation))
							System.out.println(c.get().getClass()+" isnt AbstractRelation; value:"+c.get());
						else {
							// main morphism
							Relation m = (Relation) c.get();
							if(!(m.getDomain() instanceof AbstractRelation)) 
								System.out.println(m.getDomain().getClass()+" isnt AbstractRelation; value:"+m);
							else {
								// morphism in domain "has unit"
								Relation d = (Relation) m.getDomain();
								if(!(d.getDomain() instanceof String))
									System.out.println(d.getDomain().getClass()+" domain isnt String; value:"+d);
								else {
									if(!d.getDomain().equals(fkey))
										System.out.println("Domain doesnt match "+fkey);
								}
								if(!(d.getMap() instanceof String))
									System.out.println(d.getMap().getClass()+" map isnt String; value:"+d);
								else {
									if(!d.getMap().equals("Has unit"))
										System.out.println("Map doesnt match 'Has unit'"+d);
								}
								if(!(d.getRange() instanceof Long))
									System.out.println(d.getRange().getClass()+" range isnt Long; value:"+d);
								else {
									if(!d.getRange().equals(Long.valueOf(i)))
										System.out.println("Range doesnt match "+i);
								}
								// that takes care of morphism within morphism, now check remainder of composite morphism
								if(!(m.getMap() instanceof String))
									System.out.println(m.getMap().getClass()+" map isnt String; value:"+m);
								else {
									if(!m.getMap().equals("has identity"))
										System.out.println("Map doesnt match 'has identity'"+m);
								}
							}
						}
					} else
						System.out.println("Failed to find any result set for "+o.get());	
				} else
					System.out.println("Failed to find any result set for domain:"+fkey);
				++recs;
		}
		System.out.println("BATTERY2 verification SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. retrieved "+recs);
	}

}
