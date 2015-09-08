#! /usr/bin/perl -w


$home         = "/Users/ivana/scratch";


$html_template     = "template.html";
$species  = "human";
$html_dir = "$home/html_".$species;
$cluster_function_html_dir = "$html_dir/function";


for ($cluster_function_html_dir , $html_dir, $html_template) {
    -e $_ || die "$_ not found in $home.\n";
}
@clusterfiles = split "\n", `ls $cluster_function_html_dir`;

@cluster_ids = ();
for $cf (@clusterfiles) {
    next if $cf =~ /isolated/;
    @field = split '\.', $cf;
    $cluster_id = $field[0];
    $cluster_id =~ s/cluster_//g;
    push @cluster_ids, $cluster_id;
}

print "@cluster_ids\n";

$cluster_list_html = "";
for $cluster_id (@cluster_ids) {
    $cluster_list_html .= "<a href=\"cluster_$cluster_id.html\">Cluster $cluster_id</a><br>\n";
}

open (FH, "<$html_template") || die "Cno $html_template: $!\n";
undef $/;
$template = <FH>;
$/ = "\n";
close FH;

@colors = ("#1B9E77","#D95F02","#7570B3","#E7298A","#66A61E","#E6AB02");
$cluster_id_of_colors = scalar @colors;
$ct = 0;
for $cluster_id (@cluster_ids) {
    
    $cluster_html = $template;
    $cluster_html =~ s/__CLUSTERS__/$cluster_list_html/;
    $cluster_html =~ s/__TITLE__/Cluster $cluster_id/;
    $cluster_html =~ s/__CLUSTER_JSON__/json_$species\/cluster_$cluster_id.json/;

    $color_no = $ct%$cluster_id_of_colors;
    $color = $colors[$color_no];
    $cluster_html =~ s/__COLOR__/$color/g;
    
    $function = `cat $cluster_function_html_dir/cluster_$cluster_id.function.html`;
    $cluster_html =~ s/__FUNCTION__/$function/;

    $fnm = "$html_dir/cluster_$cluster_id.html";
    open (FH, ">$fnm") || die "Cno $fnm: $!\n";
    print FH  $cluster_html;
    close FH;

    $ct ++;
}

