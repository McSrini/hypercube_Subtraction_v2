/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.merge;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import ca.mcmaster.hypercube_subtraction_v2.collection.*;
import java.util.*;

/**
 *
 * @author tamvadss
 * 
 * merge and absorb rectangles
 * 
 */
public class RectangleMerger {
    
    //key is # of fixed vars
    private TreeMap<Integer, List<Rectangle>> rectangleMap = new TreeMap<Integer, List<Rectangle>>();
    
    public boolean isMIP_Infeasible = false;
    
    public RectangleMerger (List<Rectangle> rects) {
        for (Rectangle rect : rects){
            List<Rectangle> current = this.rectangleMap.get(rect.getSize());
            if (null==current) current = new ArrayList<Rectangle>();
            current.add(rect );
            this.rectangleMap.put( rect.getSize(), current);
        }
    }
    
    public List<Rectangle> absorbAndMerge (){
        this.absorb();
        this.merge();
        List<Rectangle> result = new ArrayList<Rectangle> ();
        for ( List<Rectangle> rectList : this.rectangleMap.values()) {
            result.addAll(rectList) ;
        }
        return result;
    }
    
    //absorb rects into other rects in the MAP
    private void absorb () {
        
        int max =  this.rectangleMap.lastKey();
        int min =  this.rectangleMap.firstKey();
        for (int currentDepth = max; currentDepth > min ; currentDepth -- ){
            
            //check for absoption into higher levels
            
            List<Rectangle> rectanglesAtCurrentDepth = this.rectangleMap.remove(currentDepth );
            if (null==rectanglesAtCurrentDepth) continue;
            
            List<Rectangle> newRectanglesAtCurrentDepth = new ArrayList<Rectangle> ();
            //check is any of the higher level rects will absorb these rects, if no then retain else ignore(i.e. discard)
            for (Rectangle rect : rectanglesAtCurrentDepth){
                if (!isAbsorbed(rect)) {
                    newRectanglesAtCurrentDepth.add(rect);
                } 
            }
            if (newRectanglesAtCurrentDepth.size()>ZERO) this.rectangleMap.put( currentDepth, newRectanglesAtCurrentDepth );
        }
        
    }
    
    //is this rect which is at currentDepth absorbed into any lesser depth rect ?
    private boolean isAbsorbed(Rectangle currentRect){
        boolean result = false;
        
        int min =  this.rectangleMap.firstKey();
        int currentRectDepth = currentRect.getSize();
        for (int depth = min;depth <  currentRectDepth; depth ++ ){
            //does any rect at this depth absorb the current rect ?
            if (this.rectangleMap.get( depth)==null) continue;
            for ( Rectangle rect: this.rectangleMap.get( depth)){
               if (rect.isAbsorbed( currentRect))   {
                   result = true;
                   break;
               }
            }
            
            if (result) break;
        }
        
        return result;
    }
    
    //merge rects with other rects in map that have 1 complimentary variable
    private void merge () {
       
        int max =  this.rectangleMap.lastKey();
        int min =  this.rectangleMap.firstKey();
        
        //merge from largest depth first, as this may give you more rects at higher depths
        for (int currentDepth = max; currentDepth >= min ; currentDepth -- ){
            
            List<Rectangle> rectanglesAtCurrentDepth = this.rectangleMap.remove(currentDepth );
            if (null==rectanglesAtCurrentDepth) continue;
            
            //check if any of the rects at the current depth differ in 
            //only 1 complimentarty var branching. if so remove both and 
            //insert merged rect in the level above. Any rects not removed thusly end up in newRectanglesAtCurrentDepth            
            List<Rectangle> rectanglesToBeRemovedFromThisDepth = new ArrayList<Rectangle> ();
            for (Rectangle rectOne: rectanglesAtCurrentDepth) {
                
                if (rectanglesToBeRemovedFromThisDepth.contains( rectOne)) continue;
                
                boolean isComplimentFound = false;
                Rectangle complimentaryRect= null;
                for (Rectangle rectTwo: rectanglesAtCurrentDepth) {
                    Rectangle mergedRect = rectOne.mergeIfComplimentary ( rectTwo);
                    if (null!= mergedRect) {
                        //add merged rect upstairs
                        List<Rectangle> currentRectsOneLevelAbove= this.rectangleMap.remove( currentDepth-ONE);
                        if (currentRectsOneLevelAbove==null) currentRectsOneLevelAbove = new ArrayList<Rectangle>();
                        currentRectsOneLevelAbove.add(mergedRect);
                        this.rectangleMap.put( currentDepth-ONE, currentRectsOneLevelAbove);
                        
                        isComplimentFound= true;
                        complimentaryRect= rectTwo;
                        break;
                    }   
                }
                if (!isComplimentFound) {
                    //no complimentary rect for rectOne, keep it at this depth                     
                }else {
                    //remove both this rect and its compliment
                    rectanglesToBeRemovedFromThisDepth.add(rectOne) ;
                    rectanglesToBeRemovedFromThisDepth.add(complimentaryRect) ;
                }
            }
            
            List<Rectangle> newRectsAtThisDepth = new ArrayList<Rectangle> ();
            for (Rectangle rect : rectanglesAtCurrentDepth) {
                if (!rectanglesToBeRemovedFromThisDepth.contains(rect)) newRectsAtThisDepth.add(rect);
            }
            if (newRectsAtThisDepth.size()>ZERO) this.rectangleMap.put( currentDepth, newRectsAtThisDepth );
 
        }
 
        if (this.rectangleMap.containsKey(ZERO)        ) this.isMIP_Infeasible=true;
    }
}
