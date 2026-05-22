import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;


public class OpeningBookTable implements Serializable {
      
       /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = 8140495183635111979L;
    public OpeningBookEntry[] book;
    private transient int table_size;
    private int storedPositions = 0;
    private transient int maxValue;
    private transient Game game;
   
    
    private OpeningBookTable(Game s, int size){
    	
        table_size = 1<<size;
        book = new OpeningBookEntry[table_size]; 
        try{
        	game = s.getClass().getDeclaredConstructor().newInstance();
        }catch
        (IllegalAccessException | InstantiationException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e){e.printStackTrace();}
        
        
    }
    
    private OpeningBookTable(){}
    
    public static OpeningBookTable getInstance(Game s){
    	    	    		
    		OpeningBookTable book = load(s);
    		
    		if(book ==null){    
    		    return new OpeningBookTable(s,14);
    		}
    
    	
     return book;
    }
    
    public static OpeningBookTable load(Game s){
    	
    
    	OpeningBookTable bookTable = new OpeningBookTable();
    	 
    	try{
        bookTable.game = s.getClass().getDeclaredConstructor().newInstance();
        Game sbe = bookTable.game;
 
        long starttime = System.currentTimeMillis();
      
        String name = sbe.bookName+Board.HEIGHT +"x"+Board.WIDTH;
        URL url = bookTable.getClass().getResource(name);
        if(url==null)
        	return null;
            
        try(ObjectInputStream in= new ObjectInputStream(
                new BufferedInputStream(url.openStream(),1 << 12));){
        
            Object dummy =  in.readObject();
            final Field fields[] =
            dummy.getClass().getDeclaredFields();
           
            Log.println(
                    "Book Time:" + (System.currentTimeMillis() - starttime));
            
            for (int i = 0; i < fields.length; ++i) {
     			 if ("book".equals(fields[i].getName())) {
                 fields[i].setAccessible(true);
                bookTable.book = ( OpeningBookEntry[]) fields[i].get(dummy);
                 
                 }                 
                 if ("storedPositions".equals(fields[i].getName())) {
                 fields[i].setAccessible(true);
               bookTable.storedPositions = fields[i].getInt(dummy);
                 }     
            }
            
        	/*for(int i = bookTable.book.length; --i>=0;){
        		
        		for(OpeningBookEntry entry = bookTable.book[i]; entry!=null; entry = entry.next){
        			counter++;}}
        	Log.println("Number of positions counted: " + counter);*/

            	
           /* book =  dummy.book;
            table_size = dummy.table_size;
            storedPositions = dummy.storedPositions;*/
            
            bookTable.table_size = bookTable.book.length;
            Board dummyBoard = new Board(sbe);
            sbe.sizeTT = 8;
            dummyBoard.startingConfig(sbe, sbe);
            
            OpeningBookEntry root= bookTable.retrieve(dummyBoard);
           
           
            //WARNING
            //Check if programs hashvalues are the same as OpeningsBook
            //In 1/tablesize this approach passes no-matching hashvalues
            
            if(root == null || bookTable.book.length<100){
            	          
            	Log.println("rebuildBook");
                OpeningBookTable dumbook = new OpeningBookTable(s,14);
               
                int counter =0;
            	
            	for(int i = bookTable.book.length; --i>=0;){
            		
            		for(OpeningBookEntry entry = bookTable.book[i]; entry!=null; entry = entry.next){
            			counter++;
            			
            			OpeningBookEntry dummer =OpeningBookEntry.createInstance(entry);
            			dummer.computeHash();
            			long hash_value = dummer.hash;
					///	dummer.setBestNewMove(dummer.getBestMove(sbe));
		
            			 int lock = (int) (hash_value & (dumbook.table_size-1));
                   		     
            			 dummer.next = dumbook.book[lock];
            			 dumbook.book[lock] = dummer;
            		}
           
            	}
            
            	Log.println("Number of positions counted: " + counter);
            	
            	if(counter!=bookTable.storedPositions){
            		Log.println(counter + " " + bookTable.storedPositions);
            		return null;
            	}
            	
            	//Log.println(dumbook.table_size +  " "+dumbook.book.length);
            	
            	bookTable.book = dumbook.book;
            	bookTable.table_size = bookTable.book.length;
            	root=bookTable.retrieve(dummyBoard);
            	bookTable.save();            
            }
    
                        bookTable.maxValue = root.Value;
            Log.println("Number of positions: " + bookTable.storedPositions);}
            catch(IOException e){e.printStackTrace(); return null;}
    	}catch( IllegalArgumentException | IllegalAccessException  | InstantiationException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException e){
    	e.printStackTrace(); return null;
    	}
    
    	return bookTable;    	
    }
    
   public static final int RANDOM_FACTOR = 15;
   

    public Move getBookMove(Board b) {
	
        b.initialise();
        int side =  b.sideToMove;
        OpeningBookEntry entry = retrieve(b);
       
        //Hack Loa
        if (entry == null ||  entry.depth == 0) {
            if (entry == null) {
                Log.println("Out the book");
            } else {
                Log.println("Not an explored position");
            }
            return null;
        }

        Move best_move = entry.getBestMove(game);
        
      

        maxValue = (side * entry.Value > side * maxValue)
                ? entry.Value
                : maxValue;
        Log.println(" " + entry.Value + " " + entry.depth);
        //Log.println(" " + maxValue);

        if (entry.depth != 1) {
            int max_score = (Game.isRandom)?-Board.WIN_SCORE:-Board.WIN_SCORE*side;
            int val;

            for(Move m = game.getGenerator().generateMoves(b, side); m != null; m = m.next) {
                m.apply(b);
                entry = retrieve(b);
                
               
                if(entry != null && entry.depth >= 0){
                	
                	int value =  entry.Value;
                	//hack to handle GHI
                	if(game.checkOnDraw(b)>=1)	//b.checkOnDraw())
                		value =0;
                	
                	if(Game.isRandom){
	                   if (value * side > side * maxValue - RANDOM_FACTOR ) {
	                    val = (int) (RANDOM_FACTOR *2 * Math.random() - RANDOM_FACTOR );
	                    
	                    if (val > max_score) {
	                        max_score = val;
	                        best_move = m;
	                        best_move.score = value;
	                    }
	                }
                	}
                 else if(value*side >side*max_score) {                            
                            best_move = m;
                            max_score = value;
                            best_move.score = value;
                        }
                }
                
                m.undo(b);
            }
        }
        else
        	for(Move m = game.getGenerator().generateMoves(b, side); m != null; m = m.next)
        		if(m.equals(best_move)){
        			best_move =m;
        			best_move.score = entry.Value;
        		}
    
        
  
        Log.println("Book move: " + best_move + " " + best_move.score);
        return best_move;
    }
    
    //problematic for games such as breakthough, knighttrough, dameo, where symmetries do not hold
    public OpeningBookEntry retrieve(Board b) {
    	
    	OpeningBookEntry dummy =  OpeningBookEntry.createInstance(b);
        OpeningBookEntry entry = getEntry(dummy);
    
           if (entry != null) {
               return entry;
           }
              
          dummy.setMirrorXEntry(dummy,game);
          entry = getEntry(dummy);
      
           if (entry != null) {
        	   entry.setPosition(b);
               Move m = entry.getBestMove(game);
               m.mirrorX();   
               entry.setBestMove(m);
      
               return entry;
           } 
      
           dummy.setMirrorXEntry(dummy,game);
           dummy.setMirrorYEntry(dummy,game);
        
           
           entry = getEntry(dummy);
      
           if (entry != null) {
           	  entry.setPosition(b);
               Move m = entry.getBestMove(game);
               m.mirrorY();   
               entry.setBestMove(m);
               return entry;
           } 
      
           dummy.setMirrorXEntry(dummy,game);
           
           entry = getEntry(dummy);
      
           if (entry != null) {
               entry.setPosition(b);
      
               Move m = entry.getBestMove(game);
      
               m.mirrorXY();
            
               entry.setBestMove(m);
      
               return entry;
           } 

       	
   		return null;
       }

      private OpeningBookEntry getEntry(OpeningBookEntry dummy) { 
           int lock = (int) (dummy.hash & (table_size-1));
    
           for (OpeningBookEntry entry = book[lock]; entry != null; entry = entry.next) {
               if (entry.equals(dummy)) {
                   return OpeningBookEntry.createInstance(entry);
               }
           }
         
           return null;  
      }
    
 /*   public OpeningBookEntry Retrieve(Board b) {
        
        OpeningBookEntry entry = getEntry(b);
 
        if (entry != null) {
            return entry;
        }
         
        dummyBoard.sideToMove = b.sideToMove;
        dummyBoard.posB = Symmetry.mirrorX(b.posB);   
        dummyBoard.posR = Symmetry.mirrorX(b.posR);   
 
        dummyBoard.hashValue = TranspositionTable.getHashValue(dummyBoard.posB, dummyBoard.posR, dummyBoard.sideToMove);
        entry = getEntry(dummyBoard);
   
        if (entry != null) {
            entry.setPosition(b);
            Move m = entry.getBestMove(b.getSBE());

            m.mirrorX();   
   
            // Log.println("Horizontale (X) spiegeling: " + m.toString());
            entry.setBestMove(m);
   
            return entry;
        } 
   
        dummyBoard.posB = Symmetry.mirrorY(b.posB);   
        dummyBoard.posR = Symmetry.mirrorY(b.posR);   
 
        dummyBoard.hashValue = TranspositionTable.getHashValue(dummyBoard.posB, dummyBoard.posR, dummyBoard.sideToMove);
        entry = getEntry(dummyBoard);
   
        if (entry != null) {
        	entry.setPosition(b);
            Move m = entry.getBestMove(b.getSBE());

            m.mirrorY();   
   
            // Log.println("Verticale (Y) spiegeling: " + m.toString());
            entry.setBestMove(m);
            return entry;
        } 
   
        dummyBoard.posB = Symmetry.mirrorX(Symmetry.mirrorY(b.posB));   
        dummyBoard.posR = Symmetry.mirrorX(Symmetry.mirrorY(b.posR));   
 
        dummyBoard.hashValue = TranspositionTable.getHashValue(dummyBoard.posB, dummyBoard.posR, dummyBoard.sideToMove);
        entry = getEntry(dummyBoard);
   
        if (entry != null) {
        	entry.setPosition(b);
   
            Move m = entry.getBestMove(b.getSBE());
   
            m.mirrorY();
            m.mirrorX();
   
            // Log.println("Gecombineerde spiegeling: " + m.toString());
            entry.setBestMove(m);
   
            return entry;
        } 
 
        return null;
    }*/

    /* public OpeningBookEntry Retrieve(Board b) {
       
	 OpeningBookEntry entry = getEntry(b);
 
        if (entry != null) {
            return entry;
        }
           
        dummyBoard.sideToMove  = b.sideToMove;
        dummyBoard.setSpace(b.space);
        
        Symmetry.mirrorX(dummyBoard.space);   
        dummyBoard.initialise();
        
        entry = getEntry(dummyBoard);
   
        if (entry != null) {
        	entry.setPosition(b);
            Move m = entry.getBestMove(b.getSBE());

            m.mirrorX();   
   
            // Log.println("Horizontale (X) spiegeling: " + m.toString());
            entry.setBestMove(m);
   
            return entry;
        } 
   
        dummyBoard.setSpace(b.space);
        Symmetry.mirrorY(dummyBoard.space);   
        dummyBoard.initialise();
        
        entry = getEntry(dummyBoard);
   
        if (entry != null) {
        	entry.setPosition(b);
            Move m = entry.getBestMove(b.getSBE());

            m.mirrorY();   
   
            // Log.println("Verticale (Y) spiegeling: " + m.toString());
            entry.setBestMove(m);
            return entry;
        } 
   
        Symmetry.mirrorX(dummyBoard.space);   
        dummyBoard.initialise();
        
        entry = getEntry(dummyBoard);
   
        if (entry != null) {
            entry.setPosition(b);
   
            Move m = entry.getBestMove(b.getSBE());
   
            m.mirrorY();
            m.mirrorX();
   
            // Log.println("Gecombineerde spiegeling: " + m.toString());
            entry.setBestMove(m);
   
            return entry;
        } 

    	
		return null;
    }

   private OpeningBookEntry getEntry(Board b) { 
        int lock = (int) (b.hashValue & (table_size-1));
 
        for (OpeningBookEntry entry = book[lock]; entry != null; entry = entry.next) {
            if (entry.equals(b)) {
                return OpeningBookEntry.createInstance(entry);
            }
        }
      
        return null;  
    }*/

    private boolean setEntry(OpeningBookEntry dup) {
        int lock = (int) (dup.hash & (table_size-1));
 
        for (OpeningBookEntry entry = book[lock]; entry != null; entry = entry.next) {
            if (dup.equals(entry)) {
                entry.copyRest(dup);
                return true;
            }
        }
        return false; 
    }

    public void store(OpeningBookEntry entry) {
        
        if (setEntry(entry)) {
            return;
        } // saved
 
        OpeningBookEntry dummy = OpeningBookEntry.createInstance(entry);
        
   
        dummy.setMirrorXEntry(entry,game);
        
         
        if (setEntry(dummy)) {
            return;
        } // saved
   
        dummy.setMirrorYEntry(entry,game);
        
        if (setEntry(dummy)) {
            return;
        } // saved

        dummy.setMirrorXYEntry(entry,game);
        
        if (setEntry(dummy)) {
            return;
        } // saved

        // Never seen before
      
        int lock = (int) (entry.hash & (table_size-1));

        entry.next = book[lock];
        book[lock] = entry; 
        storedPositions++;
    }

    public synchronized void save() {
   
    	String fileName = game.bookName+Board.HEIGHT +"x"+Board.WIDTH;
    	String fileBakName = "bak" + fileName;
    
        try {
        	
        	File file = new File(fileName);
        	if(file.exists()){
        		File bakFile = new File(fileBakName);
        		if(bakFile.exists())
        			bakFile.delete();
        		
        		file.renameTo(bakFile);
        	}
        		
        	try(ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(fileName)));){
        	   out.writeObject(this);
        	} 
        } catch (IOException e) {
            Log.println("Save Problem of the opening book ");
        }  
      
    }    
}
