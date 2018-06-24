/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 * 
 * this class represents a collected infeasible hypercube
 * 
 */
public class Rectangle {
    
    
    //note that some vars can be free
    public List <String> zeroFixedVariables = new ArrayList <String>();
    public List <String> oneFixedVariables = new ArrayList <String>();
     
    
    public Rectangle (List <String> zeroFixedVariables , List <String> oneFixedVariables ){
        this.zeroFixedVariables .addAll(zeroFixedVariables);
        this.oneFixedVariables  .addAll( oneFixedVariables);      
    }
    
    public int getSize () {
        return zeroFixedVariables.size()+ this.oneFixedVariables.size();
    }
    
    //if other rect is complimenarty , then return merged rect else return null
    public Rectangle mergeIfComplimentary (Rectangle other) {
        Rectangle result = null;
        
        int myZeroSize = this.zeroFixedVariables.size();
        int myOneSize = this.oneFixedVariables.size();
        int otherZeroSize = other.zeroFixedVariables.size();
        int otherOneSize = other.oneFixedVariables.size();
        
        boolean isSizeMatchOne = ( (myZeroSize == otherZeroSize-ONE) &&(myOneSize==otherOneSize+ONE)) ;
        boolean isSizeMatchTwo =   ( (myZeroSize == otherZeroSize+ONE) &&(myOneSize==otherOneSize-ONE)) ;
        
        boolean isComplimentary = true;
        
        if (isSizeMatchOne) {
            List < String> extraZeroVar = new ArrayList<String> ();
            for (String var : other.zeroFixedVariables) {
                if ( !this.zeroFixedVariables.contains(var)) extraZeroVar.add(var);
                if (extraZeroVar.size()>ONE){
                    //not complimentary
                    isComplimentary= false;
                    break;
                }
            }
            
            List < String> extraOneVar = new ArrayList<String> ();
            if (isComplimentary) {
                for (String var : this.oneFixedVariables  ) {
                    if ( !other.oneFixedVariables.contains(var)) extraOneVar.add(var);
                    if (extraOneVar.size()>ONE){
                        //not complimentary
                        isComplimentary= false;
                        break;
                    }
                }
            }
            
            if (isComplimentary && extraOneVar.get(ZERO).equals(extraZeroVar.get(ZERO))) {
                //well and truly complimentary
                result = new Rectangle (this.zeroFixedVariables,other.oneFixedVariables) ;
            }
            
        }else if (isSizeMatchTwo) {
            
            // ( (myZeroSize == otherZeroSize+ONE) &&(myOneSize==otherOneSize-ONE))
            
            List < String> extraZeroVar = new ArrayList<String> ();
            for (String var : this.zeroFixedVariables) {
                if ( !other.zeroFixedVariables.contains(var)) extraZeroVar.add(var);
                if (extraZeroVar.size()>ONE){
                    //not complimentary
                    isComplimentary= false;
                    break;
                }
            }
            
            List < String> extraOneVar = new ArrayList<String> ();
            if (isComplimentary) {
                for (String var : other.oneFixedVariables  ) {
                    if ( !this.oneFixedVariables.contains(var)) extraOneVar.add(var);
                    if (extraOneVar.size()>ONE){
                        //not complimentary
                        isComplimentary= false;
                        break;
                    }
                }
            }
            
            if (isComplimentary && extraOneVar.get(ZERO).equals(extraZeroVar.get(ZERO))) {
                //well and truly complimentary
                result = new Rectangle (other.zeroFixedVariables,this.oneFixedVariables) ;
            }
        }
        
        return result;
    }
    
    //is other absorbed into this ?
    public boolean isAbsorbed (Rectangle other) {
        boolean result = true ;
        
        //check if all my zero fixings are in other
        for (String var : this.zeroFixedVariables){
            if (!other.zeroFixedVariables.contains(var)) {
                result = false ;
                break;
            }   
        }
        
        //check if all my one fixings are in other
        if (result) {
            for (String var : this.oneFixedVariables){
                if (!other.oneFixedVariables.contains(var)) {
                    result = false ;
                    break;
                }   
            }
        }
        
        return result;
    }
    
     
    /*public String toString (){
        
        String result=" "; 
        result += " --- Zero fixed vars :";
        for (String str: zeroFixedVariables){
            result += str + ",";
        }
        result += "  -- One fixed vars :";
        for (String str: oneFixedVariables){
            result += str + ",";
        }
        return result;

    }*/
    public void printMe (String name){
        
        String result=" "; 
        result += " --- Zero fixed vars :";
        for (String str: zeroFixedVariables){
            result += str + ",";
        }
        result += "  -- One fixed vars :";
        for (String str: oneFixedVariables){
            result += str + ",";
        }
        System.out.println( "Best vertex for "+ name + " Is " + result);

    }
    
     
}
