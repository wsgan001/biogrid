#!/usr/bin/python

from   bg_utils.mysql   import  *


#########################################
def main():

    db     = connect_to_mysql()
    cursor = db.cursor()
    db_name = 'biogrid'
    switch_to_db(cursor, db_name)

    species =  ["Bos_taurus", "Canis_familiaris", "Cavia_porcellus", "Chlorocebus_sabaeus",
                "Cricetulus_griseus", "Equus_caballus", "Homo_sapiens", "Macaca_mulatta", "Mus_musculus",
                "Oryctolagus_cuniculus", "Pan_troglodytes", "Rattus_norvegicus", "Sus_scrofa"]

    version = '3.4.126'

    for spec in species:
        table = spec.lower()
        print table
        qry  = " load data infile "
        qry += "'/Users/ivana/databases/biogrid/clean/BIOGRID-ORGANISM-%s-%s.tab2.txt' " % (spec, version)
        qry += " into table %s ignore 1 lines" % table
        rows = search_db(cursor, qry)
        print qry
        print rows

    cursor.close()
    db.close()


#########################################
if __name__ == '__main__':
    main()
