package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.clustering.helper;

import java.util.HashSet;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;

public class GroupNamePair implements Comparable<GroupNamePair> {

	final public LiteralAnnotation groupName1;
	final public LiteralAnnotation groupName2;
	final public boolean sameCluster;
	final public double probability;
	final private Set<LiteralAnnotation> groupNames;

	public GroupNamePair(LiteralAnnotation groupName1, LiteralAnnotation groupName2, boolean sameCluster,
			double probability) {
		this.groupName1 = groupName1;
		this.groupName2 = groupName2;
		this.sameCluster = sameCluster;
		this.probability = probability;
		this.groupNames = new HashSet<>();
		groupNames.add(groupName1);
		groupNames.add(groupName2);
	}

	public GroupNamePair(GroupNamePair groupNamePair, boolean sameCluster, double probability) {
		this.groupName1 = groupNamePair.groupName1;
		this.groupName2 = groupNamePair.groupName2;
		this.sameCluster = sameCluster;
		this.probability = probability;
		this.groupNames = new HashSet<>();
		groupNames.add(groupName1);
		groupNames.add(groupName2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupNames == null) ? 0 : groupNames.hashCode());
		long temp;
		temp = Double.doubleToLongBits(probability);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		if (groupNames == null) {
			if (other.groupNames != null)
				return false;
		} else if (!groupNames.equals(other.groupNames))
			return false;
		if (Double.doubleToLongBits(probability) != Double.doubleToLongBits(other.probability))
			return false;
		if (sameCluster != other.sameCluster)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GroupNamePair [groupName1=" + groupName1.getSurfaceForm() + ", groupName2="
				+ groupName2.getSurfaceForm() + ", sameCluster=" + sameCluster + ", sameClusterProbability="
				+ probability + "]";
	}

	@Override
	public int compareTo(GroupNamePair o) {
		return Boolean.compare(this.sameCluster, o.sameCluster);
	}

}
