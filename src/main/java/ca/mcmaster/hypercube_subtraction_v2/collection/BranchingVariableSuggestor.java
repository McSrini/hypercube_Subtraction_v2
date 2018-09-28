/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;
 
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author tamvadss
 * 
 * return highest frequency variable
 * 
 */
public class BranchingVariableSuggestor {
    
    public Map<String, Integer> frequencyMap = new TreeMap <String, Integer> ();
    
    public BranchingVariableSuggestor (){
        
    }
    
    //return all vars which have the highest frequency in the infeasible rects
    //
    //note that, if any rects have a single var , then we branch on that single var first, so these are returned without examining the other vars
    public List<String> getBranchingVar (Collection<Rectangle> rects, List<String> excludedVars) {
        
        List<String> result =   new ArrayList<String>();
                //getVariablesOccuringAloneInSomeHypercube(rects, excludedVars);
                
        //if (result.size()==ZERO){
            
            //look for highest frequency variables in the infeasible hypercubes
            
            this.initializeMap(rects,excludedVars);        
        
            int maxFreq = Collections.max(frequencyMap.values()); 
            for (Entry<String, Integer> entry : this.frequencyMap.entrySet()){
                if (entry.getValue()==maxFreq){
                    result.add (entry.getKey());
                }
            }
        //}
               
        return result;
    }
    
    private List<String> getVariablesOccuringAloneInSomeHypercube (Collection<Rectangle> infeasibleHypercubeList, List<String> excludedVars) {
        List<String> result = new ArrayList<String> ();
        
        //check every hypercube, if it has only 1 active var left. If so , collect it.
        
        for (Rectangle thisHyperCube : infeasibleHypercubeList){
            List<String> thisHypercubeFixedVars = new ArrayList<String>();
            thisHypercubeFixedVars.addAll(thisHyperCube.zeroFixedVariables );
            thisHypercubeFixedVars.addAll(thisHyperCube.oneFixedVariables );
            thisHypercubeFixedVars.removeAll( excludedVars);
            if ( thisHypercubeFixedVars.size()==ONE) {
                result.add( thisHypercubeFixedVars.get(ZERO) );
                break; //one is enough, order does not matter as all such vars will be branched upon (i.e. will be fixed)
            }
        }
        
        return result;
    }
    
    private void initializeMap (Collection<Rectangle> rects,List<String> excludedVars){
        
        this.frequencyMap.clear();
        
        for (Rectangle rect : rects){
             
            for (String var : rect.zeroFixedVariables){
                if (!excludedVars.contains(var)) updateFrequency (var);
            }
            for (String var : rect.oneFixedVariables){
                if (!excludedVars.contains(var)) updateFrequency (var);
            }
        }
    }
    
    private void updateFrequency (String var) {
        
        Integer value = frequencyMap.get(var);
        int frequency = ZERO;
        if (null != value) frequency = value;
        this.frequencyMap.put(var,ONE+frequency);
    }
}
