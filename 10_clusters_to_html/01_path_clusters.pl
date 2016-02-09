#! /usr/bin/perl -w

$species  = "human";
# the files below ahould have materialized from neo4j/R
$filename = "/Users/ivana/scratch/".$species."_clusters.txt";
$interactions_file = "/Users/ivana/scratch/".$species."_pubmed_ids.txt";
$home  = "/Users/ivana/scratch/";


open (IF, "<$filename" ) 
    || die "Cno $filename: $!.\n";

%clusters = ();
%name2id = ();
$ct = 0;
while ( <IF> ) {
    chomp;
    /\S/ || next;
    @aux = split;
    if ($aux[0] =~ "cluster") { # this is beginning of a cluster (of paths)
		$cluster_id = pop @aux;
		$clusters{$cluster_id} =  "";

    } else {
        # we will turn it into cluster of nodes
        for $node (@aux) {
        	$clusters{$cluster_id} =~ $node && next;
        	$clusters{$cluster_id} && ( $clusters{$cluster_id} .= "_");
        	$clusters{$cluster_id} .= "$node";
        	$ct ++;
        	$name2id{$node} = $ct;
        }
    }
}
close IF;

# edges
open (IF, "<$interactions_file" )
    || die "Cno $interactions_file: $!.\n";
@edges = ();
while (<IF>) {
    chomp;
    @aux = split;
    push @edges, $aux[0]."_".$aux[1];
}
close IF;

$cluster_ct = 0;
foreach $cluster_id (keys %clusters) {
	$cluster_ct = 0;
	@vertices = split "_", $clusters{$cluster_id};
    next if (@vertices < 2);

    open(OF, ">$home/json_".$species."/cluster_$cluster_id.json") ||
        die "Cno home/json_".$species."/cluster_$cluster_id.json: $!\n";

    print OF "{\n";
    print OF "\"nodes\":[\n";
    $first = 1;
    #print OF " $cluster_id  ",  scalar @{$clusters{$cluster_id}}, " @{$clusters{$cluster_id}} \n";
    # let's drop the isolated cases
    @vert_names = ();
    foreach $vert (@vertices) {
		($id, $name) = split "_", $vert;
		push @vert_names, $name;
    }

    foreach $name (@vertices) {
 		$id = $name2id{$name};
		if ($first) {
			$first = 0;
		} else {
			print OF ",\n";
		}
		print OF "{\"id\":$id, \"name\":\"$name\", \"cluster\":$cluster_ct}";

    }
 
    print OF "\n],\n";
    print OF "\"edges\":[\n";
    $first = 1;
    for $edge (@edges) {
		($source, $target) = split "_", $edge;
		($clusters{$cluster_id} =~ $source) || next;
		($clusters{$cluster_id} =~ $target) || next;
		if ($first) {
			$first = 0;
		} else {
			print OF ",\n";
		}
		if ( ! defined $name2id{$source}) {
		    print "source  $source $target\n";
		     exit;
		}
		print OF "{\"source\":$name2id{$source}, \"target\":$name2id{$target}}";
    }
     print OF "\n]\n}\n";

    close OF;

}
