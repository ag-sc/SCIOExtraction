package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.investigation_method.nerla;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;

public class InvestigationMethodPattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("method", "test", "investigation"));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

	public InvestigationMethodPattern() {

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
