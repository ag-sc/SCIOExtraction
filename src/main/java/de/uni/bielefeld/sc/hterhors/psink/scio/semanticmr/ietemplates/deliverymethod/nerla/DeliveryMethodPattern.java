package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.nerla;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;

public class DeliveryMethodPattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("method","spinal",""));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

//	final protected static String numbers = "([0-9]{1,2}|(" + PRE_BOUNDS
//			+ "(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh|twelfth|thirteenth|forteenth|fifteenth)))";
//
//	private static final Pattern VERTBERAL_AREA_PATTERN1 = Pattern
//			.compile("(Th?|LU?|Ce?)-?" + numbers + "\\W?(Th?|LU?|Ce?)?-?" + numbers);
//	private static final Pattern VERTBERAL_AREA_PATTERN2 = Pattern
//			.compile("(thoracic|lumbar|cervical) levels " + numbers + " and " + numbers);
//	private static final Pattern VERTBERAL_AREA_PATTERN3 = Pattern
//			.compile(numbers + "\\Wand\\W" + numbers + "\\W(thoracic|lumbar|cervical)");
//	private static final Pattern VERTBERAL_AREA_PATTERN4 = Pattern
//			.compile("(Th?|LU?|Ce?)-?" + numbers + "\\W?(Th?|LU?|Ce?)-?" + numbers);
//	private static final Pattern VERTBERAL_AREA_PATTERN5 = Pattern
//			.compile("(Th?|LU?|Ce?)-?" + numbers + "\\Wand\\W(Th?|LU?|Ce?)-?" + numbers + "(\\Wvertebrae)?");
//
//	private static final Set<Pattern> VERTEBRAL_AREA_REG_EXP = new HashSet<>(Arrays.asList(VERTBERAL_AREA_PATTERN1,
//			VERTBERAL_AREA_PATTERN2, VERTBERAL_AREA_PATTERN3, VERTBERAL_AREA_PATTERN4, VERTBERAL_AREA_PATTERN5));

	public DeliveryMethodPattern() {
//		pattern.put(EntityType.get("VertebralArea"), VERTEBRAL_AREA_REG_EXP);
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
