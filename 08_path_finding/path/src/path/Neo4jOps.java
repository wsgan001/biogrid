/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package path;

/**
 *
 * @author ivana
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.*;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;


public class Neo4jOps {
    private GraphDatabaseService graphDb;
    private GlobalGraphOperations ggo;
    private  Transaction tx;
    private String storeDir;
    
    RelationshipType PHYSICAL_LOW     = DynamicRelationshipType.withName( "PHYSICAL_LOW" );
    RelationshipType PHYSICAL_HI      = DynamicRelationshipType.withName( "PHYSICAL_HI" );
    RelationshipType GENETIC_LOW      = DynamicRelationshipType.withName( "GENETIC_LOW" );
    RelationshipType GENETIC_HI       = DynamicRelationshipType.withName( "GENETIC_HI" );
    RelationshipType PHYSICAL_HI_LOW  = DynamicRelationshipType.withName( "PHYSICAL_HI_LOW" );
    RelationshipType GENETIC_HI_LOW   = DynamicRelationshipType.withName( "GENETIC_HI_LOW" );
    
    private static Label label = DynamicLabel.label("gene_name");
    
    public Neo4jOps (String storeDir) {
        this.storeDir = storeDir;
        startDb();
        // check that we have the index for retrieval of nodes by theor official nam
        // that should be a one-time thing: "An index is created if it doesn’t exist when you ask for it."
    }

    ///////////////////////////////////
    private Node getNodeBySymbol (String symbol) {
        String query = " match (g:gene {official_symbol: '" + symbol + "'})  return id(g)";
        Result result = graphDb.execute(query);
        if (! result.hasNext()) {
            System.out.println("no node found for " + symbol);
            System.exit(1);
        }
        // oh grossnesss
        long nid = (long) result.next().entrySet().iterator().next().getValue();
        return  graphDb.getNodeById(nid);
    }
    
    ///////////////////////////////////
    public  ArrayList <ArrayList<String>> findEdges (ArrayList <Node> vertexNodes) {
        
        doBefore();
        
        ArrayList<String> vertices = new  ArrayList<String> ();
        ArrayList<String> edges    = new  ArrayList<String> ();
        
         
        ArrayList <Long> nodeIds = new ArrayList <Long> ();
        for (Node node: vertexNodes) {
            vertices.add((String)node.getProperty("official_symbol"));
            nodeIds.add(node.getId());
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("identifiers", nodeIds);
        //String query = "MATCH (a)-[int:PHYSICAL_LOW]-(b) ";
        String query = "MATCH (a)-[int]-(b) ";
        query += "WHERE id(a) in {identifiers} ";
        query += "AND id(b) in {identifiers} ";
        query += "RETURN  DISTINCT a.official_symbol AS a, b.official_symbol AS b, type(int) AS exp_type, int.pubmed_ids AS pubmed";
        Result result = graphDb.execute(query, params);
        
        while ( result.hasNext()) {
            Map <String, Object> ret = result.next();
            String edge = "";
            String gene1 = (String) ret.get("a");
            String gene2 = (String) ret.get("b");
            if ( gene1.compareTo(gene2)< 0) {
                edge = gene1 + " " + gene2;
            } else {
                edge = gene2 + " " + gene1;
            }
            edge += " " + ret.get("pubmed");
            edge += " " + ret.get("exp_type");
            if (! edges.contains(edge)) {
                edges.add(edge);
            }
        }
        
        ArrayList <ArrayList<String>> retval = new ArrayList <ArrayList<String>> ();
        retval.add(vertices);
        retval.add(edges);
        
        doAfter();
       
        return retval;
    }
    
    
    ///////////////////////////////////
    public ArrayList <Node> findPaths(String geneName1, String geneName2, int maxPathLength,
            ArrayList <ArrayList <String>>  pathsAsStringList, ArrayList <Node> vertexNodes) {

        doBefore();

        Node node1 =  getNodeBySymbol(geneName1);
        System.out.println(node1 + "    " + node1.getProperty("official_symbol"));
        Node node2 =  getNodeBySymbol(geneName2);
        System.out.println(node2 + "    " + node2.getProperty("official_symbol"));
        int pathLength = 0;
        
       //PathFinder<Path> pathFinder = GraphAlgoFactory.shortestPath(
        //PathFinder<Path> pathFinder = GraphAlgoFactory.allPaths(
        //    PathExpanders.forTypeAndDirection(PHYSICAL_LOW, Direction.BOTH),  maxPathLength);
        PathFinder<Path> pathFinder = GraphAlgoFactory.allPaths(
            PathExpanders.allTypesAndDirections(),  maxPathLength);
        Iterable<Path> paths = pathFinder.findAllPaths( node1, node2 );
        
        if (paths != null ) {           
            int numberOfPaths = 0;
            for (Path path: paths) {   
                ArrayList <String> pathNames = new ArrayList <String> ();
                numberOfPaths ++;
                for (Node node: path.nodes()) {
                    // special way in which we want to handle thigs here: do not add the the
                    // query proteins to this list
                    //if (node == path.startNode() || node== path.endNode()) continue;
                    if (!vertexNodes.contains(node)) vertexNodes.add(node);
                    pathNames.add( (String)node.getProperty("official_symbol"));
                }
                pathsAsStringList.add(pathNames);
            }
           System.out.println("number of paths: " + numberOfPaths);
           System.out.println("number of nodes: " + vertexNodes.size());
           
        } else {
            System.out.println("no paths found");
        }
        System.out.println("done");
        doAfter();
        
        return vertexNodes;
    }
      
    ///////////////////////////////////
    private void doBefore()  {
        tx = graphDb.beginTx();
    }

    private void doAfter(){
        tx.success();
        tx.finish();
    }
    private  void startDb()   {
        // it looks like neo4j oroginally puts it in 
        // /Users/ivana/databases/neo4j/libexec/data/graph.db/
        // but I like to move it so it wouldn't scribble over it incidentially
       graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(storeDir);
       ggo = GlobalGraphOperations.at(graphDb);
        
       System.out.println("graphDb started on "+ storeDir);
       System.out.println("graphDb: " + graphDb);
    }

    private void shutdownDb()  {
        try {
            if ( graphDb != null ) graphDb.shutdown();
        } finally  {
            graphDb = null;
        }
        System.out.println("graphDb closed"); 
    }
 
    public void dbStatistics() {
        long relationshipCounter = 0;
        long nodeCounter = 0;
        doBefore();
        for (Node node: ggo.getAllNodes()) {
            nodeCounter++;
         }
        for (Relationship re: ggo.getAllRelationships()) {
            relationshipCounter++;
        }
        System.out.println("Number of Relationships: " + relationshipCounter);
        System.out.println("Number of Nodes: " + nodeCounter);
        System.out.println("Available labels: " );
        for (Label label:   ggo.getAllLabels()){
            System.out.println("\t" + label.name() );
        }
        System.out.println("Available indices: " );
        Schema schema = graphDb.schema();
        for (IndexDefinition index: schema.getIndexes() ) {
            System.out.println("\t" + index.toString() +  "  " + index.getLabel());
        }
        
        
        doAfter();
    }
    
 
    
    
}
