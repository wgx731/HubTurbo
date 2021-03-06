package storage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Model;
import model.TurboIssue;
import model.TurboLabel;
import model.TurboMilestone;
import model.TurboUser;

import org.eclipse.egit.github.core.PullRequest;

class SerializableIssue {
	private String creator;
	private String createdAt;
	private LocalDateTime updatedAt;
	private int numOfComments;
	private PullRequest pullRequest;
	
	private int id;
	private String title;
	private String description;
	
	// for comments, but not used for current version
	//private String cachedDescriptionMarkup;

	private int parentIssue;
	private boolean state;
	
	private TurboUser assignee;
	private SerializableMilestone milestone;
	private String htmlUrl;
	private List<SerializableLabel> labels;
	
	public SerializableIssue(TurboIssue issue) {
		this.creator = issue.getCreator();
		this.createdAt = issue.getCreatedAt();
		this.updatedAt = issue.getUpdatedAt();
		this.numOfComments = issue.getCommentCount();
		this.pullRequest = issue.getPullRequest();
		
		this.id = issue.getId();
		this.title = issue.getTitle();
		this.description = issue.getDescription();
		//this.cachedDescriptionMarkup = issue.getDescriptionMarkup();
		
		this.parentIssue = issue.getParentIssue();
		this.state = issue.isOpen();
		this.assignee = issue.getAssignee();
		
		TurboMilestone turboMilestone = issue.getMilestone();
		if (turboMilestone != null) {
			this.milestone = new SerializableMilestone(issue.getMilestone());
		} else {
			this.milestone = null;
		}
		
		this.htmlUrl = issue.getHtmlUrl();
		
		ObservableList<TurboLabel> turboLabelObservableList = issue.getLabels();
		List<TurboLabel> turboLabelList = turboLabelObservableList.stream().collect(Collectors.toList());
		this.labels = convertFromListOfTurboLabels(turboLabelList);
	}
	
	private List<SerializableLabel> convertFromListOfTurboLabels(List<TurboLabel> turboLabelsList) {
		List<SerializableLabel> list = new ArrayList<SerializableLabel>();
		if (turboLabelsList == null) {
			return null;
		} else {
			for (TurboLabel label : turboLabelsList) {
				list.add(new SerializableLabel(label));
			}
		}
		return list;
	}
	
	public TurboIssue toTurboIssue(Model model) {
		TurboIssue tI = new TurboIssue(this.title, this.description, model);
		
		tI.setCreator(creator);
		tI.setCreatedAt(createdAt);
		tI.setUpdatedAt(updatedAt);
		tI.setCommentCount(numOfComments);
		tI.setPullRequest(pullRequest);
		
		tI.setId(id);
		//tI.setDescriptionMarkup(cachedDescriptionMarkup);
		
		tI.setParentIssue(parentIssue);
		tI.setOpen(state);
		tI.setAssignee(assignee);
		if (milestone == null) {
			tI.setMilestone(null);
		} else {
			tI.setMilestone(milestone.toTurboMilestone());
		}
			
		tI.setHtmlUrl(htmlUrl);

		ObservableList<TurboLabel> turboLabelList = FXCollections.observableArrayList();
		if (labels == null) {
			tI.setLabels(turboLabelList);
		} else {
			for (SerializableLabel label : labels) {
				turboLabelList.add(label.toTurboLabel());
			}
			tI.setLabels(turboLabelList);
		}
		return tI;
	}
}
