package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow;

import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.SlotType;

/**
 * This class represents a typed BOW. This is a set of BOW terms and a slot type
 * from which this BOW originates from.
 * 
 * @author hterhors
 *
 */
public class TypedBOW {

	/**
	 * The BOW set.
	 */
	final public Set<String> bow;

	/**
	 * The slot type from which this BOW originates from.
	 */
	final public SlotType slotType;

	public TypedBOW(Set<String> bow, SlotType slotType) {
		this.bow = bow;
		this.slotType = slotType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bow == null) ? 0 : bow.hashCode());
		result = prime * result + ((slotType == null) ? 0 : slotType.hashCode());
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
		TypedBOW other = (TypedBOW) obj;
		if (bow == null) {
			if (other.bow != null)
				return false;
		} else if (!bow.equals(other.bow))
			return false;
		if (slotType == null) {
			if (other.slotType != null)
				return false;
		} else if (!slotType.equals(other.slotType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TypedBOW [bow=" + bow + ", slotType=" + slotType + "]";
	}

}
