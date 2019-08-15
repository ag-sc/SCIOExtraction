package de.uni.bielefeld.sc.hterhors.psink.scio.nerla.corpus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.filter.EntityTemplateAnnotationFilter;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.exce.DocumentLinkedAnnotationMismatchException;
import de.hterhors.semanticmr.init.reader.ISpecificationsReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonInstanceIO;
import de.hterhors.semanticmr.json.converter.InstancesToJsonInstanceWrapper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.result.specifications.ResultSpecifications;

public class ExtractGroupNameNERLADataFromSlotFillingData {

	public static void main(String[] args) throws IOException {
		new ExtractGroupNameNERLADataFromSlotFillingData("experimental_group", ResultSpecifications.systemsScope);
	}

	public ExtractGroupNameNERLADataFromSlotFillingData(String type, ISpecificationsReader specs) throws IOException {
		SystemScope.Builder.getScopeHandler().addScopeSpecification(specs).build();

		AbstractCorpusDistributor shuffleCorpusDistributor = new ShuffleCorpusDistributor.Builder()
				.setCorpusSizeFraction(1F).setTrainingProportion(80).setTestProportion(20).setSeed(100L).build();

		InstanceProvider instanceProvider = new InstanceProvider(
				new File("src/main/resources/slotfilling/" + type + "/corpus/instances/"), shuffleCorpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {
			List<Instance> newInstances = new ArrayList<>();

			System.out.println(instance.getName());
			Set<AbstractAnnotation> annotations = new HashSet<>();

			for (EntityTemplate annotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

				add(annotations, annotation);

			}
			System.out.println("Found annotations: " + annotations.size());

			projectAnnotationsIntoDocument(instance.getDocument(), annotations);

			newInstances.add(new Instance(instance.getOriginalContext(), instance.getDocument(),
					new Annotations(new ArrayList<>(annotations))));

			InstancesToJsonInstanceWrapper conv = new InstancesToJsonInstanceWrapper(newInstances);

			JsonInstanceIO io = new JsonInstanceIO(true);
			io.writeInstances(new File(
					"src/main/resources/nerl/" + "group_name" + "/corpus/instances/" + instance.getName() + ".json"),
					conv.convertToWrapperInstances());

		}
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
							abstractAnnotation.getEntityType().entityName, m.group(), m.start()));
				} catch (RuntimeException e) {
					System.out.println("Could not map annotation to tokens!");
				}
			}
		}
		System.out.println("Found additional annotations: " + additionalAnnotations.size());
		annotations.addAll(additionalAnnotations);

	}

	public void add(Set<AbstractAnnotation> annotations, EntityTemplate annotation) {
		EntityTemplateAnnotationFilter filter = annotation.filter().docLinkedAnnoation().nonEmpty().merge().multiSlots()
				.singleSlots().build();

		if (annotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
			if (annotation.getRootAnnotation().getEntityType() == EntityType.get("DefinedExperimentalGroup")
					|| annotation.getRootAnnotation().getEntityType() == EntityType.get("AnalyzedExperimentalGroup")) {
				DocumentLinkedAnnotation a = annotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation();
				annotations.add(AnnotationBuilder.toAnnotation(a.document, "GroupName", a.getSurfaceForm(),
						a.getStartDocCharOffset()));
			}
		}

		for (Entry<SlotType, Set<AbstractAnnotation>> a : filter.getMergedAnnotations().entrySet()) {

			if (a.getKey().equals(SlotType.get("hasGroupName")))
				annotations.addAll(a.getValue());

		}

		EntityTemplateAnnotationFilter filter2 = annotation.filter().merge().entityTemplateAnnoation().multiSlots()
				.nonEmpty().singleSlots().build();

		for (Entry<SlotType, Set<AbstractAnnotation>> a : filter2.getMergedAnnotations().entrySet()) {
			for (AbstractAnnotation abstractAnnotation : a.getValue()) {
				add(annotations, abstractAnnotation.asInstanceOfEntityTemplate());
			}
		}

	}

}