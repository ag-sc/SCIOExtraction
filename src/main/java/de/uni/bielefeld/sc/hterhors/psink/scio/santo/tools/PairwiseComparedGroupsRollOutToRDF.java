package de.uni.bielefeld.sc.hterhors.psink.scio.santo.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.slots.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.santo.ResultSanto2Json;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;

/**
 * Rolls out the pairwise compared groups.
 * 
 * For that, the result is duplicated and the experimental groups are pairwise
 * filled in with no specific place of reference/ target group.
 * 
 * @author hterhors
 *
 */
public class PairwiseComparedGroupsRollOutToRDF {

	public static void main(String[] args) throws IOException {

		new PairwiseComparedGroupsRollOutToRDF();

	}

	private final File resultInstanceDirectory = new File("src/main/resources/slotfilling/result/corpus/instances/");
	private final File observationInstanceDirectory = new File(
			"src/main/resources/slotfilling/observation/corpus/instances/");

	public PairwiseComparedGroupsRollOutToRDF() throws IOException {

		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.build();
		SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build();

		InstanceProvider.maxNumberOfAnnotations = 100;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

//		InstanceProvider instanceProviderO = new InstanceProvider(observationInstanceDirectory, corpusDistributor);

		InstanceProvider instanceProvider = new InstanceProvider(resultInstanceDirectory, corpusDistributor);

		int docID = 0;
		for (Instance instance : instanceProvider.getInstances()) {
			System.out.println(instance.getName());
			PrintStream psRDF = new PrintStream(
					"unroll/export_" + ResultSanto2Json.exportDate + "/" + instance.getName() + "_Jessica.n-triples");
			PrintStream psAnnotation = new PrintStream(
					"unroll/export_" + ResultSanto2Json.exportDate + "/" + instance.getName() + "_Jessica.annodb");
			PrintStream psDocument = new PrintStream(
					"unroll/export_" + ResultSanto2Json.exportDate + "/" + instance.getName() + "_export.csv");

			List<AbstractAnnotation> annotations = unrollAnnotations(instance);

			/*
			 * Add Observations cause they are not direct connected to results.
			 */
//			annotations.addAll(collectObservations(instanceProviderO, instance));

			SantoAnnotations collectRDF = collectDataAsRDF(annotations);

			List<String> c = new ArrayList<>(collectRDF.getRdf().stream().collect(Collectors.toList()));
			List<String> c2 = new ArrayList<>(collectRDF.getAnnodb().stream().collect(Collectors.toList()));
			Collections.sort(c);
			Collections.sort(c2);
			c.forEach(psRDF::println);
			c2.forEach(psAnnotation::println);
			psAnnotation.close();
			psRDF.close();

			psDocument.print(toCSV(docID, instance.getDocument().tokenList));
			psDocument.close();
			docID++;
		}

	}

	private Collection<? extends AbstractAnnotation> collectObservations(InstanceProvider instanceProviderO,
			Instance instance) {
		Optional<Instance> o = instanceProviderO.getInstances().stream()
				.filter(s -> s.getName().equals(instance.getName())).findFirst();
		if (o.isPresent())
			return o.get().getGoldAnnotations().getAbstractAnnotations();
		else
			return Collections.emptySet();

	}

	private String toCSV(int docID, List<DocumentToken> tokenList) {

		StringBuilder sb = new StringBuilder(
				"# DocID, SentenceNr, SenTokenPos, DocTokenPos, SenCharOnset(incl), SenCharOffset(excl), DocCharOnset(incl), DocCharOffset(excl), Text\n");

		for (DocumentToken documentToken : tokenList) {
			sb.append(docID).append(", ").append(documentToken.getSentenceIndex() + 1).append(", ")
					.append(documentToken.getSenTokenIndex() + 1).append(", ")
					.append(documentToken.getDocTokenIndex() + 1).append(", ").append(documentToken.getSenCharOffset())
					.append(", ").append(documentToken.getSenCharOffset() + documentToken.getLength()).append(", ")
					.append(documentToken.getDocCharOffset()).append(", ")
					.append(documentToken.getDocCharOffset() + documentToken.getLength()).append(", ").append("\"")
					.append(documentToken.getText().replaceAll("\"", "\\\\\"")).append("\"").append("\n");
		}

		return sb.toString();
	}

	private SantoAnnotations collectDataAsRDF(List<AbstractAnnotation> annotations) {
		SantoAnnotations collectRDF = new SantoAnnotations(new HashSet<>(), new HashMap<>());
		for (AbstractAnnotation annotation : annotations) {

			AnnotationsToSantoAnnotations.collectRDF(annotation, collectRDF, "http://scio/data/",
					"http://psink.de/scio/");

		}
		return collectRDF;
	}

	public List<AbstractAnnotation> unrollAnnotations(Instance instance) {

		List<AbstractAnnotation> annotations = new ArrayList<>();
		for (AbstractAnnotation resultAnnotation : instance.getGoldAnnotations().getAnnotations()) {

			List<AbstractAnnotation> defGroups = new ArrayList<>(resultAnnotation.asInstanceOfEntityTemplate()
					.getMultiFillerSlot(SlotType.get("hasPairwisedCompareGroups")).getSlotFiller());

			if (defGroups.isEmpty()) {
				annotations.add(resultAnnotation);
			} else {
				for (int i = 0; i < defGroups.size() - 1; i++) {
					AbstractAnnotation expGroup1 = defGroups.get(i);
					for (int j = i + 1; j < defGroups.size(); j++) {

						AbstractAnnotation expGroup2 = defGroups.get(j);

						AbstractAnnotation deepCopyOfResult = resultAnnotation.deepCopy();

						MultiFillerSlot mfs = deepCopyOfResult.asInstanceOfEntityTemplate()
								.getMultiFillerSlot(SlotType.get("hasPairwisedCompareGroups"));

						for (AbstractAnnotation slotFiller : resultAnnotation.asInstanceOfEntityTemplate()
								.getMultiFillerSlot(SlotType.get("hasPairwisedCompareGroups")).getSlotFiller()) {
							mfs.removeSlotFiller(slotFiller);
						}

						deepCopyOfResult.asInstanceOfEntityTemplate()
								.setSingleSlotFiller(SlotType.get("hasTargetGroup"), expGroup1);
						deepCopyOfResult.asInstanceOfEntityTemplate()
								.setSingleSlotFiller(SlotType.get("hasReferenceGroup"), expGroup2);

						annotations.add(deepCopyOfResult);

					}
				}
			}
		}

		return annotations;
	}
}
