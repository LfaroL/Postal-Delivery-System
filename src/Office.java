import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
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

	private Set<String> wanted;
	public void setWanted(Set<String> wanted) {
		this.wanted = wanted;
	}
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
		boolean hasCriminalRecipient = wanted.contains(letter.getRecipient());
		boolean officeFull = isFull();
		Office destOffice = letter.getDestOffice();
		if (sneakOn) {
			accept(letter);
		} else if ((destOffice != null && destOffice.getTransitTime() != -1)&& !hasCriminalRecipient && !officeFull) {
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
		if (sneakOn) {
			srcOffice.accept(pkg);
		} else if (!hasCriminalRecipient && !officeFull
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
		for (int idx = toMail.size()-1 ; idx >= 0 ; idx--) {
			Deliverable d = toMail.get(idx);
			toMail.remove(idx);
			network.put(d);
			if (d.getDaysInTransit() == d.getInitDay()) {
				Logging.transitSent(Logging.LogType.OFFICE, d);
			}
		}
	}
	
	public void returnAllLettersToSender(Office office, int day) {
		int size = toPickUp.size();
		for (int idx = size-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (d.getDestOffice() == office) { 
				if (ifLetter(d)) {
					returnLetterToSender(d, day, idx);
				}
				else {
					remove(d, idx);
				}
			}
		}
	}
	
	public boolean destroyOffice(String recipient, Set<String> wanted, int day) {
		boolean destroyOffice = false;
		int size = toPickUp.size();
		for (int idx = size-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (recipient.equals(d.getRecipient())) {
				// destroy office if return recipient is criminal
				if (d instanceof Letter) {
					if (wanted.contains(((Letter)d).getReturnRecipient())) {
						destroyOffice = true;	
//						returnAllLettersToSender(recipient,  day);

					}
				}
				// Pick up only one deliverable per pick up command
				break;
			}
		}
		return destroyOffice;
	}
	public boolean pickUp(String recipient, int day) {
		boolean pickedUp = false;
		int size = toPickUp.size();
		for (int idx = size-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (recipient.equals(d.getRecipient())) {
				toPickUp.remove(idx);
				Logging.itemComplete(Logging.LogType.OFFICE, d, day+1);
				pickedUp = true;
			}
		}
		return pickedUp;
	}

	public void drop(int day) {
		int size = toPickUp.size();
		for (int idx = size-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (ifLetter(d) && day - d.getInitDay() > 14) {
				returnLetterToSender(d, day, idx);	
			}
			else if (day - d.getInitDay() > 14) {
				remove(d, idx);
			}
		}
	}
	
	public boolean ifLetter(Deliverable d) {
		return (d instanceof Letter) && !((Letter)d).getReturnRecipient().equals("NONE");
	}
	
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
	
	public void returnLetterToSender(Deliverable d, int day, int idx) {
		Letter letter = switchLetter(d, day);
		toPickUp.remove(idx);
		toMail.add(letter);
		Logging.newDeliverable(Logging.LogType.OFFICE, letter);		
	}
	
	public void remove(Deliverable d, int idx) {
		toPickUp.remove(idx);
		Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
		Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
	}
	
	public void removeAllLetters() {
		int size = toPickUp.size();
		for (int idx = size-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (d instanceof Letter) {
				toPickUp.remove(idx);
			}
		}
	}
	
	public void removeAllPackages() {
		int size = toPickUp.size();
		for (int idx = size-1 ; idx >= 0 ; idx--) {
			toPickUp.remove(idx);
		}	
	}
	
	public void removeAllDeliverables() {
		removeAllLetters();
		removeAllPackages();
	}
	
	public void delayDeliverables(String recipient, int day, int delayTime) {
		int pickUpSize = toPickUp.size();
		for (int idx = pickUpSize-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (recipient.equals(d.getRecipient())) {
				if (d instanceof Letter) {
					Letter letter = delayLetter(d, delayTime);
					toPickUp.remove(idx);
					toMail.add(letter);
				} else {
					Package pkg = delayPackage(d, delayTime);
					toPickUp.remove(idx);
					toMail.add(pkg);
				}
			}
		}
//		int mailSize = toMail.size();
//		for (int idx = mailSize-1 ; idx >= 0 ; idx--) {
//			Deliverable d = toMail.get(idx);
//			if (recipient.equals(d.getRecipient())) {
//				d.setInitDay(d.getInitDay() - delayTime);
//			}
//		}
	}
	
	public void destroyLetters() {
		int pickUpSize = toPickUp.size();
		for (int idx = pickUpSize-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (d instanceof Letter) {
				toPickUp.remove(idx);
				Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
				Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
			}
		}
	}
	
	public void destroyPackages() {
		int pickUpSize = toPickUp.size();
		for (int idx = pickUpSize-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			toPickUp.remove(idx);
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
		for (int idx = pickUpSize-1 ; idx >= 0 ; idx--) {
			setRequiredPostage(getRequiredPostage() + 1);
			setPersuasionAmount(getPersuasionAmount() + 1);		
		}
		int mailSize = toMail.size();
		for (int idx = mailSize-1 ; idx >= 0 ; idx--) {
			setRequiredPostage(getRequiredPostage() + 1);
			setPersuasionAmount(getPersuasionAmount() + 1);
		}
	}
	
	public void deflationCommand() {
		int pickUpSize = toPickUp.size();
		for (int idx = pickUpSize-1 ; idx >= 0 ; idx--) {
			if (getRequiredPostage() - 1 != 0) {
				setRequiredPostage(getRequiredPostage() - 1);
			}
			setPersuasionAmount(getPersuasionAmount() - 1);	
		}
		int mailSize = toMail.size();
		for (int idx = mailSize-1 ; idx >= 0 ; idx--) {
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
