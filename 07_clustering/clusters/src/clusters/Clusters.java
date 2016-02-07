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
import scala.Int;



/**
 *
 * @author ivana
 */
public class Clusters {
       
    public static void main(String[] args) {
        //ArrayList <String> names = readNames("/Users/ivana/projects/colabs/Biserka/progesterone/PR_binding_sites/scripts/report_table", 
        //        "\\s+", 8);   
        ArrayList <String> names = readNames("/Users/ivana/projects/colabs/Biserka/progesterone/PR_binding_sites/scripts/test.list",
                "\\s+", 8);   
        for (String name: names) {
            System.out.println(name);
        }
        
        Neo4jOps neoInterface = new Neo4jOps();
        //Neo4jOps.firstNbrs   (names);
        ArrayList <String[]> interactingPairs = Neo4jOps.interaction (names);       
        neoInterface.shutdownDb();
        // for now just output here
        for (String[] intPair: interactingPairs) {
            System.out.println (intPair[0] + "  " +intPair[1]);
        }
        
        
        System.exit(0);
        
        ROps rInterface = new ROps();
        ArrayList <String[]> vertices = new ArrayList<> ();
        for (String name: names) {
            String [] vertex = new String [1];
            vertex[0] = name; // other indices could be properties of the nodes
            vertices.add(vertex);
        }
        
        HashMap  <String,Integer> clusterMembership = rInterface.findClusters(interactingPairs, vertices);
        rInterface.shutDown();
        
        for (String name: names) {
            System.out.printf("%d   %s   %d \n", 
                    names.indexOf(name), name, clusterMembership.get(name));
        }       
         for (String[] pair: interactingPairs) {
             System.out.printf("e %d  %d \n", names.indexOf(pair[0]), names.indexOf(pair[1]));
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
