/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.common;
 
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ZERO;
import ca.mcmaster.hypercube_subtraction_v2.TestDriver;


/**
 *
 * @author tamvadss
 */
public class VariableCoefficientTuple  implements Comparable{
        
    public String varName ;
    public double coeff;
    
    public VariableCoefficientTuple (String varname, double coefficient)   {
    
        this.varName  =  varname; 
        coeff =coefficient;
    }
       
    //lowest magnitude coeff first
    public int compareTo(Object other) {
        int result =ZERO;
        //return this - other
        if (ZERO== Double.compare(Math.abs(this.coeff ), Math.abs(  ((VariableCoefficientTuple)other).coeff ))) {
            //largest magnitude objective coefficient first 
            double thisMagn = TestDriver.objective.getObjectiveCoeffMagnitude(this.varName);
            double otherMagn= TestDriver.objective.getObjectiveCoeffMagnitude(((VariableCoefficientTuple)other).varName);
            result = Double.compare(  otherMagn , thisMagn )   ; //this.varName.compareTo(  ((VariableCoefficientTuple)other).varName  );
        }else {
            result= Double.compare(Math.abs(this.coeff ), Math.abs(  ((VariableCoefficientTuple)other).coeff ));
        }
        return result;
    } 
    
    /*public String toString (){
        return "Tuple is " + this.varName + " " + this.coeff;
    }*/
}
