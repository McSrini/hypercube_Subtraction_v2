/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.hueristicSolvers;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ZERO;
import ca.mcmaster.hypercube_subtraction_v2.collection.BranchingVariableSuggestor;
import ca.mcmaster.hypercube_subtraction_v2.collection.Rectangle;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class Ramos3 {
    
    private Random rand =null;
    
    //all hypercubes have 0 fixed vars
    private Map<Rectangle, Map<String, Integer>> mapOfVarFrequency = new HashMap<Rectangle, Map<String, Integer>>();
    
    public Ramos3(List<Rectangle> infeasibleHypercubesList, long seed){
        
        rand=new Random (seed) ;
        //Collections.shuffle(infeasibleHypercubesList, rand);
        inititializeMap( infeasibleHypercubesList);
    }
    
    //grow the supplied solution by flipping vars when possible
    public List<String> growSolutionByFlippingVars (List<String>  currentFixings, List<Rectangle> infeasibleHypercubesList) {
        List<String> flippableVars = new ArrayList<String>();
        for (String flipVar: currentFixings){
            
            List<String> updatedCurrentFixings = new ArrayList<String>();
            updatedCurrentFixings.addAll(currentFixings );
            updatedCurrentFixings.removeAll(flippableVars);
            
            if (!isInfeasible(flipVar,updatedCurrentFixings,   infeasibleHypercubesList)) {
                flippableVars.add(flipVar);
            }
        }
        return flippableVars;
    }

    
    public List<String> getBranchingVars () {
         List<String> result = new ArrayList<String>();
         while (!this.mapOfVarFrequency.isEmpty()){
             //System.out.println("map size " + this.mapOfVarFrequency.size());
             result.add(this.getNextBranchingVar());
         }
         return result;
    }
    
    //if we flip the var given the current fixings, will any constraint be vioalted, i.e. will any hypercube be matched ?
    //Or, none of the vars in the hypercube are in the fixings
    private boolean isInfeasible(String flipVar, List<String>  currentFixings, List<Rectangle> infeasibleHypercubesList) {
        boolean isInfeasible = false;
        for (Rectangle rect: infeasibleHypercubesList){
            
            int countOfOneFixingsInThisHypercube = ZERO;
            for (String fixing: currentFixings){
                if (fixing.equals(flipVar)) continue;
                if (rect.zeroFixedVariables.contains( fixing)) countOfOneFixingsInThisHypercube++;
                if (countOfOneFixingsInThisHypercube!=ZERO)  break;
            }
            if (countOfOneFixingsInThisHypercube ==ZERO){
                isInfeasible=true;
                break;
            }
        }
        return isInfeasible;
    }
        
    private void inititializeMap (Collection<Rectangle> infeasibleHypercubesList) {
        
        BranchingVariableSuggestor suggestor = new BranchingVariableSuggestor () ;
        suggestor.getBranchingVar(infeasibleHypercubesList, new ArrayList<String> ());
        
        for (Rectangle rect:infeasibleHypercubesList){
            List<String> varList = new ArrayList<String> ();
            varList.addAll( rect.zeroFixedVariables);
            varList.addAll( rect.oneFixedVariables);
            Map<String, Integer> varmap= new HashMap<String, Integer>();
            
            for (String var: varList) {
                varmap.put (var, suggestor.frequencyMap.get(var)) ;
            }
            mapOfVarFrequency .put (rect, varmap) ;
        }
        
    }
        
    private String getNextBranchingVar () {
        Rectangle rect = this.getRectangleWithFewestHighFrequencyVariables();
        String result = this.getHighestFrequencyVar(rect);
        this.mapOfVarFrequency.remove(rect);
        this.removeHypercubesHavingVar(result);
        //refresh map
        if (!mapOfVarFrequency.isEmpty()) this.inititializeMap( this.mapOfVarFrequency.keySet());
        return result ;
    }
    
    //get rect with fewest vars having frequency >1
    private Rectangle getRectangleWithFewestHighFrequencyVariables ( ) {
        Rectangle result = null;
        Integer lowestFreq = Integer.MAX_VALUE;
        for (Rectangle rect : this.mapOfVarFrequency.keySet()){
            //find # of vars whose freq > 1
            int count = ZERO;
            Map<String, Integer> varmap = this.mapOfVarFrequency.get(rect);
            for (Map.Entry<String,Integer> freq : varmap.entrySet()){
                if (freq.getValue()>ONE  ) count ++;
            }
            if (count <lowestFreq) {
                lowestFreq=count;
                result=rect;
            }else if (count == lowestFreq && rand.nextBoolean()){
                //50%chance of replacement
                lowestFreq=count;
                result=rect;
            }
        }
        
        return result;
    }
    
    private String getHighestFrequencyVar(Rectangle rect) {
          String result = null;
          Map<String, Integer> varmap = this.mapOfVarFrequency.get(rect);
          int max = Collections.max(varmap.values());
          
          
          List<Map.Entry<String,Integer>> entrylist = new ArrayList<>();
          for (Map.Entry<String,Integer> entry:varmap.entrySet()){
              entrylist.add(entry);
          }
          Collections.shuffle( entrylist, rand);
          
          for (Map.Entry<String,Integer> entry : entrylist){
              if (entry.getValue()==max) {
                  result= entry.getKey();
                  break;
              }
          }
          //System.out.println(result+" "+max) ;
          return result;
    }
    
    private void removeHypercubesHavingVar(String var) {
        List<Rectangle> removalList = new ArrayList<Rectangle>();
        for (Rectangle rect: this.mapOfVarFrequency.keySet()){
            Map<String, Integer> varmap = this.mapOfVarFrequency.get(rect);
            if (varmap.keySet().contains(var)) removalList.add(rect);
        }
        
        for (Rectangle rect:removalList){
            this.mapOfVarFrequency.remove(rect);
        }
    }
    
}
