package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;

public class InjuryDeviceWrapper extends SCIOWrapper {

	public InjuryDeviceWrapper(EntityTemplate injuryDevice) {
		super(injuryDevice);
	}

	public List<DocumentLinkedAnnotation> getAnnotations() {

		List<DocumentLinkedAnnotation> annotations = new ArrayList<>();

		collectDLA(annotations);

		return annotations.stream().filter(a -> a != null).collect(Collectors.toList());

//		List<DocumentLinkedAnnotation> annotations = new ArrayList<>();
//
//		annotations.add(getDocumentLinkedAnnotation(SCIOSlotTypes.hasUpperVertebrae));
//		annotations.add(getDocumentLinkedAnnotation(SCIOSlotTypes.hasLowerVertebrae));
//
//		return annotations.stream().filter(a -> a != null).collect(Collectors.toList());

	}

}
