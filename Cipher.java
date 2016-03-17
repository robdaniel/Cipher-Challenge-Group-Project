import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Cipher {
	static int[][] sbox = new int[16][16];
	static int[][] sboxInv = new int[16][16];
	
	// Scrambles the sbox columns
	// Circular shift each column to the right by it's row index + shiftAmount
	public static int[][] shiftColumns(int[][] input, int shiftAmount) {
		
		int[][] tbox = new int[16][16];
		
		for (int r = 0; r < input.length; r++) {
			for (int c = 0; c < input.length; c++) {
				tbox[r][c] = input[r][(c + r + shiftAmount) % input.length];
			}
		}
		
		return tbox;
	}
	
	// Scrambles the sbox rows
	// Circular shift each row down by it's column index + shiftAmount
	public static int[][] shiftRows(int[][] input, int shiftAmount) {
		int[][] tbox = new int[16][16];
		
		for (int r = 0; r < input.length; r++) {
			for (int c = 0; c < input.length; c++) {
				tbox[r][c] = input[(r + c + shiftAmount) % input.length][c];
			}
		}
		
		return tbox;
	}
	
	// Calculates the sbox and scrambles it using shiftRows/shiftColumns
	public static void calculateSbox() {
		int[][] tbox = new int[16][16];
		int counter = 0;
		
		for (int r=0; r<tbox.length; r++) {
			for (int c=0; c< tbox.length; c++) {
				tbox[r][c] = counter;
				counter++;
			}
		}
		
		sbox = shiftColumns(shiftRows(shiftColumns(tbox, 5), 11), 5);
	}
	
	// Calculates the inverted sbox for decryption
	public static void calculateSboxInv() {
		
		for (int r=0; r<sboxInv.length; r++) {
			for (int c=0; c< sboxInv.length; c++) {
				String hex = Integer.toHexString(sbox[r][c]);
				hex = hex.length() == 1 ? "0" + hex : hex;
				
				int row = Integer.parseInt("0"+hex.charAt(0), 16);
				int col = Integer.parseInt("0"+hex.charAt(1), 16);
				int val = Integer.parseInt(Integer.toHexString(r)+Integer.toHexString(c), 16);
				sboxInv[row][col] = val;
			}
		}
	}
	
	// XOR's two binary strings
	public static String XOR(String kString, String tString){
		
		StringBuilder rstring = new StringBuilder();
		String temp = Long.toBinaryString(Long.parseLong(kString,2) ^ Long.parseLong(tString,2));
		
		for(int i=0; i< kString.length()-temp.length(); i++)
			rstring.append(0);
		
		rstring.append(temp);
			
		return rstring.toString();
	}
	
	// XOR's the even bytes of the input with 0xFF
	public static String subtractBytes(String binString) {
        
		String xorString = "11111111";
		String result = "";
		
		for (int i = 0; i < binString.length(); i += 8) {
			String subString = binString.substring(i, i + 8);
			
			if ((i / 8) % 2 == 0) {
				subString = XOR(subString, xorString);
			}
			
			result += subString;
		}
		
		return result;
    }
	
	// Substitutes each byte of the binary string with the values in the given sbox
	// first hex digit is the row index, second digit is the column index
	public static String substituteBytes(String binString, int[][] sbox) {

		String result = "";
		
		for (int i = 0; i < binString.length(); i += 8) {
			String rowString = binString.substring(i, i + 4);
			String columnString = binString.substring(i + 4, i + 8);

			// Prepend 0's to the binary string
			while (rowString.length() < 4) rowString = "0" + rowString;
			while (columnString.length() < 4) columnString = "0" + columnString;
			
			if ((i / 8) % 2 == 1) {
				int row = Integer.parseInt(rowString, 2);
				int column = Integer.parseInt(columnString, 2);
				
				String sub = Integer.toBinaryString(sbox[row][column]);
				while (sub.length() < 8) sub = "0" + sub;
				
				result += sub;
			}
			else
				result += rowString + columnString;
		}
		
		return result;
	}
	
	// Encrypts the given plaintext using the passed key
	public static String burgundy_Encrypt(String plaintext, String key) {
		String result;
		
		// Prepend 0's to the binary strings
		while (key.length() < 48) key = "0" + key;
		while (plaintext.length() < 48) plaintext = "0" + plaintext;
		
		result = XOR(substituteBytes(subtractBytes(plaintext), sbox), key);
		
		return result;
	}

	// Decrypts the given ciphertext with the passed key
	public static String burgundy_Decrypt(String ciphertext, String key) {
		String result;

		// Prepend 0's to the binary strings
		while (key.length() < 48) key = "0" + key;
		while (ciphertext.length() < 48) ciphertext = "0" + ciphertext;
		
		result = subtractBytes(substituteBytes(XOR(ciphertext, key), sboxInv));
		
		return result;
	}
	
	public static void main(String[] args) throws Exception{

		Scanner stdin = new Scanner(System.in);
		
		// Get the necessary user information...
		System.out.println("Enter 1 to encrypt the file, or 2 to decrypt the file.");
		int choice = stdin.nextInt();
		
		System.out.println("Enter the input file.");
		String infilename = stdin.next();
		
		System.out.println("Enter the output file.");
		String outfilename = stdin.next();
		
		System.out.println("Enter the key in hex.");
		String key = stdin.next();

		key += key;
		
		Scanner fin = new Scanner(new File(infilename));
		FileWriter fout = new FileWriter(new File(outfilename));

		Radix64 translate = new Radix64();

		calculateSbox();
		calculateSboxInv();
		
		//encrypt
		if(choice == 1)
		{
			String ptext = "";
			
			while(fin.hasNext()) {
				ptext = fin.next();
	
				String keyBinString = Long.toBinaryString(Long.decode("0x" + key));
				String ptextBinString = translate.toBinary(ptext);
				String line = burgundy_Encrypt(ptextBinString, keyBinString);

				line = translate.toChars(line);
				fout.write(line);
				fout.write("\n");
			}
		}
		//decrypt
		else if(choice == 2)
		{
			String ctext = "";

			while(fin.hasNext())
			{
				ctext = fin.nextLine();

				String keyBinString = Long.toBinaryString(Long.decode("0x" + key));
				String ctextBinString = translate.toBinary(ctext);
				String line = burgundy_Decrypt(ctextBinString, keyBinString);

				line = translate.toChars(line);
				fout.write(line);
				fout.write("\n");
			}
		}
		else
			System.out.println("Invalid action selected");

		fout.close();
		fin.close();
	}

}
