import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Main {
	private static Set<Office> offices = new HashSet<>();
	private static Set<String> wanted;
	private static Network network;

	private static String baseDir;
	private static String commandsFilePath, officesFilePath, wantedFilePath;

	private static Office getOffice(String officeName) {
		for (Office o : offices) {
		    if (o.getName().equals(officeName)) {
				return o;
			}
		}
		return null;
	}

	private static List<String> readFileIntoLine(String path) throws Exception {
		int count = 0;
		List<String> lines = new ArrayList<>();
		File file = new File(path);
		FileReader fileReader = new FileReader(file);

		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		int index = 0;
		while ((line = bufferedReader.readLine()) != null) {
			if (index == 0) {
				count = Integer.parseInt(line);
			} else if (line != null && line.length() != 0) {
				lines.add(line);
			}
			index ++;
		}
		fileReader.close();

		if (lines.size() != count) {
			throw new Exception ("Record number does not match: " + path);
		}
		return lines;
	}

	private static Set<Office> initOffices(String path) throws Exception {
		Set<Office> offices = new HashSet<>();
		List<String> lines = readFileIntoLine(path);
		for (String line : lines) {
			String[] parts = line.split(" ");
			if (parts.length == 6) {
				Office o = new Office(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
						Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
				offices.add(o);
			}
		}
		return offices;
	}

	private static Set<String> initWanted(String path) throws Exception {
		Set<String> wanted = new HashSet<>();
		List<String> lines = readFileIntoLine(path);
		for (String line : lines) {
			wanted.add(line.trim());
		}
		return wanted;
	}

	private static void prepareFiles() {
		baseDir = System.getProperty("user.dir");
		commandsFilePath = baseDir + "\\commands.txt";
		officesFilePath = baseDir + "\\offices.txt";
		wantedFilePath = baseDir + "\\wanted.txt";
	}

	public static void main(String[] args) throws Exception {
		prepareFiles();
		List<String> commands;
		network = new Network();
		try {
			wanted = initWanted(wantedFilePath);
			offices = initOffices(officesFilePath);
			for (Office o : offices) {
				o.setWanted(wanted);
				o.setNetwork(network);
			}
			network.populateOffices(offices);
			commands = readFileIntoLine(commandsFilePath);
		} catch (Exception e) {
			//File reading problem, exit the program
			throw new Exception("Problem happened", e);
		}

		//Initialize Logging
		Logging.initialize(offices);

		int idx = 0;
		int day = 1;
		int totalDays = 0;

		List<Integer> skipIDX = new ArrayList<Integer>();
		
		boolean destroyOffice = false;
		boolean previousPickUpGood = false;
		boolean hasPendingDeliverables = true;
		boolean sneakOn = false;
		
		for (int x = 0 ; x < commands.size(); x++) {
			String cmd = commands.get(x);
			if (cmd.equals("DAY")) {
				totalDays++;
			}
		}
		
		while (idx < commands.size()) {
			//Start of the day, check if any in transit items have arrived
			network.checkAndDeliver(day);
			
			String cmd = commands.get(idx);
			if (isDayCommand(cmd)) {
				//End of the day.
				for (Office o : offices) {
					// Remove deliverables longer than 14 days
					o.drop(day);
					// Send accepted deliverables
					o.sendToNetwork();
				}
				//End of the day. Log end of day.
				Logging.endOfDay(Logging.LogType.MASTER, day, null);
				for (Office o : offices) {
					Logging.endOfDay(Logging.LogType.OFFICE, day, o.getName());
				}
				day = day + 1;
			}

			String[] parts = cmd.split(" ");
			if (isPickupCommand(cmd)) {
				String dest = parts[1];
				String recipient = parts[2].trim();

				if (wanted.contains(recipient)) {
					Logging.criminalAppended(Logging.LogType.FRONT, recipient, dest);
					previousPickUpGood = false;
				} else {
					Office office = getOffice(dest);
					destroyOffice = office.destroyOffice(recipient, wanted, day);
					previousPickUpGood = office.pickUp(recipient, day);
					if (destroyOffice) {
						// return All letters to sender
						office.setTransitTime(-1);
						office.returnAllLettersToSender(office, day);
						Logging.officeDestroyed(Logging.LogType.OFFICE, office);
						Logging.officeDestroyed(Logging.LogType.MASTER, office);
					}
				}
			} else if (isLetterCommand(cmd)) {
				String src = parts[1];
				String recipient = parts[2];
				String dest = parts[3];
				String returnRecipient = parts[4];
				Office srcOffice = getOffice(src);
				Office destOffice = getOffice(dest);

				Letter letter = new Letter();
				letter.setInitiatingOffice(srcOffice);
				letter.setDestOffice(destOffice);
				letter.setInitDay(day);
				letter.setDaysInTransit(day);
				letter.setRecipient(recipient);
				letter.setReturnRecipient(returnRecipient);
				letter.setIntendedDest(dest);

				Logging.newDeliverable(Logging.LogType.OFFICE, letter);
				srcOffice.acceptLetterIfGood(letter, sneakOn);
			} else if (isPackageCommand(cmd)) {
				String src = parts[1];
				String recipient = parts[2];
				String dest = parts[3];
				int money = Integer.parseInt(parts[4]);
				int length = Integer.parseInt(parts[5]);

				Office srcOffice = getOffice(src);
				Office destOffice = getOffice(dest);

				Package pkg = new Package();
				pkg.setInitiatingOffice(srcOffice);
				pkg.setDestOffice(destOffice);
				pkg.setInitDay(day);
				pkg.setDaysInTransit(day);
				pkg.setRecipient(recipient);
				pkg.setLength(length);
				pkg.setMoney(money);
				pkg.setIntendedDest(dest);
				
				Logging.newDeliverable(Logging.LogType.OFFICE, pkg);
				srcOffice.acceptPackageIfGood(pkg, sneakOn);
			} else if (isBuildCommand(cmd)) {
				String src = parts[1];
				int transitTime = Integer.parseInt(parts[2]);
				int requiredPostage = Integer.parseInt(parts[3]);
				int capacity = Integer.parseInt(parts[4]);
				int persuasionAmount = Integer.parseInt(parts[5]);
				int maxPackageLength = Integer.parseInt(parts[6]);

				Office office = new Office(src, transitTime, requiredPostage, capacity, persuasionAmount,
						maxPackageLength);
				
//				Iterator<Office> iterator = offices.iterator();
//				while (iterator.hasNext()) {
//					Office o = iterator.next();
//				   	if (office.getName().equals(o.getName())) {
//				   		iterator.remove();
//						Logging.officeDestroyed(Logging.LogType.OFFICE, o);
//				   	}
//				}
				boolean officeExists = false;
				
				for (Office o : offices) {
					if (office.getName().equals(o.getName())) {
						offices.remove(o);
						Logging.officeDestroyed(Logging.LogType.OFFICE, o);
						Logging.officeDestroyed(Logging.LogType.MASTER, o);
						officeExists = true;
					}
				}
				offices.add(office);
				
				if (!officeExists) {
					Logging.createNewLogFile(office);
				}
				
				Logging.newOffice(Logging.LogType.OFFICE, office);
				
				for (Office o : offices) {
					o.setWanted(wanted);
					o.setNetwork(network);
				}	
				network.populateOffices(offices);
				
			} else if (isScienceCommand(cmd)) {
				int daysToTimeTravel = Integer.parseInt(parts[1]);
				
				boolean timeTravelDayValid = timeTravelDayValid(daysToTimeTravel, day, totalDays);
				if (timeTravelDayValid) {
					skipIDX.add(idx);
					String timeTravelCMD = commands.get(idx);
					while (daysToTimeTravel != 0) {
						if (daysToTimeTravel < 0) {
							idx--;
							timeTravelCMD = commands.get(idx);
							if (isDayCommand(timeTravelCMD)) {
								daysToTimeTravel++;
							}
						} else if (daysToTimeTravel > 0) {
							idx++;		
							// To go over the day the command
							timeTravelCMD = commands.get(idx-1);
							if (isDayCommand(timeTravelCMD)) {
								daysToTimeTravel--;
							}
						}
					}
				} else if (!timeTravelDayValid){
					idx = commands.size();
					network.destroyLetters();
					
					for (Office o : offices) {
						o.destroyLetters();
					}
					
					network.destroyPackages();
					
					for (Office o : offices) {
						o.destroyPackages();
					}
					
					for (Office o : offices) {
						o.destroy();
					}
					
				}
			} else if (isSneakCommand(cmd)) {
				sneakOn = true;
			} else if (isGoodCommand(cmd)) {
				if (previousPickUpGood) {
					// skip rest of day
					String goodCMD = commands.get(idx+1);
					while (!isDayCommand(goodCMD)) {
						idx++;	
						goodCMD = commands.get(idx+1);
					}
					Logging.goodEnough(Logging.LogType.MASTER);
				}
			} else if (isNSADelayCommand(cmd)) {
				String recipient = parts[1];
				int delayTime = Integer.parseInt(parts[2]);

				for (Office o : offices) {
					o.delayDeliverables(recipient, day, delayTime);
				}
				network.delayDeliverables(recipient, delayTime);
			} else if (isInflationCommand(cmd)) {
				for (Office o : offices) {
					o.inflationCommand();
				}
			} else if (isDeflationCommand(cmd)) {
				for (Office o : offices) {
					o.deflationCommand();
				}				
			}
			
//			if (cmd.equals("DAY")) {
//				hasPendingDeliverables = hasPendingDeliverables();
//			}
			
			//Ready for next day
			if (!isScienceCommand(cmd)) {
				idx++;
				for (int i : skipIDX) {
					if (idx == i) {
						idx++; 
					}
				}
			}
		}

		Logging.cleanUp();

	}

	static boolean timeTravelDayValid(int daysToTimeTravel, int day, int totalDays) {
		boolean timeTravelDayValid = true;
		if (day + daysToTimeTravel <= 0 || day + daysToTimeTravel > totalDays ) {
			timeTravelDayValid = false;
		}
		return timeTravelDayValid;
	}
	
	static boolean hasPendingDeliverables() {
		//Checks if in network, there are any deliverables.
		//Checks if in offices, if there are any deliverables.
		boolean hasPendingDeliverables = false;
		if (!network.isNetworkEmpty()) {
			hasPendingDeliverables = true;
		}
		if (!hasPendingDeliverables) {
			for (Office o : offices) {
				if (!o.isEmpty()) {
					hasPendingDeliverables = true;
				}
			}
		}
		return hasPendingDeliverables;
	}
  
	static boolean isDayCommand(String command) {
		return command.startsWith("DAY");
	}

	static boolean isPickupCommand(String command) {
		return command.startsWith("PICKUP");
	}

	static boolean isLetterCommand(String command) {
		return command.startsWith("LETTER");
	}

	static boolean isPackageCommand(String command) {
		return command.startsWith("PACKAGE");
	}
	
	static boolean isBuildCommand(String command) {
		return command.startsWith("BUILD");
	}

	static boolean isScienceCommand(String command) {
		return command.startsWith("SCIENCE");
	}

	static boolean isGoodCommand(String command) {
		return command.startsWith("GOOD");
	}
	
	static boolean isNSADelayCommand(String command) {
		return command.startsWith("NSADELAY");
	}
	
	static boolean isSneakCommand(String command) {
		return command.startsWith("SNEAK");
	}

	static boolean isInflationCommand(String command) {
		return command.startsWith("INFLATION");
	}

	static boolean isDeflationCommand(String command) {
		return command.startsWith("DEFLATION");
	}
}
