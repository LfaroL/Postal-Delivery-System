public class Deliverable {
	private Office initiatingOffice;
	private Office destOffice;
	private String intendedDest;
	private String recipient;
	private int initDay;
	private int daysInTransit;
	
	
	public int getDaysInTransit() {
		return daysInTransit;
	}
	public void setDaysInTransit(int daysInTransit) {
		this.daysInTransit = daysInTransit;
	}
	public int getInitDay() {
		return initDay;
	}
	public void setInitDay(int initDay) {
		this.initDay = initDay;
	}
	public Office getInitiatingOffice() {
		return initiatingOffice;
	}
	public void setInitiatingOffice(Office iniatingOffice) {
		this.initiatingOffice = iniatingOffice;
	}
	public Office getDestOffice() {
		return destOffice;
	}
	public void setDestOffice(Office destOffice) {
		this.destOffice = destOffice;
	}
	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
	public String getIntendedDest() {
		return intendedDest;
	}
	public void setIntendedDest(String intendedDest) {
		this.intendedDest = intendedDest;
	}

}
