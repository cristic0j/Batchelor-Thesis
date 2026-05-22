import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
/*
 * Created on 9-mei-2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import java.util.ListIterator;

/**
 * @author Mark Winands(Administrator)
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class EndGameDatabaseFile {

	private static int whitePieces=6;
	private static int blackPieces=1;
	private static long[] max = new long[2];
	private static int progresscounter=0;
	
	protected static final byte DRAW = 3;
	protected static final byte WIN =1;
	protected static final byte LOSS =2;
	private static final int DRAWVALUE =-100;
	protected Game gameAI;
	protected Generator generator;
	private final Board b = new Board();
	
	private final byte [][] confR = new byte[Board.HEIGHT][Board.WIDTH];
	private AbstractList<Long> firstList = null; 
	private AbstractList<Long> secondList = null; 

	private static final int LIST_SIZE = 1<<21;	

	@SuppressWarnings("rawtypes")
	private LinkedList[][] allPages = new  LinkedList[24][24];
	private final static int PAGE_SIZE = 16; //2^16
	private final static long PAGE_MASK = ((1L<<PAGE_SIZE)-1);
	private final static int MAX_PAGES = 10;
	
	private  byte[][][] egdbMemory;

	private  boolean loadDataBaseInMemory = false;
	private RandomAccessFile[][] egdb;
	private final static String load = "databases" + File.separator;
	

	
	public static void main(String[] args)
	{
		if (args.length==2)
		{
			whitePieces=Integer.parseInt(args[0]);
			blackPieces=Integer.parseInt(args[1]);
		}
		
		
		long startTime = System.currentTimeMillis();
		Game game = new SurakartaGame();
		game.isMemEGDB = false;
		
		Board.setSize(game);
		new EndGameDatabaseFile(game).generate();
		final long endTime = System.currentTimeMillis();
			
		Log.println("Time needed: "+(endTime-startTime));
			
	}
	
	
	public static EndGameDatabaseFile createEndgameDatabase(Game game){
		
		EndGameDatabaseFile db  =null;
		
		 try {
			db = (EndGameDatabaseFile) Class.forName(	game.getGameName() +  "EndGameDatabaseFile").getDeclaredConstructor().newInstance();	
			db.initialiseDataBase(game);
		
		 } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			 return new EndGameDatabaseFile(game);}
	
		return db;
		
	}
	
	protected EndGameDatabaseFile(){
		
		
	}
	
	protected EndGameDatabaseFile(Game game){
		
		initialiseDataBase(game);
	}
	
	@SuppressWarnings("rawtypes")
	protected void initialiseDataBase(Game game)
	{		
		//LoaCombinatorics.createSymmetryLists(whitePieces, blackPieces);
		loadDataBaseInMemory = game.isMemEGDB;
		
		if(!loadDataBaseInMemory)
		for(int i =allPages.length; --i>=0;)
			for(int j =allPages.length; --j>=0;)
				allPages[i][j] = new LinkedList();
		
		try{
		gameAI  = game.getClass().getDeclaredConstructor().newInstance();}
		catch(IllegalAccessException | InstantiationException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e){e.printStackTrace();}
		
		generator =  gameAI.getGenerator();
			
		Log.println("SwapNumber: "+Board.SQUARE);
		
		openDatabase();
		
		//System.out.println("test: " + loadDataBaseInMemory);
		
		if(loadDataBaseInMemory){
			closeDatabase();
			//System.out.println("test: " + loadDataBaseInMemory);
		}
				
	}
	
	public  void generate(){
		
		try
		{
			do{
	
			long halfLength=Combinatorics.possiblePositions((byte)Board.SQUARE, (byte)whitePieces, (byte)blackPieces);
			max[0]=halfLength;
			max[1]=halfLength;
			
			Log.println("Length: "+halfLength);
			
			boolean finished=false;
			Log.println();
			Log.println("Initializing...");
			
			//long startTime = System.currentTimeMillis();
			initialize((halfLength/4)+1);
			//long endTime = System.currentTimeMillis();
			
			//Log.println("Ini Time needed: "+(endTime-startTime));
			
			
			Log.print("0: ");
			Log.println(progresscounter+"/"+max[0]*2);
			
			firstList = null;
			secondList = null;
			
			byte counter=0;
			while (!finished)
			{
				counter++;
				Log.print(counter +": ");
				finished = retroStep(counter, 	whitePieces, blackPieces);
			}
			
			//startTime = System.currentTimeMillis();
			assignDraws(whitePieces, blackPieces );
			//endTime = System.currentTimeMillis();
			
			///Log.println("Draw Time needed: "+(endTime-startTime));
					
		
		Log.println("Finished: " + whitePieces + " " + blackPieces);
		whitePieces--;
		blackPieces++;
		}while(false);//whitePieces>=blackPieces);
		closeDatabase();
		} catch (IOException e) {e.printStackTrace();}
		Log.println("Finished");
	}
	
	private void initialize(long length)
	{
		initialize(whitePieces, blackPieces,length);
		
		if(whitePieces!=blackPieces)
			initialize(blackPieces, whitePieces, length);
	}
	
	/*private  void openDatabase() throws Exception
	{
		egdb=new RandomAccessFile[whitePieces+blackPieces-1][whitePieces+blackPieces-1];
		egdbMemory=new byte[whitePieces+blackPieces-1][whitePieces+blackPieces-1][];
		
		String s = this.gameAI.getGameName();
		for (int i=0;i<whitePieces+blackPieces-1;i++)
			for (int j=0;j<whitePieces+blackPieces-1;j++){
				egdb[i][j]= new RandomAccessFile(s + " "+(Board.HEIGHT)+"x"+(Board.WIDTH)+" "+(i+1)+"-"+(j+1)+" EGDB.txt","rw");
					if (loadDataBaseInMemory){			
						egdbMemory[i][j] = new byte[(int)egdb[i][j].length()];
						egdb[i][j].read(egdbMemory[i][j]);
						//System.out.println(i+"-"+j+" Size: "+egdbMemory[i][j].length);
					}
			}
	}*/
	
	
	private void initialize(int wP, int bP, long length){
		
	
	try
	{	
		/*if(egdb[wP-1][bP-1].length()>100)
			return;*/
		
		if(egdb[wP-1][bP-1]==null){
		
			
			String name = load+ gameAI.getGameName() + File.separator + gameAI.getGameName() + " " +(Board.HEIGHT)+"x"+(Board.WIDTH)+" "+ wP+"-"+ bP+" EGDB.txt";
			egdb[wP-1][bP-1] = new RandomAccessFile(name,"rw");
		}
		
		
		
		egdb[wP-1][bP-1].setLength(length);
		egdb[wP-1][bP-1].seek(0);
		byte buffer[] = new byte[1024];
		long len = length;
		
		do{
			int counter = (buffer.length<len)?buffer.length:(int)len;		
			egdb[wP-1][bP-1].write(buffer,0,counter);
			len-=counter;
		}while(len>0);
		
		
	} catch (IOException e) { e.printStackTrace();}
		
	/*	try
		{	
			egdb[wP-1][bP-1].seek(0);
			for (long i=0;i<length;i++)
			{
				//egdb[whitePieces-1][blackPieces-1].seek(i);
				egdb[wP-1][bP-1].writeByte(0);
			}
		} catch (Exception e) { e.printStackTrace();}*/
	}
	
private boolean retroStep(int round, int wP, int bP, int phase, AbstractList<Long> list){
	
	byte[][] conf  = new byte[Board.HEIGHT][Board.WIDTH];

	boolean changed=false;
	long tmp=0;
	
	//final byte [][] hulpConf = new byte[Board.HEIGHT][Board.WIDTH];
	long ind2;
	long ind3;
	long ind4;
	
	
	try
	{			
			final long length = max[phase];
			DataBasePage page=null;
			
			for (long i=0;i<length;i++)
			{	
				if((i&PAGE_MASK)==0)
					 page=new DataBasePage(egdb[wP-1][bP-1],(int)(i>>PAGE_SIZE),(int)PAGE_MASK+1);
				
				//if (debug) Log.println("Index: "+i);
				Symmetry.clear(conf);
				Combinatorics.setConf(conf,i,wP,bP);
				//if (debug) Board.showBoard(conf);					
						  				    
				/*Symmetry.setMirrorX(conf,hulpConf);
				ind2 = LoaCombinatorics.getIndex(hulpConf,wP,bP);*/
				
				ind2 = Combinatorics.getIndexMirrorX(conf,wP,bP);
				
				if(i<=ind2){
				  /*Symmetry.setMirrorY(conf,hulpConf);
				  ind3 = LoaCombinatorics.getIndex(hulpConf,wP,bP);*/
					ind3 = Combinatorics.getIndexMirrorY(conf,wP,bP);
				}
				else
				 ind3=-1;
				
				if(i<=ind3){
					 /*Symmetry.setMirrorXY(conf,hulpConf);
				     ind4 = LoaCombinatorics.getIndex(hulpConf,wP,bP);*/
				     
				     ind4 = Combinatorics.getIndexMirrorXY(conf,wP,bP);
				}
				else
					 ind4=-1;
			    
			    //conf = EndGameDataBasePN.toBigByteBoard(conf, xSize,ySize );
				
				//if (debug) Log.println("Ind2: "+ind2+" Ind3: "+ind3+" Ind4: "+ind4);
				//if (debug) Log.println("Bitcounter: "+bitcounter);
				if ((i<=ind4))
				{
					tmp=i;
					
					/*if (!currentSegmentLoaded)
					{
						currentSegment = getSegment(i,wP,bP);
						segmentChanged=false;
						currentSegmentLoaded=true;
					}*/
					
					byte databaseResult= page.getInfo((int)(i&PAGE_MASK));   //(byte)((currentSegment>>>(bitcounter<<1))&(byte)3);
					
					if (databaseResult==0)
					{	
					   int value = doLookUp(round,conf);
						
					  /* if(value!=0){
						if (value >= Board.WIN_SCORE) {
							currentSegment|=(byte)((WIN<<(bitcounter<<1)));
						}else if (value <= -Board.WIN_SCORE){
							currentSegment|=(byte)((LOSS<<(bitcounter<<1)));
						} else if (value==DRAWVALUE){
							currentSegment|=(byte)((DRAW<<(bitcounter<<1)));
						} 
						
					   
					   segmentChanged=true;*/
					   
					   
					   if(value!=0){
						   
						   byte val;
							if (value >= Board.WIN_SCORE) {
								val = WIN;
							}else if (value <= -Board.WIN_SCORE){
								val=LOSS;
							} else {
								val=DRAW;
							}
							
							page.setInfo((int)(i&PAGE_MASK),val);
					   
						changed = true;
					   progresscounter++;
					}
					   else if(list.size()<LIST_SIZE) list.add(Long.valueOf(i));
					}
				}
								
				if( ( ((i!=0)&&(i&PAGE_MASK)==PAGE_MASK)) || i == length-1)///(bitcounter==3 || i == length-1)
				{					
					page.writeToDB();
					
					/*currentSegmentLoaded=false;
					if (segmentChanged)
					{	changed = true;
						egdb[wP-1][bP-1].seek(i/4);
						egdb[wP-1][bP-1].writeByte(currentSegment);
					}*/
				}
			}
			
			if(round==1){
				max[phase]=tmp+1;
				egdb[wP-1][bP-1].setLength((max[phase]/4)+1);}
				Log.print(max[phase] + " ");		
	}
	catch (IOException e) {e.printStackTrace(); throw new Error("This is not normal");}
	
	return !changed;
}

private boolean retroList(int round, int wP, int bP, int phase, AbstractList<Long> list){
	
	byte[][] conf  = new byte[Board.HEIGHT][Board.WIDTH];
	byte currentSegment;
	boolean changed = false;
	
	try
	{		Iterator<Long> iterator = list.iterator();
			
			while(iterator.hasNext())
			{	
				long i = (iterator.next()).longValue();
				final int bitcounter=(int)(i&3);
				Symmetry.clear(conf);
				Combinatorics.setConf(conf,i,wP,bP);
			    int value = doLookUp(round, conf);
						
			    if(value!=0){
						
			    	currentSegment = getSegment(i,wP,bP);
					
			    	if (value >= Board.WIN_SCORE) {
							currentSegment|=(byte)((WIN<<(bitcounter<<1)));
						}else if (value <= -Board.WIN_SCORE){
							currentSegment|=(byte)((LOSS<<(bitcounter<<1)));
						} else if (value==DRAWVALUE){
							currentSegment|=(byte)((DRAW<<(bitcounter<<1)));
						} 					   
					   
					   progresscounter++;
					   egdb[wP-1][bP-1].seek(i/4);
					   egdb[wP-1][bP-1].writeByte(currentSegment);
					   changed = true;
					   iterator.remove();
					
			    }
			}
			Log.print(max[phase] + " ");		
	}
	catch (IOException e) {e.printStackTrace(); throw new Error("This is not normal");}
	
	return !changed;
}


	private boolean retroStep(int round, int wP, int bP)
	{		
		boolean first, second;
		this.allPages[bP][wP].clear();
		this.allPages[wP][bP].clear();
		
		if(firstList == null || firstList.size()>=LIST_SIZE){
			firstList = new LinkedList<>();
			first= retroStep(round, wP, bP, 0, firstList);
		}
		else first= retroList(round, wP, bP, 0, firstList);
		
		this.allPages[bP][wP].clear();
		this.allPages[wP][bP].clear();
		
		if(wP != bP)
			if(secondList == null || secondList.size()>=LIST_SIZE){
				secondList = new LinkedList<>();
				this.allPages[wP][bP].clear();
				this.allPages[bP][wP].clear();
				second = retroStep(round, bP, wP, 1, secondList);
			}
			else second= retroList(round, bP, wP, 1, secondList);
		else
		 second = true;
		
		Log.println(progresscounter+"/"+(max[0]+max[1]));
	
		return first && second;
	}
	
	
	private byte getSegment(long index, int wP, int bP) throws IOException{
				
		egdb[wP-1][bP-1].seek(index/4);
		
		return egdb[wP-1][bP-1].readByte();
	}
	
	
	private byte getInfoFromDB(long index, LinkedList <DataBasePage> pages, final int wP, final int bP) throws IOException
	{
		
		if (loadDataBaseInMemory){
			byte result=egdbMemory[wP-1][bP-1][(int)(index>>2)];
			return (byte) ((result>>>((index&3)<<1))&3);}
		
		
		
		//if (debug) Log.println("Requesting Index: "+index);
		ListIterator<DataBasePage> i = pages.listIterator();
		int searchPageNumber=(int)(index>>>PAGE_SIZE);
		//if (debug) Log.println("PageNumber: "+searchPageNumber);
		while (i.hasNext())
		{
			DataBasePage p = i.next();
			if (p.getPageNumber()==searchPageNumber)
			{
				//if (debug) Log.println("Found, returning: "+p.getInfo((int)(index&65535)));
				i.remove();
				pages.addFirst(p);
				return p.getInfo((int)(index&PAGE_MASK));
			}
		}
		//if (debug) Log.println("Not found, creating new page");
		
		DataBasePage newP;
		if (pages.size()>=MAX_PAGES)
		{  newP = pages.getLast();
			newP.setDataBasePage(egdb[wP-1][bP-1],searchPageNumber,1<<(PAGE_SIZE));
			//((DataBasePage)(pages.getLast())).writeToDB();
			pages.removeLast();
			//if (debug) Log.println("Too many pages, removing one.");
		}
		else newP = new DataBasePage(egdb[wP-1][bP-1],searchPageNumber,1<<(PAGE_SIZE));
		//}catch(Exception e){System.out.println("test" + wP + " " +bP);newP=null;System.exit(0);};
		
		pages.addFirst(newP);
		
		return newP.getInfo((int)(index&PAGE_MASK));
	}
		
	private  final int doLookUp(int round, byte[][] conf) throws IOException{
		
		   	final int turn = 1;
		    b.setSpace(conf);
		    b.initialise(turn);
		    Move moveList=null;
			boolean firstGeneration = true;
			
			int value = (round>1)?0:gameAI.gameTheoreticEvaluation(b, -turn);
			
		    if(value==0){
			    							
		    	moveList = (round==1)?generator.generateMoves(b, turn):generator.generateNoCaptureMoves(b, turn);
			
		    	if(moveList==null)
		    		moveList = new Move();
			
		    	value= -Board.WIN_SCORE;
		    	}
		    else{
		    	if(turn*value >= Board.WIN_SCORE){
					value = Board.WIN_SCORE;
		    	}
				else
				if(turn*value <= -Board.WIN_SCORE){
					value = -Board.WIN_SCORE;
				}else{   
					value=DRAWVALUE;
				}
		    }
		    	
			for (Move m = moveList; m!=null; m = m.next)
			{
				m.apply(b);							
				//ended=Board.hasEnded(current);
				
				int score =  gameAI.gameTheoreticEvaluation(b, turn);
												
				if (score ==0)
				{   byte info=0;
												
					info = getInfo(b);
					
					if(info==LOSS){
						score = Board.WIN_SCORE;
					}
					else
						if(info==WIN){
							score = -Board.WIN_SCORE;
						}
						else if(info==DRAW )score=DRAWVALUE;									
					
				} //game-over
				else{ 
				   	if(turn*score>= Board.WIN_SCORE){
						score = Board.WIN_SCORE;
			    	}
					else
					if(turn*score<= -Board.WIN_SCORE){
						score = -Board.WIN_SCORE;
					}else{   
						score=DRAWVALUE;
					}
				}
				
				m.undo(b);
			
			if(score > value){
				value= score;
				if(value >= Board.WIN_SCORE)
					break;
			}
			
			if( m.next==null && value!=0 &&  round>1 && firstGeneration){
				
				firstGeneration = false;
				m.next = generator.generateCaptureMoves(b, turn);
				}								
			}
		
	 if(moveList!=null && moveList.next!=null)
		generator.recycle(moveList);
	 
		return value;	
	}
	
	@SuppressWarnings("unchecked")
	public byte getInfo(Board board) throws IOException {
				
		final int wP = board.W_Counter-1;
		final int bP = board.B_Counter-1;
		
		if (board.sideToMove == Board.B_SIDE)
		{	/*long currentIndex=LoaCombinatorics.getIndex(space,wP,bP);
		    Symmetry.setMirrorX(space,hConf);
			long ind2 = LoaCombinatorics.getIndex(hConf,wP,bP);
			if (ind2<currentIndex) currentIndex=ind2;
			Symmetry.setMirrorY(space,hConf);
			long ind3 = LoaCombinatorics.getIndex(hConf,wP,bP);
			if (ind3<currentIndex) currentIndex=ind3;
			Symmetry.setMirrorXY(space,hConf);
			long ind4 = LoaCombinatorics.getIndex(hConf,wP,bP);
			if (ind4<currentIndex) currentIndex=ind4;
			
			long info = getSegment(currentIndex,wP,bP);
			int tmpbitcounter= (int)(currentIndex&3L);
			return (byte)((info>>>(tmpbitcounter<<1))&3);*/
			
			
			
			if(egdb[wP][bP]==null  && egdbMemory[wP][bP]==null)
				return 0;
			
			long currentIndex=Combinatorics.getIndex(board.space,board.W_Counter,board.B_Counter);
						
			long ind2 = Combinatorics.getIndexMirrorX(board.space,board.W_Counter,board.B_Counter);
			if (ind2<currentIndex) currentIndex=ind2;
			
			long ind3 = Combinatorics.getIndexMirrorY(board.space,board.W_Counter,board.B_Counter);
			if (ind3<currentIndex) currentIndex=ind3;
			
			long ind4 = Combinatorics.getIndexMirrorXY(board.space,board.W_Counter,board.B_Counter);
			if (ind4<currentIndex) currentIndex=ind4;
			
			return getInfoFromDB(currentIndex, allPages[board.W_Counter][board.B_Counter],  board.W_Counter,board.B_Counter); 
		}
		else
		{	
			if(egdb[bP][wP]==null && 	egdbMemory[bP][wP]==null)
				return 0;
			
			Symmetry.setReverse(board.space, confR);
			long currentIndex=Combinatorics.getIndex(confR,board.B_Counter,board.W_Counter);
			
			/*Symmetry.setMirrorX(confR,hConf);
			long ind2 = LoaCombinatorics.getIndex(hConf,bP,wP);
			if (ind2<currentIndex) currentIndex=ind2;
			Symmetry.setMirrorY(confR,hConf);
			long ind3 = LoaCombinatorics.getIndex(hConf,bP,wP);
			if (ind3<currentIndex) currentIndex=ind3;
			Symmetry.setMirrorXY(confR,hConf);
			long ind4 = LoaCombinatorics.getIndex(hConf,bP,wP);
			if (ind4<currentIndex) currentIndex=ind4;*/
		
			long ind2 = Combinatorics.getIndexMirrorX(confR,board.B_Counter,board.W_Counter);
			if (ind2<currentIndex) currentIndex=ind2;
			
			long ind3 = Combinatorics.getIndexMirrorY(confR,board.B_Counter,board.W_Counter);
			if (ind3<currentIndex) currentIndex=ind3;
			
			long ind4 = Combinatorics.getIndexMirrorXY(confR,board.B_Counter,board.W_Counter);
			if (ind4<currentIndex) currentIndex=ind4;
			
			return getInfoFromDB(currentIndex, allPages[board.B_Counter][board.W_Counter],  board.B_Counter,board.W_Counter); 
		
		}		
	}	
	
	private  void openDatabase(){
	
		egdb=new RandomAccessFile[10][10];
		egdbMemory=new byte[10][10][];
				
		String home = load+ this.gameAI.getGameName() + File.separator;
		String[] fileNames = new File(home).list(); 
		
		if(fileNames==null){
			System.out.println("No Databases available");
			return;
		}
			
		
		for (int i=fileNames.length;--i>=0;){
		
			StringTokenizer st = new StringTokenizer(fileNames[i]," ");
			st.nextToken();
			
			int size = Integer.parseInt(new StringTokenizer(st.nextToken(),"x").nextToken());
			
			//System.out.println(size);
			
			if(size!=gameAI.getSize())
					continue;
			
			//System.out.println("test");
			StringTokenizer st2 =  new StringTokenizer(st.nextToken(),"-");
			
			int l = Integer.parseInt(st2.nextToken())-1;
			int r = Integer.parseInt(st2.nextToken())-1;
			
			//System.out.println(l + " " + r);
					
		    try{ 
		       egdb[l][r]= new RandomAccessFile(home + fileNames[i],"rw");
		    
		  		if (loadDataBaseInMemory){			
						//System.out.println(egdb[i][j].length());
						egdbMemory[l][r] = new byte[(int)egdb[l][r].length()];
						egdb[l][r].read(egdbMemory[l][r]);
						//System.out.println(i+"-"+j+" Size: "+egdbMemory[i][j].length);
					}
			
			} catch (IOException e) {
				egdb[l][r]= null;
				egdbMemory[l][r] = null;
				e.printStackTrace();
			}	
		}
	}
	
	public  void closeDatabase() 
	{
		try
		 {for (int i= egdb.length; --i>=0;)
			for (int j= egdb[i].length; --j>=0;)
			if(egdb[i][j]!=null){
				egdb[i][j].close();
				egdb[i][j]=null;
			}
		 }
		 catch(IOException e){
			 
			 e.printStackTrace();
		 }
	}
	

			
		
		

	/*private  void closeDatabase() throws Exception
	{
		for (int i=1;i<whitePieces+blackPieces-1;i++)
			for (int j=1; j<whitePieces+blackPieces-1;j++)
				egdb[i][j].close();
	}*/
	

	private void setDraws(int wP, int bP) throws IOException
	{
		/*egdb[wP-1][bP-1].seek(0);
		final long length = egdb[wP-1][bP-1].length();
		for (long i=0;i<length;i++)
		{
			byte segment = egdb[wP-1][bP-1].readByte();
			
			if ((segment&3)==0) segment=(byte)(segment|DRAW);
			if (((segment>>>2)&3)==0) segment=(byte)(segment|(DRAW<<2));
			if (((segment>>>4)&3)==0) segment=(byte)(segment|(DRAW<<4));
			if (((segment>>>6)&3)==0) segment=(byte)(segment|(DRAW<<6));
			
			egdb[wP-1][bP-1].seek(i);
			egdb[wP-1][bP-1].writeByte(segment);
		}*/
		
		byte[] buffer = new byte[1024];
		long counter =0;
		int length;
		egdb[wP-1][bP-1].seek(0);
		
		while((length = egdb[wP-1][bP-1].read(buffer))!=-1){
				
		for (int i=0;i<length;i++)
		{
			byte segment = buffer[i];
			
			if ((segment&3)==0) segment=(byte)(segment|DRAW);
			if (((segment>>>2)&3)==0) segment=(byte)(segment|(DRAW<<2));
			if (((segment>>>4)&3)==0) segment=(byte)(segment|(DRAW<<4));
			if (((segment>>>6)&3)==0) segment=(byte)(segment|(DRAW<<6));
			buffer[i] =segment;
		}
		
		egdb[wP-1][bP-1].seek(counter);
		egdb[wP-1][bP-1].write(buffer,0,length);
		counter+=length;	
		}
	}
	
	private void assignDraws(int wP, int bP) throws IOException
	{
		setDraws(wP, bP);
		
		if(wP!=bP)
			setDraws(bP, wP);
	}
	
	protected void finalize() throws Throwable
	{
		
		this.closeDatabase();
	}
				
	}

