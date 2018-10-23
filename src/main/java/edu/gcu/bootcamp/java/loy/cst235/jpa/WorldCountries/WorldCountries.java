package edu.gcu.bootcamp.java.loy.cst235.jpa.WorldCountries;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class WorldCountries {
	
	// Needed for user input
	static Scanner sc = new Scanner(System.in);
	
	// Needed for the file import
	static String csvSplitBy = ",";
	static String csvLine = "";
	static String csvFile = "country.csv";
	// Needed for the encryption of the String
	static String UniqueId  = "JavaIsFun"; 
	private static SecretKeySpec secretKey;
	private static byte[] key;
	// Needed for the DB connection
	private static Connection con;
	
	// Lets start here
	public static void main(String[] args) {
		getConnection();  	// Establish the connection
		setKey(UniqueId);	// Set the encryption key
		menu();				// Show a menu to the user
	}

	// Display a menu
	private static void menu() {
		boolean again = true;
		// Loop the menu until they press anything number other than 1,2, or 3
		do {
			// Display message if the user does not enter an int
			try{

				System.out.println("1. Run World Countries insert");
				System.out.println("2. Insert user/password");
				System.out.println("3. Check user/password");
				System.out.println("Any other number to exit");
				int x = sc.nextInt();
				// Added nextLine() to capture nextInt line return glitch
				sc.nextLine();
				again = actionMenu(x);
			}
			catch (Exception e)
			{
				System.out.println("You need to enter a number!");
				System.out.println("Press Enter to continue.");				
				sc.nextLine();
			}
			// keep looping until again = false
		} while (again);
	}
	
	// Using the option selected to branch to different functionality
	private static boolean actionMenu(int opt) {
		switch (opt){
		case 1:
			System.out.println("Run World Countries insert");
			runCountryInsert();
			return true;
		case 2:
			System.out.println("Insert user/password");	
			runInsertUser();
			return true;
		case 3:
			System.out.println("Check user/password");	
			isValiduser(runCheckUser());
			return true;
		default:
			System.out.println("Exiting");				
			return false;
		}
	}
	
	// Using a boolean from the runCheckuser()
	static void isValiduser(boolean test) {
		if (test) {
			System.out.println("Congratulation! You are real!");
		}
		else {
			System.out.println("Imposter! Try again!");			
		}
	}
	
	// Insert the user into the DB
	static void runInsertUser() {
		String user = getUserInput("Enter username to Insert: ");
		String pass = encrypt(getUserInput("Enter password to Insert: "),UniqueId);	
		// Using a preparedStatement so use the ? place holders
		String sql = "INSERT INTO userauthentication (auth_username, auth_password)"
				+  "VALUES (?,?)";
		
		try {
			PreparedStatement stmt = con.prepareStatement(sql);
			// change the ? place holders into the actual values
			stmt.setString(1, user);
			stmt.setString(2, pass);
			// Execute the SQL with the real data replacing the ? place holders			
			stmt.executeUpdate();
		}
		catch(Exception e) {
			System.out.println(e);
		}		
	}
	
	// check if the user exists in the DB, return a boolean
	static boolean runCheckUser() {
		ResultSet set;
		int id = 0;
		String user = getUserInput("Enter username to Check: ");
		// Encrypt the password entered
		String pass = encrypt(getUserInput("Enter password to Check: "),UniqueId);
		// Using a preparedStatement so use the ? place holders		
		String sql = "SELECT AUTH_ID FROM userauthentication WHERE (auth_username = ?) AND (auth_password = ?)";
		
		try {
			PreparedStatement stmt = con.prepareStatement(sql);
			// change the ? place holders into the actual values			
			stmt.setString(1, user);
			stmt.setString(2, pass);
			// Execute the SQL with the real data replacing the ? place holders			
			set = stmt.executeQuery();
			// Check to see if at least ONE item came back, if one did, then user is valid
			if (set.next() == true) {
				return true;
			}
			else
			{
				return false;
			}
		}
		catch(Exception e) {
			System.out.println(e);	
		}	
		return false;
	}
	
	// generic user input method
	static String getUserInput(String message) {
		System.out.println(message);
		return sc.nextLine();
	}
	
	// Establish the connection to the DB and store in the static class con property
	public static void getConnection() {
		try {
			con=DriverManager.getConnection("jdbc:oracle:thin:@oracle-instance1.ceqiop0skezv.us-east-2.rds.amazonaws.com:1521:ORCL","gcukymbrlee","Stinker1");
			Class.forName("oracle.jdbc.driver.OracleDriver");
		}
		catch(Exception e) {
			System.out.println(e);
		}
	}

	// Do the Country read from file and insert into dB
	private static void runCountryInsert()  {

		try(BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			// Read in each line from the file
			while((csvLine=br.readLine())!= null) {
				// Split each line using the comma and put elements into an array
				String[] country = csvLine.split(csvSplitBy);
				// Using a preparedStatement so use the ? place holders		
				String sql = "insert into worldcountries " + 
						"(country_name,country_population, country_area, country_gdp, country_literacy_rate)" + 
						"values(?,?,?,?,?)";
				PreparedStatement prep = con.prepareStatement(sql);	
				// change the ? place holders into the actual values from the split line array
				prep.setString(1, country[0]);
				prep.setInt(2, Integer.parseInt(country[1]));
				prep.setInt(3, Integer.parseInt(country[2]));
				prep.setInt(4, Integer.parseInt(country[3]));
				prep.setDouble(5, Double.parseDouble(country[4]));	
				// Execute the SQL with the real data replacing the ? place holders
				prep.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		catch (FileNotFoundException f) {
		}
		catch (IOException g) {
		}		
	}
	
	// Create the key for the encryption piece
	public static void setKey(String myKey) {
		MessageDigest sha = null;
		try {
			key = myKey.getBytes("UTF-8");
			sha = MessageDigest.getInstance("SHA-256");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, "AES");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	// Encrypt the string passed in
	public static String encrypt(String strToEncrypt, String secret) {
		
		try {
			setKey(secret);
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		}
		catch(Exception e) {
			System.out.println("Error while encrypting: "+ e.toString());
		}
		return null;
	}		
}