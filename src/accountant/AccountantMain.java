package accountant;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class AccountantMain {

	private double assets = 0;
	private Map<LocalDate, List<Action>> calendar = new HashMap<LocalDate, List<Action>>();
	private Map<String, List<Rule>> recurring = new HashMap<>();

	@SuppressWarnings("resource")
	public AccountantMain() throws ParseException, IOException {
		new BufferedReader(new FileReader("data.txt")).lines().forEach(this::process);

		LocalDate from = LocalDate.now();//.plusDays(1);
		LocalDate to = from.plusMonths(4).minusDays(from.getDayOfMonth());
		//to = LocalDate.parse("2015-09-04");

		double min = Double.MAX_VALUE;
		double expenses = 0.0;
		double investments = 0.0;
		LocalDate minDate = from;
		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy dd MMM");

		for (LocalDate date = from; date.isBefore(to) || date.isEqual(to); date = date.plusDays(1)) {
			// Fixed actions
			if (calendar.containsKey(date)) {
				for (Action action : calendar.get(date)) {
					if (action.type.equalsIgnoreCase("INVEST")) {
						double investment = Math.floor(Math.abs(action.value/100) * assets);
						System.out.println("Invested " + investment);
						investments += investment;
					} else {
						double change = Math.floor(action.value
								* Ticker.getCurrencyRate(action.type, "SEK"));
						assets += change;
						if (change < 0)
							expenses += Math.abs(change);
						//System.out.println((change < 0 ? " Expense " : " Income ") + change + " " + action.name);
					}
				}
			}

			// Recurring
			for (String r : recurring.keySet()) {
				boolean doIt = r.equalsIgnoreCase("DAY");

				if (!doIt) {
					try {
						int dayOfMonth = 0;

						if (r.equalsIgnoreCase("LAST"))
							dayOfMonth = date.dayOfMonth().withMaximumValue().getDayOfMonth();
						else
							dayOfMonth = Integer.parseInt(r);

						doIt = date.getDayOfMonth() == dayOfMonth;
					} catch (Exception e) {
						int dayOfWeek = 0;
						switch (r) {
							case "MONDAY":
								dayOfWeek = 1;
								break;
							case "TUESDAY":
								dayOfWeek = 2;
								break;
							case "WEDNESDAY":
								dayOfWeek = 3;
								break;
							case "THURSDAY":
								dayOfWeek = 4;
								break;
							case "FRIDAY":
								dayOfWeek = 5;
								break;
							case "SATURDAY":
								dayOfWeek = 6;
								break;
							case "SUNDAY":
								dayOfWeek = 7;
								break;
						}
						doIt = date.getDayOfWeek() == dayOfWeek;
					}
				}

				if (doIt) {
					for (Rule rule : recurring.get(r)) {
						if (rule.from.isBefore(date.plusDays(1)) && (rule.to == null || rule.to.isAfter(date.minusDays(1)))) {
							double change = Math.floor(rule.value
									* Ticker.getCurrencyRate(rule.type, "SEK"));

							assets += change;

							if (change < 0)
								expenses += Math.abs(change);
						}
					}
				}
			}

			if (assets < min) {
				min = assets;
				minDate = date;
			}

			//if (date.getDayOfWeek() == 7)
				System.out.println(formatter.print(date) + ":  " + (int) assets);
		}
		int days = Days.daysBetween(from, to).getDays();
		System.out.println();
		System.out.println("Days:                                           " + days);
		System.out.println("Assets:                                         " + (int)assets);
		System.out.println("Assets/day:                                     " + (int)(assets / days));
		System.out.println("Expense/day:                                    " + (int)(expenses / days));
		System.out.println("Minimum:                                        " + (int)min + " (" + minDate + ")");
		System.out.println("Investments:                                    " + (int)investments);
		System.out.println("Average recurring expenses per month (30 days): " + totalRecurringExpenses());
		System.out.println("Remaining days:                                 " + (int)(assets / (expenses / days)));
	}

	private int totalRecurringExpenses() throws IOException {
		double total = 0;
		for (String r : recurring.keySet()) {
			double times = 0;
			if (r.equalsIgnoreCase("DAY")) {
				times = 30;
			} else if (r.equalsIgnoreCase("LAST")) {
				times = 1;
			} else {
				try {
					Integer.parseInt(r);
					times = 1;
				} catch (Exception e) {
					times = 30.0/7;
				}
			}
			if (times > 0) {
				for (Rule rule : recurring.get(r)) {

					double change = Math.floor(rule.value
							* Ticker.getCurrencyRate(rule.type, "SEK"));

					if (change < 0)
						total += Math.abs(change * times);
				}
			}
		}
		return (int)Math.round(total);
	}

	private void process(String line) {
		try {
			if (line.indexOf("//") > 0)
				line = line.substring(0, line.indexOf("//"));
			line = line.replace((char)65279, ' ').trim();
			if (line.isEmpty() || line.startsWith("//"))
				return;
			
			if (line.toUpperCase().startsWith("ASSET")) {

				String[] c = line.split("\\s+", 3);
				// Asset
				assets += Double.parseDouble(c[1])
						* Ticker.getCurrencyRate(c[2], "SEK");

			} else if (line.toUpperCase().startsWith("EVERY")) {

				// Recurring
				String[] c = line.split("\\s+", 9);
				String rule = c[1].toUpperCase();
				if (!recurring.containsKey(rule)) {
					recurring.put(rule, new ArrayList<>());
				}
				LocalDate from = c.length > 4 ? c[4].equalsIgnoreCase("from") ? LocalDate.parse(c[5]) : LocalDate.now() : LocalDate.now();
				LocalDate to = c.length > 6 ? LocalDate.parse(c[7]) : c.length > 4 ? c[4].equalsIgnoreCase("to") ? LocalDate.parse(c[5]) : null : null;

				recurring.get(rule).add(new Rule(from, to, Double.parseDouble(c[2]), c[3]));

			} else {
				// Fixed dates
				String[] c = line.split("\\s+", 4);
				LocalDate date = LocalDate.parse(c[0]);
				if (!calendar.containsKey(date)) {
					calendar.put(date, new ArrayList<>());
				}
				calendar.get(date).add(
						new Action(date, Double.parseDouble(c[1]), c[2], c.length > 3 ? c[3] : ""));
			}

		} catch (IllegalArgumentException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class Rule {

		LocalDate from;
		LocalDate to;
		double value;
		String type;
		String string;

		Rule(LocalDate from, LocalDate to, double value, String type) {
			this.from = from;
			this.to = to;
			this.value = value;
			this.type = type;
		}

		public String toString() {
			if (string == null) {
				StringBuilder sb = new StringBuilder();
				sb.append(from + " to " + to);
				sb.append(value < 0 ? " EXPENSE " : " INCOME ");
				sb.append(Math.abs(value));
				sb.append(" ");
				sb.append(type);
				string = sb.toString();
			}
			return string;
		}

	}

	class Action {

		LocalDate date;
		double value;
		String type;
		String name;
		String string;

		Action(LocalDate date, double value, String type, String name) {
			this.date = date;
			this.value = value;
			this.type = type;
			this.name = name;
		}

		public String toString() {
			if (string == null) {
				StringBuilder sb = new StringBuilder();
				sb.append(date);
				sb.append(value < 0 ? " EXPENSE " : " INCOME ");
				sb.append(Math.abs(value));
				sb.append(" ");
				sb.append(type);
				sb.append(" ");
				sb.append(name);
				string = sb.toString();
			}
			return string;
		}

	}

	public static void main(String[] args) throws IOException {
		try {
			new AccountantMain();
		} catch (FileNotFoundException | ParseException e) {
			e.printStackTrace();
		}
	}
}
