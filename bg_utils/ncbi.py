from mysql   import search_db, switch_to_db
from ensembl import species2taxid, get_compara_name


########
def taxid2name (cursor, tax_id):
    switch_to_db (cursor, get_ncbi_tax_name (cursor))
    qry  = "select name_txt from names where tax_id= %d " % int(tax_id)
    qry += " and name_class = 'scientific name'";
    rows = search_db (cursor, qry)
    if (not rows):
        rows = search_db (cursor, qry, verbose = True)
        return ""
    return rows[0][0]

########
def taxid2trivial (cursor, tax_id):
    switch_to_db (cursor, get_ncbi_tax_name (cursor))
    qry  = "select name_txt from names where tax_id= %d " % int(tax_id)
    qry += " and name_class = 'trivial'";
    rows = search_db (cursor, qry)
    if (not rows or 'ERROR' in rows[0]):
        rows = search_db (cursor, qry, verbose = True)
        return ""
    return rows[0][0]

########
def trivial2taxid (cursor, trivial_name):
    switch_to_db (cursor, get_ncbi_tax_name (cursor))
    qry  = "select tax_id from names where name_txt= '%s' " % trivial_name
    qry += " and name_class = 'trivial'";
    rows = search_db (cursor, qry)
    if (not rows or 'ERROR' in rows[0]):
        rows = search_db (cursor, qry, verbose = True)
        return ""
    return int(rows[0][0])


########
def taxid2parentid (cursor, tax_id):
    switch_to_db (cursor, get_ncbi_tax_name (cursor))
    qry = "select parent_tax_id from nodes where tax_id= %d " % int(tax_id)
    rows = search_db (cursor, qry)
    if (not rows):
        rows = search_db (cursor, qry, verbose = True)
        return ""
    try:
        retval = int(rows[0][0])
    except:
        retval = ""
    return retval

########
def get_ncbi_tax_name (cursor):
    qry = "show databases like 'ncbi%tax%'"
    rows = search_db (cursor, qry)
    if (not rows):
        rows = search_db (cursor, qry, verbose = True)
        return ""
    return rows[0][0]

########
def get_common_name (cursor, species):
    switch_to_db(cursor, get_compara_name(cursor))
    tax_id = species2taxid (cursor, species)
    switch_to_db(cursor,get_ncbi_tax_name (cursor))
    qry   = "select name_txt from names where "
    qry  += "tax_id = %d and " % tax_id
    qry  += "name_class = 'genbank common name'"
    rows = search_db (cursor, qry)
    if rows:
        if ('ERROR' in rows[0]):
            search_db (cursor, qry, verbose = True)
            return ""
        else:
            return rows[0][0]

    return ""

#########
########
# this is not working bcs the siceintific name is nto necessarilty unique
# eg canis lupus, canis lupus familiaris, canis familiaris
def trivial2scientific (cursor, trivial):
    switch_to_db(cursor,get_ncbi_tax_name (cursor))
    qry   = "select tax_id from names where "
    qry  += "name_txt = '%s' and " % trivial
    qry  += "name_class = 'trivial'"
    rows = search_db (cursor, qry)
    if rows:
        if ('ERROR' in rows[0]):
            search_db (cursor, qry, verbose = True)
            return ""
        else:
            try:
                tax_id = int(rows[0][0])
            except:
                return ""
            sciname = taxid2name(cursor, tax_id).lower().replace(" ", "_")
            #canis_lupus_familiaris - don't know what to do with it
            sciname = sciname.replace("_familiaris", "")
            return sciname

    return ""

#########
def find_mammals(cursor, trivial_name_list):
    
    mammals = []
    for trivial_name in trivial_name_list:
        switch_to_db(cursor, get_compara_name(cursor))
        tax_id = trivial2taxid (cursor, trivial_name)
        parent_id = taxid2parentid (cursor, tax_id)

        tax_id = parent_id
        is_mammal = False
        while tax_id:
            qry  = "select name_txt from names where tax_id= %d " % int(tax_id)
            qry += " and name_class = 'scientific name'";
            rows = search_db (cursor, qry)
            if rows and rows[0][0]:
                if 'mammal' in rows[0][0].lower():
                    is_mammal = True
                    break
                elif 'vertebrat' in  rows[0][0].lower():
                    # if the thing wasa mammal, we would have found it by now
                    is_mammal = False
                    break
               
            parent_id = taxid2parentid (cursor, tax_id)
            if parent_id and parent_id>1:
                tax_id = parent_id
            else:
                tax_id = None

        if is_mammal: 
            mammals.append(trivial_name)

            
    return mammals
