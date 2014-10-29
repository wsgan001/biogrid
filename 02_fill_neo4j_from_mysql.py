#!/usr/bin/python 

# restarting neo4j database (clearing the old data):
# ./bin/neo4j stop && rm -rf data && mkdir data && ./bin/neo4j start

import MySQLdb
import sys 
from py2neo import neo4j, node, rel 
from bg_utils.mysql   import  *


#################################################################
def main():

    #############################################################
    # fish interactors from mysql database
    db     = connect_to_mysql()
    cursor = db.cursor()
    switch_to_db (cursor, 'biogrid')

    system_type = 'physical'

    qry  = "select official_symbol_A,  official_symbol_B, pubmed_id "
    qry += "from homo_sapiens "
    qry += "where throughput='Low Throughput' "
    qry += " and experimental_system_type = '%s'" % system_type
    rows = search_db (cursor, qry)
    if not rows:
        rows = search_db (cursor, qry, verbose=True)
    print "done reading mysql - number of hits:", len(rows)

    #############################################################
    # group pubmed ids
    interactions = {}
    symbols  = []
    for row in rows:
        if not len(row) == 3: 
            print row
            exit(1)
        [official_symbol_A,  official_symbol_B, pubmed_id ] = row
        official_symbol_A = official_symbol_A.upper()
        official_symbol_B = official_symbol_B.upper()
        if official_symbol_A == official_symbol_B: continue

        label = " ".join( sorted([official_symbol_A, official_symbol_B]))
        pubmed_id = str(pubmed_id)
        if not interactions.has_key(label): interactions[label] = []
        if not pubmed_id in interactions[label]:
            interactions[label].append(pubmed_id)
        if not official_symbol_A in symbols: symbols.append(official_symbol_A)
        if not official_symbol_B in symbols: symbols.append(official_symbol_B)
    print "done reading biogrid"
    #############################################################
    # check that the official symbol corresponds to a nuclear protein 
    if False:
         nuclear = {}
         for  symbol in symbols:
              nuclear[symbol] = is_nuclear(cursor, symbol)
         print "done checking nuclear"
    cursor.close()
    db.close()

    #############################################################
    # fill neo4j database
    graph_db = neo4j.GraphDatabaseService() 
    batch    = neo4j.WriteBatch(graph_db)

    #############################################################
    # create  the nodes
    if 1:
        ct = 0
        for  symbol in symbols:
            batch.get_or_create_indexed_node("symbol_idx", "official_symbol", symbol, 
                                             {"official_symbol": symbol})
            ct +=1;
            if not ct%1000:
                print ct, "...", 
                batch.submit()
                batch.clear()
                print "done"
        batch.submit()

        print "done with batch submitting of the names"
        batch.clear()
        
    #############################################################
    # create  relationships
    ct = 0
    for label in interactions.keys():
        [official_symbol_A, official_symbol_B] = label.split (" ")
        #if not nuclear[official_symbol_A] or not nuclear[official_symbol_B]: continue
        a = graph_db.get_or_create_indexed_node("symbol_idx", "official_symbol", official_symbol_A)
        b = graph_db.get_or_create_indexed_node("symbol_idx", "official_symbol", official_symbol_B)
        pubmed_ids = ";".join(interactions[label])
        batch.create(rel(a, system_type , b,  pubmed_ids=pubmed_ids))
        ct +=1;
        if not ct%1000:
            print ct, "...", 
            batch.submit()
            batch.clear()
            print "done"
    batch.submit()

###############################################################
if __name__ == "__main__": 
    main()
