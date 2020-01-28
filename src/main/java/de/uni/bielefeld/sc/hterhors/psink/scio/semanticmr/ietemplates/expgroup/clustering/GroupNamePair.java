package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;

public class GroupNamePair implements Comparable<GroupNamePair> {

	final public DocumentLinkedAnnotation groupName1;
	final public DocumentLinkedAnnotation groupName2;
	final public boolean sameCluster;

	public GroupNamePair(DocumentLinkedAnnotation groupName1, DocumentLinkedAnnotation groupName2,
			boolean sameCluster) {
		this.groupName1 = groupName1;
		this.groupName2 = groupName2;
		this.sameCluster = sameCluster;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupName2 == null) ? 0 : groupName2.hashCode());
		result = prime * result + ((groupName1 == null) ? 0 : groupName1.hashCode());
		result = prime * result + (sameCluster ? 1231 : 1237);
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
		GroupNamePair other = (GroupNamePair) obj;
		if (groupName2 == null) {
			if (other.groupName2 != null)
				return false;
		} else if (!groupName2.equals(other.groupName2))
			return false;
		if (groupName1 == null) {
			if (other.groupName1 != null)
				return false;
		} else if (!groupName1.equals(other.groupName1))
			return false;
		if (sameCluster != other.sameCluster)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GroupNamePair [groupName1=" + groupName1.getSurfaceForm() + ", groupName2="
				+ groupName2.getSurfaceForm() + ", sameCluster=" + sameCluster + "]";
	}

	@Override
	public int compareTo(GroupNamePair o) {
		return Boolean.compare(this.sameCluster, o.sameCluster);
	}
}
