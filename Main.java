package art;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class Main {

	private static int length;
	private static int lengthOfOutput;
	private static FileInputStream file;
	static ArrayList<Symbol> symbols;
	static double[] cumulativeProbabilities;
	static double upperBound = 1;
	static double lowerBound = 0;
	enum SubsetPosition { UP, DOWN, MIDDLE, ELSE};
	static double[] currentPartition;
	static boolean endOfMessage;
	private static FileOutputStream fileOut;
	private static String code;

	public static void main(String[] args) throws InterruptedException, IOException {
		
		endOfMessage = false;
		
		if(args[1].equals("kompresja")){
				
				length = 0;
				lengthOfOutput = 0;
				file = new FileInputStream(args[0]); //otwieramy plik do kompresji
				fileOut = new FileOutputStream("E:\\Studia\\Kodowanie i kompresja danych\\Lab\\Lab4\\out");
				System.out.println("Plik: "+args[0]);
				
				symbols = new ArrayList<>();
				int key;
				
				while( (key = file.read())!=-1){ //tworzymy liste symboli kodowych
					
					length++;
					int position = containsSymbol(key);
					
					if (position!=-1){
						
						symbols.get(position).amount ++;
						
					} else {
						symbols.add(new Symbol(key));
					}
				}
				
				//liczenie entropii
				double entropy = 0;
				
				for (int i = 0; i < symbols.size(); i++){
					
					double prob = ((double)symbols.get(i).amount)/length;
					entropy += prob*Math.log(prob);
					
				}
				
				entropy /=-Math.log(2);
				
				symbols.add(new Symbol(256)); // dodawanie symbolu specjalnego
				length++;
				
				code = "";
				int counter = 0;
				
				cumputeProbabilities(); //obliczanie prawdopodobieństw symboli
				computeCumulativeProbabilities(); //obliczanie skumulowanych prawdopodobieństw
				
				//zapisujemy słownik do pliku
				
				int symbolsAmount = symbols.size()-1;   //nie liczymy symbolu specjalnego
				System.out.println("Ilosc symboli w slowniku: " + symbolsAmount);
				
				fileOut.write(symbolsAmount-1);
				
				
				for(int i = 0 ; i < symbolsAmount; i++){
					fileOut.write(symbols.get(i).symbol);
					byte[] bytes = toByteArray(cumulativeProbabilities[i+1]);
					fileOut.write(bytes);
					//System.out.println(symbols.get(i).symbol + ", " + cumulativeProbabilities[i+1]);
				}
								
				//koniec zapisu słownika do pliku
				length--;
				
				System.out.println("Długość pliku wynosi " + length + " bajtów.");
				file.close();
				
				file = new FileInputStream(args[0]); 
				
				FileChannel channel = file.getChannel();
				long fileSize = channel.size();
				
				
				while( fileSize > -1){ //czytamy symbol z pliku, na koncu dodajemy symbol specjalny
					
					fileSize--;
					
					if (fileSize!=-1){
						key = file.read();
					} else {
						System.out.println("Dodaje znacznik konca");
						key = 256; //symbol specjalny, konczacy komunikat
					}
							
					double F_lower = cumulativeProbabilities[containsSymbol(key)];
					double F_upper = cumulativeProbabilities[containsSymbol(key)+1];
					
					double prevLowerBound = lowerBound;
					lowerBound = lowerBound +(upperBound - lowerBound)*F_lower;
					upperBound = prevLowerBound + (upperBound - prevLowerBound)*F_upper; //obliczanie biezacego przedzialu
					
					boolean loop = true;
					
					while (loop){
						SubsetPosition pos = subPos(lowerBound, upperBound);
						
						if(pos == SubsetPosition.DOWN){
							
							code += "0";
							for(int i = 0; i<counter;i++) code +="1";
							counter = 0;
							
						} else if (pos == SubsetPosition.UP){
							
							code += "1";
							
							for(int i = 0; i<counter;i++) code +="0";
							
							lowerBound = lowerBound-0.5; 
							upperBound = upperBound-0.5; 
							
							counter = 0;
							
						} else if (pos == SubsetPosition.MIDDLE){
							
							counter++;
							lowerBound = lowerBound-0.25; 
							upperBound = upperBound-0.25;
							
						} else{
							
							break;
						}
						
						lowerBound = lowerBound*2; 
						upperBound = upperBound*2; 				
					}
					
					while(code.length() >=8 ){
						
						lengthOfOutput++;

						String toWrite = code.substring(0, 8); // wycinamy jeden bajt
						code = code.substring(8); //usuwamy wyciety bajt
						
						int x = StringToInt(toWrite); //zamiana stringa na int
						fileOut.write(x); //zapis bajtu do pliku

					}
				}
				
				counter++;
				if(lowerBound < 0.25) {
					code += "0";
					for(int i = 0; i<counter;i++) code +="1";
					counter = 0;			
				}
				else {
					code += "1";
					
					for(int i = 0; i<counter;i++) code +="0";
					
				}
				
				while(code.length() >=8 ){ //to na wypadek gdyby tych jedynek lub zero było całkiem sporo

					String toWrite = code.substring(0, 8); // wycinamy jeden bajt
					code = code.substring(8); //usuwamy wyciety bajt
					
					int x = StringToInt(toWrite); //zamiana stringa na int
					fileOut.write(x); //zapis bajtu do pliku

				}
				
				int howManyZerosToAttach = 8 - code.length();
				
				for (int i = 0; i < howManyZerosToAttach; i++) code += "0";
				
				//zapisujemy przedostatni bajt
					
				int x = StringToInt(code); //zamiana stringa na int
				fileOut.write(x); //zapis bajtu do pliku
				
				fileOut.write(howManyZerosToAttach);
				
				lengthOfOutput+=2;
				
				System.out.println("Entropia kodowanych danych: " + entropy);
				
				double wspKomp = (double)(length - lengthOfOutput)/length;
				System.out.println("Współczynnik kompresji: "+ wspKomp );
				double av = (double) (lengthOfOutput*8)/length;
				System.out.println("Średnia długość kodowania: " + av + " bitow" );
						
				file.close();
				fileOut.close();
				
/////////////********************************************************************/////////////////////////////////////
			
		} else if (args[1].equals("dekompresja")){
			
			file = new FileInputStream("E:\\Studia\\Kodowanie i kompresja danych\\Lab\\Lab4\\out");
			fileOut = new FileOutputStream("E:\\Studia\\Kodowanie i kompresja danych\\Lab\\Lab4\\decoded");
			
			FileChannel channel = file.getChannel();
			long fileSize = channel.size();
			
			int symbolsAmount = file.read();
			symbolsAmount++; // to na wypadek gdyby ktoś sobie zażartował i zapisał 256 symboli w pliku
			fileSize--;
			
			cumulativeProbabilities = new double[symbolsAmount+2]; 
			cumulativeProbabilities[0] = 0;
			
			symbols = new ArrayList<>();
			
			//wczytywanie słownika i dystrubuanty
			for (int i = 0; i < symbolsAmount; i++){
				
				int symbol = file.read(); fileSize--;
				
				symbols.add(new Symbol(symbol));
				
				byte[] bytes = new byte[8];
				file.read(bytes); fileSize -= 8;
				
				double prob = toDouble(bytes);
				
				cumulativeProbabilities[i+1] = prob;
			}
			
			symbols.add(new Symbol(256)); //symbol specjalny
			
			cumulativeProbabilities[symbolsAmount+1] = 1;
			
			code ="";
			String bufor ="";
			double roz;
			int rozBufora = 0;
			
			upperBound = 1;
			lowerBound = 0;
			
			currentPartition = new double[cumulativeProbabilities.length];
			
			boolean loop = true;
			while (loop){
			
				roz = 1;
				
				for(int i=0; i < cumulativeProbabilities.length-1; i++){
					
					double F_lower = cumulativeProbabilities[i];
					double F_upper = cumulativeProbabilities[i+1];
					
					currentPartition[i] = lowerBound +(upperBound - lowerBound)*F_lower;
					currentPartition[i+1] = lowerBound + (upperBound - lowerBound)*F_upper;
					
					if( currentPartition[i+1]-currentPartition[i] < roz) roz = currentPartition[i+1]-currentPartition[i];
					
				}
				
				rozBufora = (int) Math.ceil(  Math.log(1/(0.25*roz))/Math.log(2)  );
				
				while(code.length() < rozBufora*2 && fileSize > 0){
					
					if(fileSize == 2){
						
						int x = file.read();
						String toWrite = intToString(x);
						int zerosAmount = file.read(); fileSize = 0;
						toWrite = toWrite.substring(0, 8 - zerosAmount);
						code += toWrite;									
						
					} else {
						
						int x = file.read(); fileSize--;
						code += intToString(x);
					}
					
				}
				
				while (bufor.length() < rozBufora*2 && code.length()>0){
					bufor += code.charAt(0); //dodajemy "bity" do bufora dopóki nie osągnie wymaganego rozmiaru
					code = code.substring(1);
				}
				
				double buforBitow = toNumber(bufor);
				
				for(int i = 1; i < currentPartition.length; i++){
					
					if(buforBitow < currentPartition[i] && buforBitow >= currentPartition[i-1] ){
						//sprawdzamy do jakiego przedziału wpada aktualny znacznik
						lowerBound = currentPartition[i-1];
						upperBound = currentPartition[i];
						
						if(symbols.get(i-1).symbol == 256){
							endOfMessage = true;
						} else {
							fileOut.write(symbols.get(i-1).symbol); //zapisuje odpowiedni symbol
						}
						
						i = currentPartition.length;
					}
				}
				
				if(endOfMessage) break;

				while(true){
					
					SubsetPosition pos = subPos(lowerBound, upperBound);
					
					if(pos == SubsetPosition.DOWN){
						
						//nic nie rób;
						
					} else if (pos == SubsetPosition.UP){
						
						char [] pom = bufor.toCharArray();
						pom[0] = '0'; //odejmujemy 0,5 od liczby bufor
						bufor = String.valueOf(pom);
						
						lowerBound -= 0.5;
						upperBound -= 0.5;
						
					} else if (pos == SubsetPosition.MIDDLE){
						
						char [] pom = bufor.toCharArray();
						if( pom[1] == '1') pom[1]='0'; //odejmujemy 0.25 od bufora
						else { 
							pom[0]='0'; pom[1] = '1';
						}
						
						bufor = String.valueOf(pom);
						
						lowerBound -= 0.25;
						upperBound -= 0.25;
						
					} else { break; }
					
					lowerBound = lowerBound*2; 
					upperBound = upperBound*2;	
					bufor = bufor.substring(1);
					
					if (code.length()>0){
						
						bufor += code.charAt(0);
						code = code.substring(1);
						
					} else {
						
						if(fileSize > 0){
						
							if(fileSize == 2){
								
								int x = file.read();
								String toWrite = intToString(x);
								int zerosAmount = file.read(); fileSize = 0;
								toWrite = toWrite.substring(0, 8 - zerosAmount);
								code += toWrite;									
								
							} else {
								
								int x = file.read(); fileSize--;
								code += intToString(x);
							}						
								bufor += code.charAt(0);
								code = code.substring(1);					
						}		
					}					
				}				
				if(bufor.length()==0) loop = false;
			}
			
			fileOut.close();
			file.close();	
			
		}
	}
	
	private static SubsetPosition subPos(double lB, double uB){
		
		if (lB >=0 && uB <=0.5) return SubsetPosition.DOWN;
		if (lB >= 0.5 && uB<=1) return SubsetPosition.UP;
		if (lB >= 0.25 && uB <= 0.75) return SubsetPosition.MIDDLE;
		return SubsetPosition.ELSE;	
		
	}
	
	private static double toNumber(String bufor){
		
		double res=0;
		
		for(int i=0; i<bufor.length(); i++){
			if(bufor.charAt(i) == '1') res += 1/(Math.pow(2, i+1));
		}
		
		return res;
		
	}
	
	//zwraca pozycję symbolu
	private static int containsSymbol(int symbol){
		
		for(int i = 0; i < symbols.size(); i++){
			if (symbols.get(i).symbol == symbol) return i;
		}
		
		return -1;
	}
	
	private static void cumputeProbabilities(){
		
		for (int i = 0; i < symbols.size(); i++){
			symbols.get(i).prob = (double) symbols.get(i).amount/length;
		}		
	}
	
	//obliczanie dystrybuanty rozkładu symboli
	private static void computeCumulativeProbabilities(){
		
		cumulativeProbabilities = new double[symbols.size()+1];
		cumulativeProbabilities[0] = 0;
		
		for(int i = 1; i < symbols.size()+1; i++) cumulativeProbabilities[i] = symbols.get(i-1).prob + cumulativeProbabilities[i-1];	
		
	}
	
	public static byte[] toByteArray(double value) {
	    byte[] bytes = new byte[8];
	    ByteBuffer.wrap(bytes).putDouble(value);
	    return bytes;
	}

	public static double toDouble(byte[] bytes) {
	    return ByteBuffer.wrap(bytes).getDouble();
	}
	
	public static int StringToInt(String string){
		
		int res=0;
		
		for (int i=7; i >=0; i--){
			
			if(string.charAt(7-i)=='1'){
				res += Math.pow(2, i);
			}
		}
		
		return res;
		
	}
	
	public static String intToString(int number){
		
		String res="";
		while(number >0 ){
			if(number%2==1) res = "1" + res;
			else res = "0"+res;
			number /=2;
		}
			
	    while(res.length()<8) res = "0" + res;
	    
		return res;
	}

}
