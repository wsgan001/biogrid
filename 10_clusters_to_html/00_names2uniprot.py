#!/usr/bin/python


import MySQLdb
import os, sys
from   bg_utils.mysql   import  *

#########################################
def main():
    db     = connect_to_mysql()
    cursor = db.cursor()

    switch_to_db (cursor, 'baseline')
    species = 'human'
    inf = open('/Users/ivana/scratch/' + species + '_clusters.txt', 'r')
    outf =  open('/Users/ivana/scratch/' + species + '_names2uniprot.txt', 'w')
    written = []
    for line in inf.readlines():
        fields = line.split()
        if len(fields) < 3: continue
        if fields[0] == 'e': continue # edge entry
        name = fields[1]
        if name in written: continue
        qry = "select uniprot_ids from hgnc_id_translation "
        qry += "where approved_symbol='%s'" % name
        rows = search_db(cursor, qry);
        if rows:
            print >> outf, name, rows[0][0]
        else:
            print >> outf, name, 'not found'
        written.append(name)

    inf.close()
    outf.close()
    cursor.close()
    db.close()

 

#########################################
if __name__ == '__main__':
    main()
