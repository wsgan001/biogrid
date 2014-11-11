/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clusters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 *
 * @author ivana
 */
public class Neo4jOps {
    
    private static GraphDatabaseService graphDb;
    private static Transaction tx;
    private static ExecutionEngine engine;
 
    public Neo4jOps () {
        startDb("/Users/ivana/databases/neo4j/data/graph.db");
        engine = startEngine(); 
    }
    
    static void firstNbrs(ArrayList<String> names) {
        Map<String, Object> params = new HashMap<>();
        params.put("official_symbols", names);
        String query = "MATCH (tnf7int)-[:physical]-(neighbor) ";
        query += "WHERE tnf7int.official_symbol in  {official_symbols} ";
        query += "RETURN DISTINCT tnf7int.official_symbol AS tnf7int, ";
        query += " neighbor.official_symbol AS nbr";
        
        genericQuery (engine, query, params);
 
    }
       
   static ArrayList <String[]>  interaction (ArrayList<String> names) {
        Map<String, Object> params = new HashMap<>();
        params.put("official_symbols", names);
        String query = "MATCH (tnf7int)-[:physical]-(neighbor) ";
        query += "WHERE tnf7int.official_symbol in {official_symbols} ";
        query += "AND neighbor.official_symbol in {official_symbols} ";
        query += "RETURN  DISTINCT tnf7int.official_symbol, neighbor.official_symbol";
        
        //genericQuery (engine, query, params);
        
        doBefore();
        ExecutionResult  result = engine.execute(query, params);
        doAfter();
      
        ArrayList <String[]>  interactingPairs = new  ArrayList <> ();
        String outLine;
        for (Map<String, Object> row : result) {
            String [] pair = new  String[2];
            int i = 0;
            for (Map.Entry<String, Object> column : row.entrySet()) {
                pair[i] = (column.getValue().toString());
                i++;    
            }
            interactingPairs.add(pair);
        }
        return interactingPairs;
   }
    
   private static void genericQuery (ExecutionEngine engine, String query, 
                                     Map<String, Object> params) {
        doBefore();
        ExecutionResult  result = engine.execute(query, params);
        doAfter();
        System.out.println(result);
        String outLine;
        for (Map<String, Object> row : result) {
            outLine = "";
            for (Map.Entry<String, Object> column : row.entrySet()) {
                outLine += column.getValue() + " ";
            }
            System.out.println(outLine);
        }
     
   }
   
   private static ExecutionEngine startEngine() {
       StringBuffer dumpBuffer=new StringBuffer();
       StringLogger dumpLogger=StringLogger.wrap(dumpBuffer);
       return  new ExecutionEngine(graphDb, dumpLogger); 
    }
    public static void shutdownDb()  {
        try {
            if ( graphDb != null ) graphDb.shutdown();
        } finally  {
            graphDb = null;
        }
        System.out.println("graphDb closed"); 
    }
    ////////////////////////////
    private  static void dbStatistics() {
        long relationshipCounter = 0;
        long nodeCounter = 0;
        doBefore();
        GlobalGraphOperations ggo = GlobalGraphOperations.at(graphDb);
        for (Node node: ggo.getAllNodes()) {
            nodeCounter++;
         }
        for (Relationship re: ggo.getAllRelationships()) {
            relationshipCounter++;
        }
        System.out.println("Number of Relationships: " + relationshipCounter);
        System.out.println("Number of Nodes: " + nodeCounter);
        doAfter();
    }
    //////////////////////////////////////////////////////////////////////////

    private static void startDb(String storeDir)   {
       graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(storeDir);
       System.out.println("graphDb started on "+ storeDir);
       dbStatistics();
    }
    //////////////////////////////////////////////////////////////////////////
    public static  void doBefore()  {
        tx = graphDb.beginTx();
    }

    public static void doAfter(){
        tx.success();
    }

   
}