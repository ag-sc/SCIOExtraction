package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.treatment.nerla;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.nerla.annotation.RegularExpressionNerlAnnotator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.treatment.specs.TreatmentSpecs;

public class TreatmentNERLAnnotator {

	public static void main(String[] args) {

		RegularExpressionNerlAnnotator annotator = new RegularExpressionNerlAnnotator(SystemScope.Builder
				.getScopeHandler().addScopeSpecification(TreatmentSpecs.systemsScopeReader).build(),
				new TreatmentPattern());

		File instanceDirectory = new File("src/main/resources/slotfilling/treatment/corpus/instances/");

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory);

		JsonNerlaIO io = new JsonNerlaIO(true);

		for (Instance instance : instanceProvider.getInstances()) {

			System.out.println(instance.getName());

			try {

				Map<EntityType, Set<DocumentLinkedAnnotation>> annotations = annotator.annotate(instance.getDocument());

				List<JsonEntityAnnotationWrapper> wrappedAnnotation = annotations.values().stream()
						.flatMap(v -> v.stream()).map(d -> new JsonEntityAnnotationWrapper(d))
						.collect(Collectors.toList());

				io.writeNerlas(new File("src/main/resources/slotfilling/treatment/corpus/nerla/"
						+ instance.getName() + ".nerla.json"), wrappedAnnotation);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
