import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Class that handles deliverable in transit */

public class Network {
	private List<Deliverable> deliverablesInTransit = new ArrayList<>();
	private Map<String, Office> officeMap = new HashMap<>();
	public void put(Deliverable d) {
		deliverablesInTransit.add(d);
	}

	public void checkAndDeliver(int day) {
		for (int index = deliverablesInTransit.size() -1 ; index >= 0 ; index--) {
			Deliverable d = deliverablesInTransit.get(index);
			Office initOffice = d.getInitiatingOffice();
			Office destOffice = d.getDestOffice();
			if (d.getDaysInTransit() + initOffice.getTransitTime() + 1 <= day) {
				if (d.getDestOffice() != null && d.getDestOffice().getTransitTime() != -1) {
					Logging.transitArrived(Logging.LogType.OFFICE, d);
					deliverablesInTransit.remove(index);
					//put the deliverable into this office
					destOffice.receiveFromNetwork(d);
				} else {
					if (destOffice.ifLetter(d)) {
						Letter letter = destOffice.switchLetter(d, day + 2);
						deliverablesInTransit.remove(index);
						deliverablesInTransit.add(letter);
					} else {
						deliverablesInTransit.remove(index);
					}
				}
			}
		}
	}

	public void populateOffices(Set<Office> offices) {
		for (Office o : offices) {
			officeMap.put(o.getName(), o);
		}
	}

	public boolean isNetworkEmpty() {
		return deliverablesInTransit.size() == 0;
	}
	
	public void destroyLetters() {
		int size = deliverablesInTransit.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = deliverablesInTransit.get(index);
			if (d instanceof Letter) {
				deliverablesInTransit.remove(index);
			}
		}
	}
	
	public void destroyPackages() {
		int size = deliverablesInTransit.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = deliverablesInTransit.get(index);
			deliverablesInTransit.remove(index);
			Logging.deliverableDestroyed(Logging.LogType.MASTER, d);
			Logging.deliverableDestroyed(Logging.LogType.OFFICE, d);
		}
	}
	
	public void delayDeliverables(String recipient, int delayTime) {
		int size = deliverablesInTransit.size();
		for (int index = size-1 ; index >= 0 ; index--) {
			Deliverable d = deliverablesInTransit.get(index);
			if (recipient.equals(d.getRecipient())) {
				if (d instanceof Letter) {
					Letter letter = d.getDestOffice().delayLetter(d, delayTime);
					deliverablesInTransit.remove(index);
					deliverablesInTransit.add(letter);
				} else {
					Package pkg = d.getDestOffice().delayPackage(d, delayTime);
					deliverablesInTransit.remove(index);
					deliverablesInTransit.add(pkg);
				}
			}
		}
	}
}
