package accountant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Ticker {

	private static Map<String, Map<String, Double>> cache = new HashMap<String, Map<String, Double>>();

	public static double getCurrencyRate(String from, String to)
			throws IOException {
		
		if (from.equalsIgnoreCase("EUR")) {
			return 10.61;
		}
		if (from.equalsIgnoreCase("USD")) {
			return 9.30;
		}
		if (from.equalsIgnoreCase("DKK")) {
			return 1.42;
		}
		if (from.equalsIgnoreCase("SEK")) {
			return 1;
		}
		if (from.equalsIgnoreCase("GBP")) {
			return 12.16;
		}
		
		String a = from.compareTo(to) < 0 ? from : to;
		String b = from.compareTo(to) < 0 ? to : from;
		if (cache.containsKey(a) && cache.get(a).containsKey(b)) {
			return cache.get(a).get(b);
		} else {
			URL url = new URL("http://rate-exchange.appspot.com/currency?from="
					+ from + "&to=" + to);
			String response = new BufferedReader(new InputStreamReader(
					url.openStream())).readLine();
			response = response.substring(response.indexOf("\"rate\": ") + 8);
			response = response.substring(0, response.indexOf(","));
			double rate = Double.parseDouble(response);
			if (!cache.containsKey(a)) {
				cache.put(a, new HashMap<>());
			}
			cache.get(a).put(b, rate);
			return rate;
		}
	}

}
