package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.nerla;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.AgeInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.WeightInterpreter;

public class ResultPattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<String> RESULT_STOP_WORDS = new HashSet<>(Arrays.asList("result"));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

	public ResultPattern(EntityType rootEntityType) {
		super(rootEntityType);
	}

	public Map<EntityType, Set<Pattern>> getHandMadePattern() {
		return pattern;

	}

	@Override
	public Set<String> getStopWords() {
		return RESULT_STOP_WORDS;
	}

	@Override
	public int getMinTokenlength() {
		return 2;
	}

}
