
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.merge;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.LOGGING_LEVEL;
import ca.mcmaster.hypercube_subtraction_v2.collection.*;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

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
    private List<Rectangle> incomingRectangles;
    
    public boolean isMIP_Infeasible = false;
    
    private static Logger logger=Logger.getLogger(RectangleMerger.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,LOG_FOLDER+RectangleMerger.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
             
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    public RectangleMerger ( List<Rectangle> initialRectangles, List<Rectangle> incomingRectangles) {
        
        List<Rectangle>  allRects = new ArrayList<Rectangle> ();
        allRects.addAll(initialRectangles );
        allRects.addAll(incomingRectangles );
        
        for (Rectangle rect : allRects){
            List<Rectangle> current = this.rectangleMap.get(rect.getSize());
            if (null==current) current = new ArrayList<Rectangle>();
            current.add(rect );
            this.rectangleMap.put( rect.getSize(), current);
        }
        
        this.incomingRectangles = incomingRectangles ;
        
    }
    
    public List<Rectangle> absorbAndMerge (){
        
        logger.debug("num of infeasible cubes collected before absorb "+ this.getNumberOFRectsInMap()) ;
        int countAbsorbed=this.absorb(   );
        logger.debug("num of infeasible cubes collected  after absorb"+ this.getNumberOFRectsInMap());
        if (countAbsorbed>ZERO) logger.info ( " and count absorbed "+ countAbsorbed) ;

        int countMerged =this.merge(   );
        logger.debug("num of infeasible cubes collected after merge  "+ this.getNumberOFRectsInMap()  ) ;
        if (countMerged>ZERO) logger.info ( " and count countMerged "+ countMerged) ;
        
        List<Rectangle> result = new ArrayList<Rectangle> ();
        for ( List<Rectangle> rectList : this.rectangleMap.values()) {
            result.addAll(rectList) ;
        }
        return result;
    }
    
    
    //absorb newly added rects  
    private int absorb (   ) {
        
        int countAbsorbed = ZERO;
        
        int max =  this.rectangleMap.lastKey();
        int min =  this.rectangleMap.firstKey();
        for (int currentDepth = max; currentDepth > min ; currentDepth -- ){
            
            //check for absoption into higher levels
            
            List<Rectangle> rectanglesAtCurrentDepth = this.rectangleMap.remove(currentDepth );
            if (null==rectanglesAtCurrentDepth) continue;
            
            List<Rectangle> newRectanglesAtCurrentDepth = new ArrayList<Rectangle> ();
            //check is any of the higher level rects will absorb these rects, if no then retain else ignore(i.e. discard)
            for (Rectangle rect : rectanglesAtCurrentDepth){
                
                //only try to absorb new rects, let the old ones sit there
                if ( incomingRectangles.contains(rect))  {
                    logger.debug("check if rect will be absorbed "+ rect.printMe("")) ;
                    if (!isAbsorbed(rect)) {
                        newRectanglesAtCurrentDepth.add(rect);
                    } else {
                        countAbsorbed++;
                    }
                }else {
                    newRectanglesAtCurrentDepth.add(rect);
                }
                
            }
            if (newRectanglesAtCurrentDepth.size()>ZERO) this.rectangleMap.put( currentDepth, newRectanglesAtCurrentDepth );
        }
        
        return countAbsorbed;
        
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
                   logger.debug (currentRect.printMe( "") + " absorbed into " +rect.printMe("") ) ;
                   break;
                }
            }
            
            if (result) break;
        }
        
        return result;
    }
    
    //merge newRectangles with other rects (if any) in map that have 1 complimentary variable
    private int merge ( ) {
        
        int countMerged = ZERO;
       
        int max =  this.rectangleMap.lastKey();
        //int min =  this.rectangleMap.firstKey();
        
        //merge from largest depth first, as this may give you more rects at higher depths
        for (int currentDepth = max; currentDepth >ZERO ; currentDepth -- ){
            
            List<Rectangle> rectanglesAtCurrentDepth = this.rectangleMap.remove(currentDepth );
            if (null==rectanglesAtCurrentDepth) continue;
            
            logger.debug("current depth for merge is "+ currentDepth);
            
            
            //check if any of the rects at the current depth differ in 
            //only 1 complimentarty var branching. if so remove both and 
            //insert merged rect in the level above. Any rects not removed thusly end up in newRectanglesAtCurrentDepth            
            List<Rectangle> rectanglesToBeRemovedFromThisDepth = new ArrayList<Rectangle> ();
            for (Rectangle rectOne: rectanglesAtCurrentDepth) {
                
                if (!incomingRectangles.contains(rectOne )) continue ;
                logger.debug("trying to merge "+rectOne.printMe(""));
                
                if (rectanglesToBeRemovedFromThisDepth.contains( rectOne)) continue;
                
                boolean isComplimentFound = false;
                Rectangle complimentaryRect= null;
                for (Rectangle rectTwo: rectanglesAtCurrentDepth) {
                    
                    logger.debug("trying to MATCH with "+rectTwo.printMe(""));
                     
                    Rectangle mergedRect = rectOne.mergeIfComplimentary ( rectTwo);
                    if (null!= mergedRect) {
                        //add merged rect upstairs
                        List<Rectangle> currentRectsOneLevelAbove= this.rectangleMap.remove( currentDepth-ONE);
                        if (currentRectsOneLevelAbove==null) currentRectsOneLevelAbove = new ArrayList<Rectangle>();
                        currentRectsOneLevelAbove.add(mergedRect);
                        this.rectangleMap.put( currentDepth-ONE, currentRectsOneLevelAbove);
                        
                        isComplimentFound= true;
                        complimentaryRect= rectTwo;
                        countMerged ++;
                        logger.debug (rectOne.printMe( "") + " merged with " + rectTwo.printMe( "")) ;
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
            }//end for rectOne
            
            List<Rectangle> newRectsAtThisDepth = new ArrayList<Rectangle> ();
            for (Rectangle rect : rectanglesAtCurrentDepth) {
                if (!rectanglesToBeRemovedFromThisDepth.contains(rect)) newRectsAtThisDepth.add(rect);
            }
            
            if (newRectsAtThisDepth.size()>ZERO) {
                this.rectangleMap.put( currentDepth, newRectsAtThisDepth );
                logger.debug("number of rects left at level "+currentDepth+ " is " +newRectsAtThisDepth.size()) ;
            }else{
                logger.debug("no rects left at level "+currentDepth) ;
            }
 
        }
 
        if (this.rectangleMap.containsKey(ZERO)        ) this.isMIP_Infeasible=true;
        return countMerged;
    }
    
    private int getNumberOFRectsInMap () {
        int size = ZERO;
        
        for (List<Rectangle> rects :rectangleMap.values()) {
            size+=rects.size();
            for (Rectangle rect: rects){
                logger.debug (rect.printMe("")) ;
            }
        }
        
        return size;
    }
}
