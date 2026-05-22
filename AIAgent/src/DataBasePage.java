import java.io.IOException;
import java.io.RandomAccessFile;


public final class DataBasePage 
{
	private RandomAccessFile database;
	private int pageNumber;
	private byte[] data;
	private int pageSize;
	private int realSize;
	
	
	public DataBasePage(RandomAccessFile db, int pN, int pS) throws IOException
	{		
		setDataBasePage(db,pN,pS);
	}
	
	public void setDataBasePage(RandomAccessFile db, int pN, int pS) throws IOException
	{
		database = db;
		pageNumber = pN;
		pageSize = pS;
		
		if(data==null)
			data = new byte[pageSize>>2];
		
		database.seek(((long)pN)*(pS>>2));
		realSize=database.read(data);
	}
	
	public int getPageNumber()
	{
		return pageNumber;
	}
	
	public byte getInfo(int index){
	
		return (byte)((data[index>>2]>>>((index&3)<<1))&3);
	}
	
	public void writeToDB() throws IOException
	{
		database.seek(((long)pageNumber)*(pageSize>>2));
		database.write(data,0,realSize);
	}
	
	public void setInfo(int position, byte info)
	{
		//Log.println("Before: "+);
		data[position>>2]|=(info<<((position&3)<<1));
	}
}
