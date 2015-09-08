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

    qry  = "select official_symbol_A,  official_symbol_B, experimental_system_type, throughput, pubmed_id "
    qry += "from %s " %  species
    #qry += "where throughput='Low Throughput' "

    rows = search_db (cursor, qry)
    if not rows:
        rows = search_db (cursor, qry, verbose=True)
    print "done reading biogrid (mysql) for %s" % species
    print "number of documented interactions:", len(rows)

    #############################################################
    # group pubmed ids
    interactions = {}
    symbols  = []
    exp_labels = []
    for row in rows:
        if not len(row) == 5:
            print row
            exit(1)
        [official_symbol_A,  official_symbol_B, experimental_system_type, throughput, pubmed_id] = row
        official_symbol_A = official_symbol_A.upper()
        official_symbol_B = official_symbol_B.upper()
        if official_symbol_A == official_symbol_B: continue

        label = " ".join( sorted([official_symbol_A, official_symbol_B]))
        exp_label = experimental_system_type.upper()
        if throughput=="Low Throughput": exp_label += "_LOW"
        elif throughput=="High Throughput": exp_label += "_HI"
        else: exp_label += "_HI_LOW"
        if not exp_label in exp_labels: exp_labels.append(exp_label)
        pubmed_id = str(pubmed_id)
        if not interactions.has_key(label): interactions[label] = {}
        if not interactions[label].has_key(exp_label): interactions[label][exp_label] = []
        if not pubmed_id in interactions[label][exp_label]:
            interactions[label][exp_label].append(pubmed_id)
        if not official_symbol_A in symbols: symbols.append(official_symbol_A.upper())
        if not official_symbol_B in symbols: symbols.append(official_symbol_B.upper())

    count = {}
    for exp_label in exp_labels:
        count[exp_label] = 0
        for label in interactions.keys():
            if interactions[label].has_key(exp_label):
                count[exp_label] += 1
    for exp_label in exp_labels:
        print "number of unique ", exp_label, \
            "interactions (some with multiple pubmed entries): ", count[exp_label]

    #############################################################
    # after they took out batch from pu2neo, I do not know how to do the
    # database filling efficiently - so just output as csv, and try slurping it in
    outf = open ("names.csv", "w")
    print >> outf, 'symbol'
    for symbol in symbols:
        print >> outf, symbol.upper()
    outf.close()

    # trying to sort out relationships using neo4j's cypher things get too compplicated
    outf = {}
    for exp_label in exp_labels:
        outf[exp_label] = open ("pubmed_ids."+ exp_label.lower()+".csv", "w")
        print >> outf[exp_label], 'symbol_1,symbol_2,pubmed_ids'

    for label in interactions.keys():
        [official_symbol_A, official_symbol_B] = label.split (" ")
        for exp_label in interactions[label].keys():
            pubmed_ids = ";".join(interactions[label][exp_label])
            print >> outf[exp_label], "%s,%s,%s" % (official_symbol_A, official_symbol_B,pubmed_ids)

    for exp_label in exp_labels:
        outf[exp_label].close()


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

    for exp_label in exp_labels:
        infile = os.getcwd() + "/pubmed_ids."+ exp_label.lower()+".csv"
        qry = "USING PERIODIC COMMIT 500 "
        qry += "LOAD CSV WITH HEADERS FROM  'file://%s' " % infile
        qry += "AS line MATCH (g1:gene {official_symbol: line.symbol_1}), (g2:gene {official_symbol: line.symbol_2}) "
        qry += "CREATE (g1)-[:%s {pubmed_ids: line.pubmed_ids}]->(g2) " % exp_label
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
    species =  ["homo_sapiens"]
    for spec in species:
        make_neo4jdb(mysql_cursor, spec)

    mysql_cursor.close()
    db.close()




###############################################################
if __name__ == "__main__": 
    main()
