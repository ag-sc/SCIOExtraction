package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.nerl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.reader.ISpecificationsReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonInstanceIO;
import de.hterhors.semanticmr.json.converter.InstancesToJsonInstanceWrapper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.DataStructureLoader;

public class ExtractNERLADataFromSlotFillingData {

	public static void main(String[] args) throws IOException {
		new ExtractNERLADataFromSlotFillingData("investigation_method",
				DataStructureLoader.loadDataStructureReader("InvestigationMethod"));
	}

	public ExtractNERLADataFromSlotFillingData(String type, ISpecificationsReader specs) throws IOException {
		SystemScope.Builder.getScopeHandler().addScopeSpecification(specs).build();

		AbstractCorpusDistributor shuffleCorpusDistributor = new ShuffleCorpusDistributor.Builder()
				.setCorpusSizeFraction(1F).setTrainingProportion(80).setTestProportion(20).setSeed(100L).build();

		InstanceProvider instanceProvider = new InstanceProvider(
				new File("src/main/resources/slotfilling/" + type + "/corpus/instances/"), shuffleCorpusDistributor);
		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		for (Instance instance : instanceProvider.getInstances()) {
			List<Instance> newInstances = new ArrayList<>();
			if (!names.contains(instance.getName()))
				continue;
			System.out.println(instance.getName());
			Set<AbstractAnnotation> annotations = new HashSet<>();

			for (EntityTemplate annotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

				add(annotations, annotation);

			}
			projectAnnotationsIntoDocument(instance.getDocument(), annotations);

			newInstances.add(new Instance(instance.getOriginalContext(), instance.getDocument(),
					new Annotations(new ArrayList<>(annotations))));

			InstancesToJsonInstanceWrapper conv = new InstancesToJsonInstanceWrapper(newInstances);

			JsonInstanceIO io = new JsonInstanceIO(true);
			io.writeInstances(
					new File("src/main/resources/nerl/" + type + "/corpus/instances/" + instance.getName() + ".json"),
					conv.convertToWrapperInstances());

		}
	}

	public void add(Set<AbstractAnnotation> annotations, EntityTemplate annotation) {

		if (annotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
			DocumentLinkedAnnotation a = annotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation();
			annotations.add(a);
		}

//		EntityTemplateAnnotationFilter filter = annotation.filter().docLinkedAnnoation().nonEmpty().merge().multiSlots()
//				.singleSlots().build();
//		for (Entry<SlotType, Set<AbstractAnnotation>> a : filter.getMergedAnnotations().entrySet()) {
//
//			annotations.addAll(a.getValue());
//
//		}
//
//		EntityTemplateAnnotationFilter filter2 = annotation.filter().merge().entityTemplateAnnoation().multiSlots()
//				.nonEmpty().singleSlots().build();
//
//		for (Entry<SlotType, Set<AbstractAnnotation>> a : filter2.getMergedAnnotations().entrySet()) {
//			for (AbstractAnnotation abstractAnnotation : a.getValue()) {
//				add(annotations, abstractAnnotation.asInstanceOfEntityTemplate());
//			}
//		}

	}

	/**
	 * Projects existing annotations into the whole document.
	 * 
	 * @param document
	 * @param annotations
	 */
	private void projectAnnotationsIntoDocument(Document document, Set<AbstractAnnotation> annotations) {

		Set<AbstractAnnotation> additionalAnnotations = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Matcher m = Pattern
					.compile(Pattern.quote(abstractAnnotation.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()))
					.matcher(document.documentContent);

			while (m.find()) {
				try {

					additionalAnnotations.add(AnnotationBuilder.toAnnotation(document,
							abstractAnnotation.getEntityType().name, m.group(), m.start()));
				} catch (RuntimeException e) {
					System.out.println("Could not map annotation to tokens!");
				}
			}
		}
		System.out.println("Found additional annotation projections: " + additionalAnnotations.size());
		annotations.addAll(additionalAnnotations);

	}

}
