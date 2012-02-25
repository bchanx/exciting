import java.util.*;
import java.io.*;

public class Exciting {

	public static void main (String[] args) {
		System.out.println("Exciting script to find mailing addresses!");
		if(args.length != 1) {
			System.out.println("Please supply input file!");
			System.exit(-1);
		}

		try {
			FileInputStream f = new FileInputStream(args[0]);
			DataInputStream input = new DataInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			String str;
			StringBuffer s = new StringBuffer();
			while ((str = br.readLine()) != null) {
				s.append(str);
				s.append(" ");
			}
			input.close();

			AddrHelper a = new AddrHelper();
			ArrayList<String> addresses = a.findMailing(s.toString());

			for (int i=0; i<addresses.size(); ++i) {
				System.out.println(" [ADDRESS FOUND]: "+addresses.get(i));
			}

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Exception thrown!");
		}
	}

}

class AddrHelper {

	private HashMap<String, Integer> addr_types = new HashMap<String, Integer>();
	private HashMap<String, Integer> cities = new HashMap<String, Integer>();
	private HashMap<String, Integer> states = new HashMap<String, Integer>();
	private HashMap<String, Integer> dirs = new HashMap<String, Integer>();
	private ArrayList<String> potential = new ArrayList<String>();

	private static final int START_CONFIDENCE = 30;
	private static final int THRESHOLD = 80;
	private static final int NAME_BUFFER = 2;

	public AddrHelper() {
		addr_types.put("drive", 30);
		addr_types.put("dr", 30);
		addr_types.put("street", 30);
		addr_types.put("st", 30);
		addr_types.put("avenue", 30);
		addr_types.put("ave", 30);
		addr_types.put("hills", 30);

		cities.put("chicago", 20);
		cities.put("new", 10);
		cities.put("york", 10);
		cities.put("san", 10);
		cities.put("francisco", 10);
		cities.put("austin", 20);
		cities.put("phoenix", 20);

		states.put("az", 20);
		states.put("ca", 20);
		states.put("il", 20);
		states.put("ny", 20);
		states.put("tx", 20);

		dirs.put("n", 10);
		dirs.put("s", 10);
		dirs.put("e", 10);
		dirs.put("w", 10);
		dirs.put("north", 10);
		dirs.put("south", 10);
		dirs.put("east", 10);
		dirs.put("west", 10);
	}

	// (TODO) identify the split name cities to store as single key (cases like San Francisco or New York)
	private String sanitize (String str) {
		StringBuffer s = new StringBuffer();
		String[] tokens = str.split(" ");
		String badChars = "[\\?!,\\.#\\(_\\)-]";
		for (int i=0; i<tokens.length; ++i) {
			// ignore empty strings
			String ss = tokens[i].trim();
			if (ss.compareTo("") == 0) {
				continue;
			}
			s.append(ss.replaceAll(badChars, "").toLowerCase());
			s.append(" ");
		}
		return s.toString();
	}

	// check if str is an int
	private boolean isInt (String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// check if str is a zip code
	private boolean isZip (String str) {
		try {
			Integer.parseInt(str);
			if (str.length() == 5) {
				return true;
			}
		} catch (Exception e) {}
		return false;
	}

	// main function
	public ArrayList<String> findMailing(String str) {
		String s = sanitize(str);

		String[] tokens = s.split(" ");
		StringBuffer result = new StringBuffer();
		int confidence = 0;
		int name_buffer = 0;
		boolean found_int = false;
		boolean found_addr = false;
		boolean found_city = false;
		boolean found_state = false;
		boolean found_zip = false;

		for (int i=0; i<tokens.length; ++i) {

			// find the next int that's immediately followed by a string
			if (isInt(tokens[i]) && confidence <= START_CONFIDENCE) {
				found_int = true;
				confidence = START_CONFIDENCE;
				name_buffer = 0;
				//System.out.println("STARTING INT: "+tokens[i]);
				result = new StringBuffer();
				result.append(tokens[i]+" ");
				continue;
			}
			
			// start testing for mailing addr
			if (found_int) {
				//System.out.println("CURRENT CONFIDENCE: "+confidence+"  current token: "+tokens[i]);

				// check for addr type
				if (!found_addr && addr_types.containsKey(tokens[i])) {
					found_addr = true;
					confidence += addr_types.get(tokens[i]);
					result.append(tokens[i]+" ");
					continue;
				}
				
				// check for city
				// (TODO) very weak checking atm
	//			if (!found_city && cities.containsKey(tokens[i])) {
				if (cities.containsKey(tokens[i])) {
					found_city = true;
					confidence += cities.get(tokens[i]);
					result.append(tokens[i]+" ");
					continue;
				}

				// check for state
				if (!found_state && states.containsKey(tokens[i])) {
					found_state = true;
					confidence += states.get(tokens[i]);
					result.append(tokens[i]+" ");
					continue;
				}

				// check for zip code (if a city or a state has been found)
				if ((found_city || found_state) && !found_zip && isZip(tokens[i])) {
					found_zip = true;
					confidence += 20;
					result.append(tokens[i]+" ");
					continue;
				}

				// allow some buffer space for guessing street name (only if no addr/city/state has been found)
				if (!found_addr && !found_city && !found_state && !addr_types.containsKey(tokens[i])) {
					// could be a directional key
					if (dirs.containsKey(tokens[i])) {
						result.append(tokens[i]+" ");
						confidence += dirs.get(tokens[i]);
						continue;
					// see if we still have buffer space
					} else if (name_buffer < NAME_BUFFER) {
						name_buffer++;
						//confidence += 10;
						result.append(tokens[i]+" ");
						continue;
					}
				}

				// if we've reach the threshold, store into potential hashmap
				if (confidence >= THRESHOLD) {
					// System.out.println(" reached threshold, adding: "+result.toString());
					potential.add(result.toString());
					result = new StringBuffer();
					found_int = found_addr = found_city = found_state = found_zip = false;
					confidence = name_buffer = 0;
					// if current token is an int, start again
					if (isInt(tokens[i])) {
						found_int = true;
						result.append(tokens[i]+" ");
						confidence = START_CONFIDENCE;
					}
					continue;
				}

				// doesn't match anything, assume it's bad input
				confidence -= 10;

				// if confidence is 0, reset
				if (confidence < 0) {
					found_int = found_addr = found_city = found_state = found_zip = false;
					confidence = name_buffer = 0;
				}
			}
		}

		// case at the end of the parse
		if (confidence >= THRESHOLD) {
			// System.out.println(" reached threshold, adding: "+result.toString());
			potential.add(result.toString());
		}
		return potential;
	}
}
