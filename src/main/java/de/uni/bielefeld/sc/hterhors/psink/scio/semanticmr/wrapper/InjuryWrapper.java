package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;

public class InjuryWrapper extends SCIOWrapper {

	public InjuryWrapper(EntityTemplate injury) {
		super(injury);
	}

	public List<DocumentLinkedAnnotation> getAnnotations() {

		List<DocumentLinkedAnnotation> annotations = new ArrayList<>();

		collectDLA(annotations);

		return annotations.stream().filter(a -> a != null).collect(Collectors.toList());

	}

}
