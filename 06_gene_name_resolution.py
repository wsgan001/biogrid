#!/usr/bin/python
# needed the index on hugoSymbol for this to work with any speed:
# create index hugo_idx on somatic_mutations (hugoSymbol);

import sys, os
import MySQLdb
from   bg_utils.mysql   import  *

#########################################
def find_ensembl_id (cursor, gene):
    
    ensembl_id = ""
    comment    = ""

    if gene=='Uknown': return [ensembl_id, 'unk']

    if gene[:4] =='ENSG': 
        ensembl_id = gene
        return [ensembl_id, 'ensembl']

    # look up ens id directly
    qry  = "select distinct(ensembl_gene_id) from  baseline.hgnc_id_translation  where approved_symbol='%s'" % gene
    rows = search_db (cursor, qry)
    if rows:
        ensembl_ids = [row[0] for row in rows if row[0] > 0]
        no_hits  = len(ensembl_ids)
        if no_hits == 1:  
            ensembl_id = ensembl_ids[0].replace(' ','')
            if ensembl_id:  return [ensembl_id, 'ok']

    

    # if this didn't work, go to entrez
    qry  = "select distinct(entrez_gene_id) from  somatic_mutations  where hugo_symbol='%s'" % gene
    rows = search_db (cursor, qry)
    if rows:
        entrez_ids = [row[0] for row in rows if row[0] > 0]
        no_hits  = len(entrez_ids)
        if no_hits == 1: 
            #print gene, "single entrez id:", entrez_ids[0]
            qry  = "select distinct(ensembl_gene_id) from baseline.hgnc_id_translation  "
            qry += "where entrez_gene_id='%s'" % entrez_ids[0]
            rows = search_db (cursor, qry)
            if rows:
                ensembl_ids = [row[0] for row in rows if row[0] > 0]
                no_hits  = len(ensembl_ids)
                if no_hits == 1:   
                    ensembl_id = ensembl_ids[0].replace(' ','')
                    if ensembl_id:  return [ensembl_id, 'from hgnc_id_translation via entrez_gene_id']
                # this apparently does not happen
                #print gene, entrez_id, 'multiple ensembl', ensembl_ids

    

    # if entrez failed, try older hugo names
    for column in ['previous_symbols', 'synonyms']:
        qry = "select approved_symbol, %s, ensembl_gene_id  from baseline.hgnc_id_translation " % column
        qry += "where %s like '%%%s%%'" % (column,gene)
        rows = search_db (cursor, qry)
        if  rows:
            for row in rows:
                # check that the match is exact
                previous_symbols = row[1].replace(' ','').split(',')
                exact = [ x  for x in previous_symbols if x==gene]
                if exact:
                    hugo_id    = row[0]
                    ensembl_id = row[2].replace(' ','')
                    if ensembl_id:
                        comment =  'in hgnc_id_translation ' + column + ' for ' + hugo_id
                        return [ensembl_id, comment]

    # in the case of failure, check out ncbi translation table
    qry  = "select  symbol, synonyms, db_xrefs from  baseline.ncbi_id_translation  where  symbol='%s'" % gene
    rows = search_db (cursor, qry)
    if rows:
        db_xrefs = [row[2] for row in rows]
        ensembl_ids = []
        for db_xref in db_xrefs:
            ensembl =[ x for x in  db_xref.split('|') if 'Ensembl' in x]
            for ens in ensembl:
                ensembl_ids.append (ens.split(':')[1])
        if len(ensembl_ids)==1: 
            ensembl_id = ensembl_ids[0].replace(' ','')                    
            return [ensembl_id, "symbol in ncbi_id_translation"]

    # locus tag?
    qry  = "select  symbol, synonyms, db_xrefs from  baseline.ncbi_id_translation  where  locus_tag='%s'" % gene
    rows = search_db (cursor, qry)   
    if rows:
        db_xrefs = [row[2] for row in rows]
        ensembl_ids = []
        for db_xref in db_xrefs:
            ensembl =[ x for x in  db_xref.split('|') if 'Ensembl' in x]
            for ens in ensembl:
                ensembl_ids.append (ens.split(':')[1])
        if len(ensembl_ids)==1: 
            ensembl_id = ensembl_ids[0].replace(' ','')                    
            return [ensembl_id, "locus_tag in ncbi_id_translation"]
    

    # synonyms?
    qry  = "select  symbol, synonyms, db_xrefs from  baseline.ncbi_id_translation  where synonyms like '%%%s%%'" % gene
    rows = search_db (cursor, qry)
    if rows:
        ensembl_ids = []
        for row in rows:
            synonyms = row[1].replace(' ','').split('|')
            # check that the match is exact
            exact = [ x  for x in synonyms if x==gene]
            if exact:
                db_xrefs = [row[2] for row in rows]
                for db_xref in db_xrefs:
                    ensembl =[ x for x in  db_xref.split('|') if 'Ensembl' in x]
                    for ens in ensembl:
                        ensembl_ids.append (ens.split(':')[1])

        if len(ensembl_ids)==1: 
            ensembl_id = ensembl_ids[0]
            return [ensembl_id, "in synonyms  for " + row[0] + " in  ncbi_id_translation"]

    # try uniprot resolution table 
    fields = gene.split('.')
    fields.pop()
    gene = '.'.join(fields)
    qry  = "select * from  baseline.uniprot_id_translation  where other_db_id='%s'" % gene
    rows = search_db (cursor, qry)
    if rows:
        [uniprot_id, other_db, other_db_id ] = rows[0]
        qry  = "select * from  baseline.uniprot_id_translation  where uniprot_id='%s' " % uniprot_id
        qry += "and other_db='Enembl'" # ditto: its Enembl not Ensembl
        rows2 = search_db (cursor, qry)
        if rows2:
            [uniprot_id, other_db, other_db_id ] = rows2[0]
            ensembl_id = other_db_id
            return [ensembl_id, "uniprot_id_translation"]
      
   
    return [ensembl_id, 'failure']

      
  
#########################################
def resolve_gene_name (cursor, name):
    
    qry = "select approved_symbol, approved_name  from hgnc_id_translation "
    qry += "where approved_symbol='%s'" % name
    rows = search_db(cursor, qry)
    if rows:
        return rows[0][:2]

    qry = "select approved_symbol, approved_name, previous_symbols  from hgnc_id_translation "
    qry += "where previous_symbols like '%%%s%%'" % name
    rows = search_db(cursor, qry)

    if rows and rows[0][2]:
        prev_symbols =  rows[0][2].split (', ')
        for ps in prev_symbols:
            if ps == name:
                return  rows[0][:2]

    qry = "select * from uniprot_id_translation "
    qry += "where other_db_id = '%s'" % name
    rows = search_db(cursor, qry)
    if rows:
        print '\n'.join(rows)


    return ["unresolved", ""]

  
#########################################
def main():

    if len(sys.argv) < 2:
        print  "usage: %s <names file> " % sys.argv[0]
        exit(1)

    names_file =  sys.argv[1]
    if not os.path.isfile(names_file):
        exit(1)

    db     = connect_to_mysql()
    cursor = db.cursor()
    switch_to_db (cursor, 'baseline')

    # unbuffered output
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)

    inf = open (names_file, "r")

    for line  in inf:
        name = line.rstrip().replace (' ', '').upper()
        ret = resolve_gene_name (cursor, name)
        print "%s\t%s\t%s" % (name, ret[0], ret[1])

    inf.close()

    cursor.close()
    db.close()


#########################################
if __name__ == '__main__':
    main()

