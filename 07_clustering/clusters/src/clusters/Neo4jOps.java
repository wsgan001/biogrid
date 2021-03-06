/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clusters;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 *
 * @author ivana
 */
public class Neo4jOps {
    
    private static GraphDatabaseService graphDb;
    private static Transaction tx;
 
    public Neo4jOps () {
        startDb("/home/ivana/databases/neo4j/data/graph.db");
   
    }
    
    static void firstNbrs(ArrayList<String> names) {
        Map<String, Object> params = new HashMap<>();
        params.put("official_symbols", names);
        String query = "MATCH (tnf7int)-[:physica]-(neighbor) ";
        query += "WHERE tnf7int.official_symbol in  {official_symbols} ";
        query += "RETURN DISTINCT tnf7int.official_symbol AS tnf7int, ";
        query += " neighbor.official_symbol AS nbr";
        
        doBefore();
        Result result = graphDb.execute(query, params);
        doAfter();
 
    }
    static ArrayList <String[]> interactionPaths (ArrayList<String>names1, ArrayList<String>names2) {
        ArrayList <String[]>  interactionPaths = new  ArrayList <> ();
        
        Map<String, Object> params = new HashMap<>();
        params.put("from_symbols", names1);
        params.put("to_symbols", names2);
        String query = "MATCH (tnf7int)-[interaction:physical_low]-(neighbor) ";
        query += "WHERE tnf7int.official_symbol in {from_symbols} ";
        query += "AND neighbor.official_symbol in {to_symbols} ";
        query += "RETURN  DISTINCT tnf7int.official_symbol, interaction.pubmed_ids, neighbor.official_symbol";
        
        doBefore();
        Result  result = graphDb.execute(query, params);
        
        if (result.columns().isEmpty()) {
            System.out.println ("no interacting pairs found in the provided list");
            System.exit(1);
        }
        
        while (result.hasNext()) {
            Map<String, Object> row = result.next();

            String[] path = new String[row.size()];
            int i = 0;
            for (Map.Entry<String, Object> column : row.entrySet()) {
                path[i] = column.getValue().toString();
                i++;
            }
            interactionPaths.add(path);
        }
        doAfter();
         
        return interactionPaths;
    }
   
    static ArrayList <String[]>  interaction (ArrayList<String> names, 
                ArrayList <String[]>  interactingPairs, 
                ArrayList <String>    edgesAsString) {
        Map<String, Object> params = new HashMap<>();
        params.put("official_symbols", names);
        String query = "MATCH (tnf7int)-[interaction:physical_low]-(neighbor) ";
        query += "WHERE tnf7int.official_symbol in {official_symbols} ";
        query += "AND neighbor.official_symbol in {official_symbols} ";
        query += "RETURN  DISTINCT tnf7int.official_symbol AS a , ";
        query += "neighbor.official_symbol AS b, interaction.pubmed_ids AS pubmed,  ";
        query += "type(interaction) AS exp_type";
        
        System.out.println (query);
        doBefore();
        Result result = graphDb.execute(query, params);
        
        if (result.columns().isEmpty()) {
            System.out.println ("no interacting pairs found in the provided list");
            System.exit(1);
        }

        while (result.hasNext()) {
            Map <String, Object> row = result.next();
  
            String edge = "";
            String gene1 = (String) row.get("a");
            String gene2 = (String) row.get("b");
            if ( gene1.compareTo(gene2)< 0) {
                edge = gene1 + " " + gene2;
            } else {
                edge = gene2 + " " + gene1;
            }
            edge += " " + row.get("pubmed");
            edge += " " + row.get("exp_type");
            edgesAsString.add(edge);
            String [] pair = new  String[2];
            pair[0] = gene1;
            pair[1] = gene2;
            interactingPairs.add(pair);
         }
        doAfter();
        return interactingPairs;
    }

    static ArrayList <String>  addNeighbors (ArrayList<String> names, int numOfHops ) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("official_symbols", names);
        String query = "MATCH (tnf7int)-[:physical_low*1.."+numOfHops+"]-(neighbor) ";
        query += "WHERE tnf7int.official_symbol in {official_symbols} ";
        query += "RETURN  DISTINCT  neighbor.official_symbol";
        
        System.out.println (query);
        doBefore();
        Result result = graphDb.execute(query, params);
        
        if (result.columns().isEmpty()) {
            System.out.println ("no interacting pairs found in the provided list");
            System.exit(1);
        }
      
        ArrayList <String>  origPlusNbrs = new  ArrayList <> ();
        for (String name : names) {
            if (!origPlusNbrs.contains(name)) {
                origPlusNbrs.add(name);
            }
        }

        while (result.hasNext()) {
            Map <String, Object> row = result.next();
            for (Map.Entry<String, Object> column : row.entrySet()) {
                String nbr = (column.getValue().toString());
                if (!origPlusNbrs.contains(nbr)) {
                    origPlusNbrs.add(nbr);
                }
            }
        }
        doAfter();
        System.out.println(names.size());
        System.out.println(origPlusNbrs.size());
         
        return origPlusNbrs;
    }
    
   
    public static void shutdownDb()  {
        System.out.println("closing graphDb"); 
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
       System.out.println("starting "+ storeDir);
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
        tx.close();
    }

   
}
