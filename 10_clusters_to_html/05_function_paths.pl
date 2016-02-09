#! /usr/bin/perl -w -I/Users/ivana/perlscr

use Simple;		#HTML support



$species  = "human";

$filename = "/Users/ivana/scratch/".$species."_clusters.txt";
$interactions_file = "/Users/ivana/scratch/".$species."_pubmed_ids.txt";
$name2uniprot = "/Users/ivana/scratch/".$species."_names2uniprot.txt";
$home  = "/Users/ivana/scratch/";


open (IF, "<$filename" )
    || die "Cno $filename: $!.\n";

%clusters = ();
%name2id = ();
$ct = 0;
@names = ();
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
        	$clusters{$cluster_id}  && ( $clusters{$cluster_id} .= "_");
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



foreach $cluster_id (keys %clusters) {
	@names = split "_", $clusters{$cluster_id};
    next if  (@names < 2);

    open(OF, ">$home/html_".$species."/function/cluster_$cluster_id.function.html");


    foreach $name (@names) {
       
		$ret = `grep $name $name2uniprot`; chomp $ret;
		($name_again, $uniprot) = split ' ', $ret;

		$htmlstring  = "http://www.uniprot.org/uniprot/$uniprot.txt";
		$retfile = get $htmlstring || "";
		if (! $retfile ) {
			print OF "$uniprot  no return from uniprot\n\n";
			next;
		}
		print OF "\n<hr>\n";
		print OF "<p> <b>$name</b>  <a href=\"http://www.uniprot.org/uniprot/$uniprot\" target=\"_blank\">Uniprot</a>   ";

		$reading = 0;
		$function = "";
		for $line (split "\n", $retfile) {

			$field_id = substr $line, 0, 2;
			if ($field_id eq "DE" && $line =~ 'RecName') {
				$line =~ s/"DE   RecName: Full="//g;
				print OF substr( $line, 19),"\n";
			} elsif ( $field_id eq "CC") {
				if ($line =~ "-!-") {
					if ( $reading)  {
					$function =~ s/\{.*\}\.*//g;
					print OF "$function</p>\n";
					}
					$reading = ( $line =~ 'FUNCTION' ) ;
					$line =~ s/FUNCTION\://g;
					$reading && print OF "<p>\n";
				}
				if ($reading) {
					$function .= " ".substr( $line, 8);
				}
			}
		}
		print OF "\n<br>\n";
		@lines = split "\n", `grep $name $interactions_file`;
	
		for $line (@lines) {

			chomp $line;
			@aux = split " ", $line;
			($name eq $aux[0])  || ($name eq $aux[1]) || next;

			if ($name eq $aux[0]) {
				($clusters{$cluster_id} =~ $aux[1])  || next;
				print OF "$aux[0]  interaction  with $aux[1] pubmed ids:";
			} elsif ($name eq $aux[1]) {
				($clusters{$cluster_id} =~ $aux[0])  || next;
				print OF  "$aux[1]  interaction  with $aux[0] pubmed ids:";
			}
			print OF "\n";
			@aux2 = split ";", $aux[2];
			$first = 1;
			for $pid (@aux2) {
				if ($first) {
					$first = 0;
				} else {
					print OF ",\n";
				}
				print OF "\t <a href=\"http://www.ncbi.nlm.nih.gov/pubmed/$pid\" target=\"_blank\">$pid</a>";
			}
			print OF "\n<br>\n";
		}
		print OF "\n";
    }
 
    close OF;

}
