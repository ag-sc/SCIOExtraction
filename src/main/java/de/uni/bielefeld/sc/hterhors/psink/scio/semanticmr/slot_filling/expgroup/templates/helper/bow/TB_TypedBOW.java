package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.helper.bow;

import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;

/**
 * This class represents a typed BOW. This is a set of BOW terms and a slot type
 * from which this BOW originates from.
 * 
 * @author hterhors
 *
 */
public class TB_TypedBOW {

	public final Instance instance;

	/**
	 * The BOW set.
	 */
	final public Set<String> bow;

	/**
	 * The slot type from which this BOW originates from.
	 */
	final public SlotType slotType;

	public TB_TypedBOW(Instance instance, Set<String> bow, SlotType slotType) {
		this.bow = bow;
		this.instance = instance;
		this.slotType = slotType;
	}

	/**
	 * DO NOT USE bow AS KEY FOR HASHMAP ITS JUST BASED ON INSTANCE.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((instance == null) ? 0 : instance.hashCode());
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
		TB_TypedBOW other = (TB_TypedBOW) obj;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
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
