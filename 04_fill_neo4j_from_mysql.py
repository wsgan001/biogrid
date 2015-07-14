#!/usr/bin/python 

# restarting neo4j database (clearing the old data):
# ./bin/neo4j stop && rm -rf data && mkdir data && ./bin/neo4j start
# but before you do make sure to turn off the authorization shit (see below)

from py2neo import neo4j, Node, rel
from bg_utils.mysql   import  *
import os, shutil


neo4j_home = "/Users/ivana/databases/neo4j/data"


#################################################################
def make_neo4jdb(cursor, species):

    system_type = 'physical'

    qry  = "select official_symbol_A,  official_symbol_B, pubmed_id "
    qry += "from %s " %  species
    qry += "where throughput='Low Throughput' "
    qry += " and experimental_system_type = '%s'" % system_type
    rows = search_db (cursor, qry)
    if not rows:
        rows = search_db (cursor, qry, verbose=True)
    print "done reading biogrid (mysql) for %s" % species
    print "number of documented interactions:", len(rows)

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
    print "number of unique interactions (some with multiple pubmed entries): ", len(interactions.keys())

    #############################################################
    # after they took out batch from pu2neo, I do not know how to do the
    # database filling efficiently - so just output as csv, and try slurping it in
    outf = open ("names.csv", "w")
    print >> outf, 'symbol'
    for symbol in symbols:
        print >> outf, symbol.upper()
    outf.close()

    outf = open ("pubmed_ids.csv", "w")
    print >> outf, 'symbol_1,symbol_2,pubmed_ids'
    for label in interactions.keys():
        [official_symbol_A, official_symbol_B] = label.split (" ")
        pubmed_ids = ";".join(interactions[label])
        print >> outf, "%s,%s,%s" % (official_symbol_A, official_symbol_B, pubmed_ids)
    outf.close()

    #############################################################
    # fill neo4j database
    graph_db = neo4j.Graph()
    # some bs about not authorized
    # "Alternatively, authentication can be disabled completely by editing the value of
    # the dbms.security.authorization_enabled
    #setting in the conf/neo4j-server.properties file."
    infile = os.getcwd() + '/names.csv'
    qry = "LOAD CSV WITH HEADERS FROM 'file://%s' " % infile
    qry +="AS line CREATE (g:gene {official_symbol: line.symbol} )"
    ret = graph_db.cypher.execute(qry)
    print "qry:", qry
    print "ret:", ret

    qry = "CREATE CONSTRAINT ON (g:gene) ASSERT g.official_symbol IS UNIQUE"
    ret = graph_db.cypher.execute(qry)
    print "qry:", qry
    print "ret:", ret

    infile = os.getcwd() + '/pubmed_ids.csv'
    qry = "USING PERIODIC COMMIT 500 "
    qry += "LOAD CSV WITH HEADERS FROM  'file://%s' " % infile
    qry += "AS line MATCH (g1:gene {official_symbol: line.symbol_1}), (g2:gene {official_symbol: line.symbol_2}) "
    qry += "CREATE (g1)-[:INTERACTS { pubmed_ids: line.pubmed_ids }]->(g2) "
    ret = graph_db.cypher.execute(qry)
    print "qry:", qry
    print "ret:", ret

    print "moving", neo4j_home+"/graph.db", "to", neo4j_home+"/"+species+"_graph.db"
    shutil.move(neo4j_home+"/graph.db", neo4j_home+"/"+species+"_graph.db")


###############################################################
def main():
    #############################################################
    # fish interactors from mysql database
    db     = connect_to_mysql()
    mysql_cursor = db.cursor()
    switch_to_db (mysql_cursor, 'biogrid')

    #species =  ["bos_taurus", "canis_familiaris", "cavia_porcellus", "chlorocebus_sabaeus",
    #            "cricetulus_griseus", "equus_caballus", "homo_sapiens", "macaca_mulatta", "mus_musculus",
    #            "oryctolagus_cuniculus", "pan_troglodytes", "rattus_norvegicus", "sus_scrofa"]
    # I cannot really loop because I am moving the database dir each time
    species =  ["rattus_norvegicus"]
    for spec in species:
        make_neo4jdb(mysql_cursor, spec)

    mysql_cursor.close()
    db.close()




###############################################################
if __name__ == "__main__": 
    main()
