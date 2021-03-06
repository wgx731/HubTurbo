package model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.eclipse.egit.github.core.Milestone;

public class TurboMilestone implements TurboResource {
	
	private static final String CUSTOM_DATETIME_PATTERN = "d MMMM yyyy";
	/*
	 * Attributes, Getters & Setters
	 */
	
	private int number = -1;
	public int getNumber() {return number;}
	public void setNumber(int number) {
		this.number = number;
	}
	
	private String title = "";
    public final String getTitle() {return title;}
    public final void setTitle(String value) {title = value;}
	
	private String state;
	public String getState() {return state;}
	public void setState(String state) {
		this.state = state;
	}
	
	private String description;
	public String getDescription() {return description;}
	public void setDescription(String description) {this.description = description;}
	
	private LocalDate dueOn;
	public LocalDate getDueOn() {return dueOn;}
	public void setDueOn(LocalDate dueOn) {
		this.dueOn = dueOn;
		if (this.dueOn != null) {
			setDueOnString(getDueOn().format(DateTimeFormatter.ofPattern(CUSTOM_DATETIME_PATTERN)));
		}
	}
	public void setDueOn(String dueOnString) {
		if (dueOnString == null) {
			this.dueOn = null;
		} else {
			this.dueOn = LocalDate.parse(dueOnString, DateTimeFormatter.ofPattern(CUSTOM_DATETIME_PATTERN));
		}
	}

	private String dueOnString;
    public final String getDueOnString() {return dueOnString;}
    public final void setDueOnString(String value) {dueOnString = value;}
	
	private int closed = 0;
    public final int getClosed() {return closed;}
    public final void setClosed(Integer value) {closed = value;}
    
    private int open = 0;
    public final int getOpen() {return open;}
    public final void setOpen(Integer value) {open = value;}
	
	/*
	 * Constructors and Public Methods
	 */
	
	public TurboMilestone(TurboMilestone other) {
		this(other.toGhResource());
	}

	public TurboMilestone() {
		setTitle("");
	}
	
	public TurboMilestone(String title) {
		setTitle(title);
	}
	
	public TurboMilestone(Milestone milestone) {
		assert milestone != null;
		setTitle(milestone.getTitle());
		this.number = milestone.getNumber();
		this.state = milestone.getState();
		this.description = milestone.getDescription();
		setDueOn(toLocalDate(milestone.getDueOn()));
		setClosed(milestone.getClosedIssues());
		setOpen(milestone.getOpenIssues());
	}
	
	public Milestone toGhResource() {
		Milestone ghMilestone = new Milestone();
		ghMilestone.setTitle(getTitle());
		ghMilestone.setNumber(number);
		ghMilestone.setState(state);
		ghMilestone.setDescription(description);
		ghMilestone.setDueOn(toDate(dueOn));
		return ghMilestone;
	}

	@Override
	public void copyValuesFrom(TurboResource other) {
		assert other != null;
		assert other instanceof TurboMilestone;

		TurboMilestone obj = (TurboMilestone) other;
		setTitle(obj.getTitle());
		setState(obj.getState());
		setDescription(obj.getDescription());
		setDueOn(obj.getDueOn());
		setClosed(obj.getClosed());
		setOpen(obj.getOpen());
	}
	
	public double getProgress(){
		if (getClosed() == 0 && getOpen() == 0) {
			return 0;
		}
		double total = getClosed() + getOpen();
		double progress = getClosed() / total;
		return progress;
	}
	
	public Long relativeDueDateInDays() {
		if (getDueOn() == null) {
			return null;
		}
		long daysUntilDueDate = LocalDate.now().until(getDueOn(), ChronoUnit.DAYS);
		return daysUntilDueDate;
	}
	
	public String relativeDueDateInString() {
		Long days = relativeDueDateInDays();
		if (days == null) {return null;}
		if (days < 0) {return "over";}
		if (days == 0) {return "today";}
		if (days > 0) {return days.toString() + " days";}
		
		return ""; //stub value, should never be returned
	}
	
	@Override
	public String toString() {
		return "TurboMilestone [title=" + title + "]";
	}
	
	/**
	 * A convenient string representation of this object, for purposes of readable logs.
	 * @return
	 */
	public String logString() {
		return title;
	}
	
	/*
	 * Private Methods
	 */
	
	private LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}
		Instant instant = date.toInstant();
		ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
		LocalDate localDate = zdt.toLocalDate();
		// Minus one day as GitHub API milestone due date is one day
		// ahead of GitHub UI milestone due date
		return localDate.minusDays(1);
	}

	private Date toDate(LocalDate localDate) {
		if (localDate == null) {
			return null;
		}
		// Plus one day as GitHub UI milestone due date is one day
		// behind of GitHub API milestone due date
		long epochInMilliseconds = (localDate.toEpochDay() + 1) * 24 * 60 * 60 * 1000;
		Date date = new Date(epochInMilliseconds);
		return date;
	}
	
	/*
	 * Overridden Methods
	 */

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TurboMilestone other = (TurboMilestone) obj;
		if (number != other.number)
			return false;
		if (title != other.title)
			return false;
		return true;
	}
	
}
