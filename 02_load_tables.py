#!/usr/bin/python

from   bg_utils.mysql   import  *


#########################################
def main():

    #home = "/Users/ivana"
    home = "/home/ivana"
    
    db     = connect_to_mysql()
    cursor = db.cursor()
    db_name = 'biogrid'
    switch_to_db(cursor, db_name)


    species =  [ "Homo_sapiens", "Mus_musculus", "Rattus_norvegicus"]

    version = '3.4.133'

    for spec in species:
        table = spec.lower()
        print table
        qry  = " load data infile "
        qry += "'%s/databases/biogrid/clean/BIOGRID-ORGANISM-%s-%s.tab2.txt' " % (home, spec, version)
        qry += " into table %s ignore 1 lines" % table
        rows = search_db(cursor, qry)
        print qry
        print rows

    cursor.close()
    db.close()


#########################################
if __name__ == '__main__':
    main()
