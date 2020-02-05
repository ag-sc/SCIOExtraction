package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.investigation_method.nerla;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.investigation_method.specs.InvestigationMethodSpecs;

public class InvestigationMethodNERLAnnotator {

	public static void main(String[] args) throws IOException {

		RegularExpressionNerlAnnotator annotator = new RegularExpressionNerlAnnotator(SystemScope.Builder
				.getScopeHandler().addScopeSpecification(InvestigationMethodSpecs.systemsScope).build(),
				new InvestigationMethodPattern());

		File instanceDirectory = new File("src/main/resources/slotfilling/investigation_method/corpus/instances/");

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory);

		JsonNerlaIO io = new JsonNerlaIO(true);
		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		for (Instance instance : instanceProvider.getInstances()) {

			if (!names.contains(instance.getName()))
				continue;
			System.out.println(instance.getName());

			try {

				Map<EntityType, Set<DocumentLinkedAnnotation>> annotations = annotator.annotate(instance.getDocument());

				List<JsonEntityAnnotationWrapper> wrappedAnnotation = annotations.values().stream()
						.flatMap(v -> v.stream()).map(d -> new JsonEntityAnnotationWrapper(d))
						.collect(Collectors.toList());

				io.writeNerlas(new File("src/main/resources/slotfilling/investigation_method/corpus/nerla/"
						+ instance.getName() + ".nerla.json"), wrappedAnnotation);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
