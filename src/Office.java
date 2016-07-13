import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Office {
	private String name;
	private int transitTime;
	private int requiredPostage;
	private int capacity;
	private int persuasionAmount;
	private int maxPackageLength;
	private List<Deliverable> toMail = new ArrayList<>();
	private List<Deliverable> toPickUp = new ArrayList<>();

	// Set to store black-listed criminals
	private Set<String> wanted;
	public void setWanted(Set<String> wanted) {
		this.wanted = wanted;
	}
	
	// Contains reference to network for sending deliverables
	private Network network;
	public void setNetwork(Network network) {
		this.network = network;
	}

	public Office(String name, int transitTime, int requiredPostage,
			int capacity, int persuasionAmount, int maxPackageLength) {
		super();
		this.name = name;
		this.transitTime = transitTime;
		this.requiredPostage = requiredPostage;
		this.capacity = capacity;
		this.persuasionAmount = persuasionAmount;
		this.maxPackageLength = maxPackageLength;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getTransitTime() {
		return transitTime;
	}
	public void setTransitTime(int transitTime) {
		this.transitTime = transitTime;
	}
	public int getRequiredPostage() {
		return requiredPostage;
	}
	public void setRequiredPostage(int requiredPostage) {
		this.requiredPostage = requiredPostage;
	}
	public int getCapacity() {
		return capacity;
	}
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	public int getPersuasionAmount() {
		return persuasionAmount;
	}
	public void setPersuasionAmount(int persuasionAmount) {
		this.persuasionAmount = persuasionAmount;
	}
	public int getMaxPackageLength() {
		return maxPackageLength;
	}
	public void setMaxPackageLength(int maxPackageLength) {
		this.maxPackageLength = maxPackageLength;
	}
	public List<Deliverable> getMail() {
		return toMail;
	}
	
	public void acceptLetterIfGood(Letter letter, boolean sneakOn) {
		// Check if recipient is wanted and if office is full
		boolean hasCriminalRecipient = wanted.contains(letter.getRecipient());
		boolean officeFull = isFull();
		Office destOffice = letter.getDestOffice();
		
		// If package is bribed to be delivered, accept regardless of restrictions
		if (sneakOn) {
			accept(letter);
		} 
		// Check restrictions (destination office exists, recipient not criminal and office is not full)
		else if ((destOffice != null && destOffice.getTransitTime() != -1)&& !hasCriminalRecipient && !officeFull) {
			accept(letter);
		} else {
			Logging.rejectDeliverable(Logging.LogType.MASTER, letter);
			Logging.rejectDeliverable(Logging.LogType.OFFICE, letter);
		}
	}

	public void acceptPackageIfGood(Package pkg, boolean sneakOn) {
		boolean hasCriminalRecipient = wanted.contains(pkg.getRecipient());
		boolean officeFull = isFull();
		boolean lengthFitSrc = (pkg.getLength() < getMaxPackageLength());
		Office destOffice = pkg.getDestOffice();
		Office srcOffice = pkg.getInitiatingOffice();
		
		// If package is bribed to be delivered, accept regardless of restrictions
		if (sneakOn) {
			srcOffice.accept(pkg);
		} 
		// Check restrictions (destination office exists, 
		// package doesn't exceed max length of source and destination office,
		// required postage fee is paid, and
		// recipient is not criminal and office is not full)
		else if (!hasCriminalRecipient && !officeFull
				&& lengthFitSrc && pkg.getMoney() >= srcOffice.getRequiredPostage() &&
				(destOffice != null && destOffice.getTransitTime() != -1) && 
				(pkg.getLength() <= destOffice.getMaxPackageLength())) {
			srcOffice.accept(pkg);
		} else if (pkg.getMoney() >= (srcOffice.getRequiredPostage() + srcOffice.getPersuasionAmount())) {
			Logging.briberyDetected(Logging.LogType.MASTER, pkg);
			srcOffice.accept(pkg);
		} else {
			Logging.rejectDeliverable(Logging.LogType.MASTER, pkg);
			Logging.rejectDeliverable(Logging.LogType.OFFICE, pkg);
		}
	}
	
	//Receive from person
	public void accept(Deliverable d) {
		Logging.deliverableAccepted(Logging.LogType.OFFICE, d);
		toMail.add(d);
	}

	//Receive from network
	public void receiveFromNetwork(Deliverable d) {
		if (d instanceof Package) {
			Package p = (Package) d;
			if (this.maxPackageLength < p.getLength()) {
				Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
				Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
				return;
			}
		}

		if (isFull()) {
			Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
			Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
			return;
		}

		toPickUp.add(d);
	}

	public void sendToNetwork() {
		for (int index = toMail.size()-1 ; index >= 0 ; index--) {
			Deliverable d = toMail.get(index);
			toMail.remove(index);
			network.put(d);
			if (d.getDaysInTransit() == d.getInitDay()) {
				Logging.transitSent(Logging.LogType.OFFICE, d);
			}
		}
	}
	
	public void returnAllLettersToSender(Office office, int day) {
		int size = toPickUp.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			if (d.getDestOffice() == office) { 
				if (ifLetter(d)) {
					returnLetterToSender(d, day, index);
				}
				else {
					remove(d, index);
				}
			}
		}
	}
	
	public boolean checkIfWanted(String recipient) {
		boolean wantedCriminal = false;
		int size = toPickUp.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			if (recipient.equals(d.getRecipient())) {
				if (d instanceof Letter) {
					if (wanted.contains(((Letter)d).getReturnRecipient())) {
						wantedCriminal = true;	
//						returnAllLettersToSender(recipient,  day);

					}
				}
				// Pick up only one deliverable per pick up command
				break;
			}
		}
		return wantedCriminal;
	}
	
	public boolean pickUp(String recipient, int day) {
		boolean pickedUp = false;
		int size = toPickUp.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			if (recipient.equals(d.getRecipient())) {
				toPickUp.remove(index);
				Logging.itemComplete(Logging.LogType.OFFICE, d, day+1);
				pickedUp = true;
			}
		}
		return pickedUp;
	}

	public void drop(int day) {
		int size = toPickUp.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			if (ifLetter(d) && day - d.getInitDay() > 14) {
				returnLetterToSender(d, day, index);	
			}
			else if (day - d.getInitDay() > 14) {
				remove(d, index);
			}
		}
	}
	
	public boolean ifLetter(Deliverable d) {
		return (d instanceof Letter) && !((Letter)d).getReturnRecipient().equals("NONE");
	}
	
	// Replaces the recipient of the letter with the sender
	public Letter switchLetter(Deliverable d, int day) {
		Letter letter = new Letter();
		letter.setDestOffice(d.getInitiatingOffice());
		letter.setInitDay(day);
		letter.setDaysInTransit(day);
		letter.setInitiatingOffice(d.getDestOffice());
		letter.setRecipient(((Letter)d).getReturnRecipient());
		letter.setReturnRecipient("NONE");
		letter.setIntendedDest(d.getIntendedDest());
		
		return letter;
	}
	
	// Add the days in transit of a letter
	public Letter delayLetter(Deliverable d, int delayTime) {
		Letter letter = new Letter();
		letter.setDestOffice(d.getDestOffice());
		letter.setInitDay(d.getInitDay());
		letter.setDaysInTransit(d.getDaysInTransit() + delayTime);
		letter.setInitiatingOffice(d.getInitiatingOffice());
		letter.setRecipient(d.getRecipient());
		letter.setReturnRecipient(((Letter)d).getReturnRecipient());
		letter.setIntendedDest(d.getIntendedDest());
		
		return letter;		
	}
	
	// Add the days in transit of a package
	public Package delayPackage(Deliverable d, int delayTime) {
		Package pkg = new Package();
		pkg.setInitiatingOffice(d.getInitiatingOffice());
		pkg.setDestOffice(d.getDestOffice());	
		pkg.setInitDay(d.getInitDay());
		pkg.setDaysInTransit(d.getDaysInTransit() + delayTime);
		pkg.setRecipient(d.getRecipient());
		pkg.setLength(((Package)d).getLength());
		pkg.setMoney(((Package)d).getMoney());
		pkg.setIntendedDest(d.getIntendedDest());

		return pkg;		
	}

	// When a letter is not picked up after 14 days, return to sender
	public void returnLetterToSender(Deliverable d, int day, int index) {
		Letter letter = switchLetter(d, day);
		toPickUp.remove(index);
		toMail.add(letter);
		Logging.newDeliverable(Logging.LogType.OFFICE, letter);		
	}
	
	public void remove(Deliverable d, int index) {
		toPickUp.remove(index);
		Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
		Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
	}
	
	public void removeAllLetters() {
		int size = toPickUp.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			if (d instanceof Letter) {
				toPickUp.remove(index);
			}
		}
	}
	
	public void removeAllPackages() {
		int size = toPickUp.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			toPickUp.remove(index);
		}	
	}
	
	public void removeAllDeliverables() {
		removeAllLetters();
		removeAllPackages();
	}
	
	public void delayDeliverables(String recipient, int day, int delayTime) {
		int pickUpSize = toPickUp.size();
		for (int index = pickUpSize-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			if (recipient.equals(d.getRecipient())) {
				if (d instanceof Letter) {
					Letter letter = delayLetter(d, delayTime);
					toPickUp.remove(index);
					toMail.add(letter);
				} else {
					Package pkg = delayPackage(d, delayTime);
					toPickUp.remove(index);
					toMail.add(pkg);
				}
			}
		}
//		int mailSize = toMail.size();
//		for (int index = mailSize-1 ; index >= 0 ; index--) {
//			Deliverable d = toMail.get(index);
//			if (recipient.equals(d.getRecipient())) {
//				d.setInitDay(d.getInitDay() - delayTime);
//			}
//		}
	}
	
	public void destroyLetters() {
		int pickUpSize = toPickUp.size();
		for (int index = pickUpSize-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			if (d instanceof Letter) {
				toPickUp.remove(index);
				Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
				Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
			}
		}
	}
	
	public void destroyPackages() {
		int pickUpSize = toPickUp.size();
		for (int index = pickUpSize-1 ; index >= 0 ; index--) {
			Deliverable d = toPickUp.get(index);
			toPickUp.remove(index);
			Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
			Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
		}
	}
	
	public void destroy() {
		Logging.officeDestroyed(Logging.LogType.OFFICE, this);
		Logging.officeDestroyed(Logging.LogType.MASTER, this);
	}
	
	public void inflationCommand() {
		int pickUpSize = toPickUp.size();
		for (int index = pickUpSize-1 ; index >= 0 ; index--) {
			setRequiredPostage(getRequiredPostage() + 1);
			setPersuasionAmount(getPersuasionAmount() + 1);		
		}
		int mailSize = toMail.size();
		for (int index = mailSize-1 ; index >= 0 ; index--) {
			setRequiredPostage(getRequiredPostage() + 1);
			setPersuasionAmount(getPersuasionAmount() + 1);
		}
	}
	
	public void deflationCommand() {
		int pickUpSize = toPickUp.size();
		for (int index = pickUpSize-1 ; index >= 0 ; index--) {
			if (getRequiredPostage() - 1 != 0) {
				setRequiredPostage(getRequiredPostage() - 1);
			}
			setPersuasionAmount(getPersuasionAmount() - 1);	
		}
		int mailSize = toMail.size();
		for (int index = mailSize-1 ; index >= 0 ; index--) {
			if (getRequiredPostage() - 1 != 0) {
				setRequiredPostage(getRequiredPostage() - 1);
			}			
			setPersuasionAmount(getPersuasionAmount() - 1);
		}	
	}
	
	public boolean isFull() {
		return (this.toMail.size() + this.toPickUp.size()) >= capacity;
	}

	public boolean isEmpty() {
		return (this.toMail.size() + this.toPickUp.size()) == 0;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Office) && (this.name.equals(((Office)obj).getName()));
	}

	@Override
 	public String toString() {
 		return this.name;
 	}
}
