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
    
    RelationshipType INTERACTS  = DynamicRelationshipType.withName( "INTERACTS" );
    
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
    public ArrayList <ArrayList<String>> findPaths(String geneName1, String geneName2, int maxPathLength) {

        doBefore();

        Node node1 =  getNodeBySymbol(geneName1);
        System.out.println(node1 + "    " + node1.getProperty("official_symbol"));
        Node node2 =  getNodeBySymbol(geneName2);
        System.out.println(node2 + "    " + node2.getProperty("official_symbol"));
        int pathLength = 0;
        
       //PathFinder<Path> pathFinder = GraphAlgoFactory.shortestPath(
        PathFinder<Path> pathFinder = GraphAlgoFactory.allPaths(
            PathExpanders.forTypeAndDirection( INTERACTS, Direction.BOTH ),  maxPathLength);
        Iterable<Path> paths = pathFinder.findAllPaths( node1, node2 );
        ArrayList <String> vertices = new ArrayList <String>();
        ArrayList <String> edges = new ArrayList <String>();
        if (paths != null ) {
            
           for (Path path: paths) {
                String prevSymbol = null;
                for (Node node: path.nodes()) {
                    String symbol = (String) node.getProperty("official_symbol");
                   if (! vertices.contains(symbol) )  vertices.add(symbol);
                   if ( prevSymbol != null ){
                       String pair = "";
                       if (prevSymbol.compareTo(symbol) < 0 ) {
                           pair = prevSymbol + "_" + symbol;
                       } else {
                           pair = symbol + "_" +  prevSymbol ;
                       }
                       if (! edges.contains(pair) )  edges.add(symbol);
                   }
                }
           }
           System.out.println("all nodes seen " + vertices.size());
           for (String symbol: vertices) {
               System.out.println(symbol);
           }
           
        } else {
            System.out.println("no paths found");
        }
        System.out.println("done");
        doAfter();
        ArrayList <ArrayList<String>>  retPair = new  ArrayList <ArrayList<String>> ();
        retPair.add(vertices);
        retPair.add(edges);
        return retPair;
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
