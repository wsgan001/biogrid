#!/usr/bin/python

from   bg_utils.mysql   import  *

#########################################
def make_table (cursor, table):
    qry = ""
    qry += "  CREATE TABLE  %s (" % table
    qry += "  biogrid_id int(10) unsigned DEFAULT NULL, "
    qry += "  entrez_gene_A int(10) unsigned DEFAULT NULL, "
    qry += "  entrez_gene_B int(10) unsigned DEFAULT NULL, "
    qry += "  biogrid_id_A int(10) unsigned DEFAULT NULL, "
    qry += "  biogrid_id_B int(10) unsigned DEFAULT NULL, "
    qry += "  systematic_name_A varchar(40)   DEFAULT NULL, "
    qry += "  systematic_name_B varchar(40)   DEFAULT NULL, "
    qry += "  official_symbol_A varchar(40)   DEFAULT NULL, "
    qry += "  official_symbol_B varchar(40)   DEFAULT NULL, "
    qry += "  synonyms_A  varchar(500)   DEFAULT NULL, "
    qry += "  synonyms_B  varchar(500)   DEFAULT NULL, "
    qry += "  experimental_system  varchar(100)   DEFAULT NULL,  "
    qry += "  experimental_system_type  varchar(100)   DEFAULT NULL, "
    qry += "  author  blob  DEFAULT NULL, "
    qry += "  pubmed_id int(10) unsigned DEFAULT NULL, "
    qry += "  organism_A int(10) unsigned DEFAULT NULL, "
    qry += "  organism_B int(10) unsigned DEFAULT NULL, "
    qry += "  throughput  varchar(40)   DEFAULT NULL, "
    qry += "  score  varchar(40)    DEFAULT NULL, "
    qry += "  modification varchar(100)   DEFAULT NULL, "
    qry += "  phenotypes  blob   DEFAULT NULL, "
    qry += "  qualifications blob   DEFAULT NULL, "
    qry +=   "tags  varchar(10)   DEFAULT NULL, "
    qry +=   "source_database varchar(40)   DEFAULT NULL, "
    qry += "  PRIMARY KEY (biogrid_id), "
    qry += "  KEY gene_a_idx (entrez_gene_A), "
    qry += "  KEY gene_b_idx (entrez_gene_B) "
    qry += ") ENGINE=MyISAM; "

    rows = search_db(cursor, qry)
    print qry
    print rows


#########################################
def main():

    db     = connect_to_mysql()
    cursor = db.cursor()
    db_name = 'biogrid'
    switch_to_db(cursor, db_name)
    species =  ["Bos_taurus", "Canis_familiaris", "Cavia_porcellus", "Chlorocebus_sabaeus",
                "Cricetulus_griseus", "Equus_caballus", "Homo_sapiens", "Macaca_mulatta", "Mus_musculus",
                "Oryctolagus_cuniculus", "Pan_troglodytes", "Rattus_norvegicus", "Sus_scrofa"]

    for spec in species:
        table = spec.lower()
        print table
        if ( check_table_exists (cursor, db_name, table)):
            print table, " found in ", db_name
            # if you really want to start from scratch, uncomment
            #qry = "drop table %s "  % table
            #rows = search_db(cursor, qry)

        make_table (cursor, table)

    cursor.close()
    db.close()


#########################################
if __name__ == '__main__':
    main()
