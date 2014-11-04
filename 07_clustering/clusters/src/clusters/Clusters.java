/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clusters;

import java.io.BufferedReader;
import java.io.FileReader;
import static java.lang.System.in;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.*;
import org.neo4j.kernel.impl.util.StringLogger;


/**
 *
 * @author ivana
 */
public class Clusters {
    
    private static GraphDatabaseService graphDb;
    private static ExecutionEngine engine;
    
    public static void main(String[] args) {
        startDb("/Users/ivana/databases/neo4j/data/graph.db");
        engine = startEngine();     
        ArrayList <String> names = readNames("/Users/ivana/scratch/yujia.names.resolved", "\\t", 1);   
        extractSubnet(names);
        //findCLusters();
        shutdownDb();
      
    }
    
    private static void extractSubnet(ArrayList <String> names) {
        Map<String, Object> params = new HashMap<>();
        params.put( "official_symbols", names );
        String query = "MATCH n WHERE n.official_symbol in {official_symbols} RETURN n";
        ExecutionResult result = engine.execute( query, params );
    }
    
    
    
    private static  ArrayList <String> readNames(String filename, String separator, int column) {
        String [] fields;
        ArrayList <String> names = new ArrayList ();
        try {

            BufferedReader br = new BufferedReader(new FileReader(filename));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // chop the input line into fields
                if (strLine.trim().length() ==0) continue;
                fields = strLine.trim().split(separator) ;
                names.add(fields[column-1]);
            }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        } 
        
        return names;
    }

    private static ExecutionEngine startEngine() {
       StringBuffer dumpBuffer=new StringBuffer();
       StringLogger dumpLogger=StringLogger.wrap(dumpBuffer);
       return  new ExecutionEngine(graphDb, dumpLogger); 
    }
    private static void shutdownDb()  {
        try {
            if ( graphDb != null ) graphDb.shutdown();
        } finally  {
            graphDb = null;
        }
        System.out.println("graphDb closed"); 
    }

    private static void startDb(String storeDir)   {
       graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(storeDir);
       System.out.println("graphDb started on "+ storeDir);
    }

       
    
    
}
