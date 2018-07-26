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
    
    public List<String> getBranchingVar (Collection<Rectangle> rects, List<String> excludedVars) {
        
        this.initializeMap(rects,excludedVars);
        
        List<String> result = new ArrayList<String> ();
        int maxFreq = Collections.max(frequencyMap.values()); 
        for (Entry<String, Integer> entry : this.frequencyMap.entrySet()){
            if (entry.getValue()==maxFreq){
                result.add (entry.getKey());
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
