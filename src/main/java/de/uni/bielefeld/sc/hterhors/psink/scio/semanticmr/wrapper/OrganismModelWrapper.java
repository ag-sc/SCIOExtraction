package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class OrganismModelWrapper extends SCIOWrapper {

	public OrganismModelWrapper(EntityTemplate organismModel) {
		super(organismModel);
	}

	public List<DocumentLinkedAnnotation> getAnnotations() {

		List<DocumentLinkedAnnotation> annotations = new ArrayList<>();

		annotations.add(getDocumentLinkedAnnotation(SCIOSlotTypes.hasAge));
		annotations.add(getDocumentLinkedAnnotation(SCIOSlotTypes.hasAgeCategory));
		annotations.add(getDocumentLinkedAnnotation(SCIOSlotTypes.hasOrganismSpecies));
		annotations.add(getDocumentLinkedAnnotation(SCIOSlotTypes.hasWeight));
		annotations.add(getDocumentLinkedAnnotation(SCIOSlotTypes.hasGender));

		return annotations.stream().filter(a -> a != null).collect(Collectors.toList());

	}

}
