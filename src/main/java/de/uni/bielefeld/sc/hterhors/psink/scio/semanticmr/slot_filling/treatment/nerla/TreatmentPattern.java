package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.nerla;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.DosageInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.TemperatureInterpreter;

public class TreatmentPattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("treatment"));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

	public TreatmentPattern(EntityType rootEntityType) {
		super(rootEntityType);
		pattern.put(EntityType.get("DosageExtracorporal"), new HashSet<>(Arrays.asList(DosageInterpreter.PATTERN)));
		pattern.put(EntityType.get("Temperature"), new HashSet<>(Arrays.asList(TemperatureInterpreter.PATTERN)));

	}

	public Map<EntityType, Set<Pattern>> getHandMadePattern() {
		return pattern;

	}

	@Override
	public Set<String> getStopWords() {
		return STOP_WORDS;
	}

	@Override
	public int getMinTokenlength() {
		return 2;
	}

}
