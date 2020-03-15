package net.nocturne.utils;

public class NameRandomizer {

	final private static String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";

	final private static java.util.Random rand = new java.util.Random();

	// consider using a Map<String,Boolean> to say whether the identifier is being used or not 
	// final Set<String> identifiers = new HashSet<String>();

	public static String randomName() {
			StringBuilder builder = new StringBuilder();
			while(builder.toString().length() == 0) {
					int length = rand.nextInt(5)+5;
					for(int i = 0; i < length; i++) {
							builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
					}
					//if(identifiers.contains(builder.toString())) {
					//		builder = new StringBuilder();
					//}
			}
			return lexicon.charAt(rand.nextInt(lexicon.length())) + builder.toString().toLowerCase();
	}

}
