package accountant;

public class Investor {
	
	public Investor() {
		double netWorth = 7000;
		double investmentPerDay = 5000/30;
		double gain = 1.07;
		double requiredMonthly;

		requiredMonthly = 11000; // fattigdomsgränsen
		requiredMonthly = 9904; // student
		requiredMonthly = 21000; // ingenjörslön

		double requiredDaily = requiredMonthly / 30;

		int day = 0;
		double dailyGain = Math.pow(gain, 1.0 / 365);
		System.out.println(dailyGain);
		
		while (day < 365 * 40) {
			netWorth *= dailyGain;
			netWorth += investmentPerDay;
			System.out.println(day + " -> " + Math.round(netWorth * (dailyGain - 1)));
			day++;
		}
		System.out.println();
		System.out.println(Math.round(day * investmentPerDay));
		System.out.println(Math.round(netWorth));
		System.out.println(Math.pow(dailyGain, day));
		System.out.println();
		System.out.println((Math.pow(dailyGain, 30) - 1) * netWorth);
		System.out.println();
		System.out.println((int)(day / 365) + " y " + (day % 365) + " d");
		System.out.println(netWorth * (dailyGain - 1) + " d^-1");
	}
	
	public static void main(String[] args) {
		new Investor();
	}

}
