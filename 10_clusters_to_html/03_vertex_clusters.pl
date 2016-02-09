#! /usr/bin/perl -w

$species  = "human";
# the files below ahould have materialized from neo4j/R
$filename = "/Users/ivana/scratch/".$species."_clusters.txt";
$home  = "/Users/ivana/scratch/";


open (IF, "<$filename" ) 
    || die "Cno $filename: $!.\n";

@edges = ();
%clusters = ();
%name2id = ();
%id2name = ();
while ( <IF> ) {
    chomp;
    /\S/ || next;
    @aux = split;
    if ($aux[0] =~ "e") { # this is an edge
	push @edges, $aux[1]."_".$aux[2];

    } else {
	($node_id, $node_name, $cluster_id) = @aux;
	if (defined $clusters{$cluster_id}) {
	    $clusters{$cluster_id} =~ $node_id && next;
	    $clusters{$cluster_id} .= "_";
	} else {
	    $clusters{$cluster_id} = "";
	}
	$clusters{$cluster_id} .= $node_id;
	$name2id{$node_name} = $node_id;
	$id2name{$node_id}   = $node_name;
    }
}
close IF;


foreach $cluster_id (keys %clusters) {

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
	push @vert_names, $id2name{$vert};
    }

    foreach $name (@vert_names) {
	$id = $name2id{$name};
	if ($first) {
	    $first = 0;
	} else {
	    print OF ",\n";
	}
	print OF "{\"id\":$id, \"name\":\"$name\", \"cluster\":$cluster_id}";

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
	print OF "{\"source\":$source, \"target\":$target}";
    }
     print OF "\n]\n}\n";

    close OF;

}
