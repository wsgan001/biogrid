# biogrid


Biogrid handling tools.

A [Biogrid](http://thebiogrid.org/) processing pipeline, a utilitarian mixture of Python, Perl, Sql, R and Java. The main flow: from Biogrid site to local MySql database; fom MySql to a [Neo4j](http://neo4j.com/) database, so the inquiries about network properties of a  given set of genes can be answered.

[07_clusters](https://github.com/ivanamihalek/biogrid/tree/master/07_clustering/clusters/src), in particular,  uses [Java/R Interface](https://www.rforge.net/JRI/) to tie the info from  Neo4j to [Girvan-Newman](http://www.pnas.org/content/99/12/7821.full) clustering algorithm (implemented in [Igraph](http://igraph.org/)).

In [10_clusters_to_html](https://github.com/ivanamihalek/biogrid/tree/master/10_clusters_to_html) some basic functionality for turning the resuts into an html format can be found. The cluster visualization can be made interactive using [Alchemy.js](http://graphalchemist.github.io/Alchemy/#/)
See [template.html](https://github.com/ivanamihalek/biogrid/blob/master/10_clusters_to_html/template.html).

