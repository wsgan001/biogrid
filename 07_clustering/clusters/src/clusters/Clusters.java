/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clusters;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import static java.lang.System.in;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import scala.Int;



/**
 *
 * @author ivana
 */
public class Clusters {
       
    public static void main(String[] args) throws FileNotFoundException {
        
        ArrayList <String> names = readNames("/home/ivana/projects/colabs/Biserka/progesterone/PR_binding_sites/scripts/report_table", 
             "\\s+", 8);   
        //ArrayList <String> names = readNames("/home/ivana/projects/colabs/Biserka/progesterone/PR_binding_sites/scripts/test.list",
         //    "\\s+", 8);   
        for (String name: names) {
            System.out.println(name);
        }
        
        Neo4jOps neoInterface = new Neo4jOps();
        //Neo4jOps.firstNbrs   (names);
        int numOfHops = 1;
        ArrayList <String> origPlusNbrs; 
        origPlusNbrs = Neo4jOps.addNeighbors(names, numOfHops);
        names = origPlusNbrs;

        ArrayList <String[]> interactingPairs = new  ArrayList <> ();  
        ArrayList <String>   edgesAsString= new  ArrayList <> () ;
        Neo4jOps.interaction (names, interactingPairs, edgesAsString);
        
        neoInterface.shutdownDb();
        
        if (interactingPairs.isEmpty()) {
            System.out.println ("No interactions in the list provided.");
            System.exit(0);
        }
        
        try {
            PrintWriter writer =  new PrintWriter("/home/ivana/scratch/pubmed_ids.txt");
            for (String pair: edgesAsString) {
               String[] fields = pair.split(" ");
               writer.printf("%s   %s   %s   %s\n", fields[0], fields[1], fields[2], fields[3]);
            }
            writer.close();
            
        } catch (Exception e) {
            System.out.printf("error opening pubmed id file for output: " + e.getMessage());
            System.exit(1);
        }
        
        ROps rInterface = new ROps();
        ArrayList <String[]> vertices = new ArrayList<> ();
        for (String name: names) {
            String [] vertex = new String [1];
            vertex[0] = name; // other indices could be properties of the nodes
            vertices.add(vertex);
        }
        
        HashMap  <String,Integer> clusterMembership = rInterface.findClusters(interactingPairs, vertices);
        rInterface.shutDown();
        
        
        try (PrintWriter writer = new PrintWriter("/home/ivana/scratch/clusterss.txt")) {

            for (String name : names) {
                writer.printf("%d   %s   %d \n",
                        names.indexOf(name), name, clusterMembership.get(name));
            }
            for (String[] pair : interactingPairs) {
                writer.printf("e %d  %d \n", names.indexOf(pair[0]), names.indexOf(pair[1]));
            }           
            writer.close();
            
        } catch (Exception e) {
            System.out.printf("error opening cluster file for output: " + e.getMessage());
            System.exit(1);
        }

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
                fields = strLine.split(separator) ;
                if (fields.length < column) continue;
                String name = fields[column-1].replaceAll("\\s","");
                if (name.equals("unresolved")) continue;
                if (names.contains(name))  continue;
                names.add(fields[column-1]);
           }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } 
        
        return names;
    }

    
       
    
    
}
