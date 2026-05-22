
public class SurakartaEvaluator extends SurakartaAbstractEvaluator{
	
	
	public SurakartaEvaluator() {

		weight = new double[]
		
		{3,9,9,9,9,3,
		 10,10,10,10,10,10,
		 10,10,10,10,10,10,
		 10,10,10,10,10,10,
		 10,10,10,10,10,10,
		 3, 9,9,9,9,3,
		};
		
	     feature = new double[weight.length];
	}

	public int evaluate(Board b, int lastturn) {
		
		if(noHash){
			
			for(int i = feature.length;--i>=0;)
				feature[i]=0;
			
		}
		
		double value =0;
		
		for(int i = b.B_Counter; --i>=0;){
			Piece p = b.arrayB[i];
			feature[p.index]=10;
			value += 10*weight[p.index];			
		}
		
		
		for(int i = b.W_Counter; --i>=0;){
			Piece p = b.arrayW[i];
			feature[p.index]=-10;
			value -= 10*weight[p.index];			
		}
		
		return ((int) value + randomIntValue());
	}
	


}
