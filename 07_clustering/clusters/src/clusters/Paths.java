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
public class Paths {
       
    public static void main(String[] args) {
        
        ArrayList <String> names1 = readNames("/Users/ivana/Dropbox/yujia/yujia.names.resolved", "\\t", 2);   
        ArrayList <String> names2 = readNames("/Users/ivana/Dropbox/yujia/data/HDACs_approved_names.tab", "\\t", 2);   
                 
        Neo4jOps neoInterface = new Neo4jOps();
        //Neo4jOps.firstNbrs   (names);
        ArrayList <String[]> paths = Neo4jOps.interactionPaths (names1, names2);       
        neoInterface.shutdownDb();
        // for now just output here
        for (String [] path: paths) {
            for (String node: path) {
                System.out.print(node + "  ");
            }
            System.out.println();
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
                String name = fields[column-1].replaceAll("\\s","");
                if (name.equals("unresolved")) continue;
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
