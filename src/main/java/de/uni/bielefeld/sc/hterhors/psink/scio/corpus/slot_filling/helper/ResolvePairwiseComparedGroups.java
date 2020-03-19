package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.slot_filling.helper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;

/**
 * Rolls out the pairwise compared groups.
 * 
 * For that, the result is duplicated and the experimental groups are pairwise
 * filled in with no specific place of reference/ target group.
 * 
 * @author hterhors
 *
 */
public class ResolvePairwiseComparedGroups {

	public static class SantoAnnotations {

		private static int annodbid = 0;

		final private Set<String> rdf;
		final private Map<String, Set<String>> annodb;

		public SantoAnnotations(Set<String> rdf, Map<String, Set<String>> annodb) {
			this.rdf = rdf;
			this.annodb = annodb;
		}

		public Set<String> getRdf() {
			return rdf;
		}

		public Set<String> getAnnodb() {
			return annodb.entrySet().stream()
					.map(e -> new String(annodbid++ + ", " + e.getKey() + "\"" + toWSSepList(e.getValue()) + "\""))
					.collect(Collectors.toSet());
		}

		public void addInstanceToAnnotation(final String annotation, String instance) {
			if (!annodb.containsKey(annotation))
				annodb.put(annotation, new HashSet<>());
			annodb.get(annotation).add(instance);
		}

		private String toWSSepList(Set<String> value) {
			String x = "";
			for (String string : value) {
				x += string + " ";
			}

			return x.trim().replaceAll("\"", "\\\\\"");
		}
	}

	public ResolvePairwiseComparedGroups(File resultInstanceDir, File observationInstanceDir, File tmpUnrolledDir)
			throws IOException {

		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.build();

		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		InstanceProvider instanceProviderO = new InstanceProvider(observationInstanceDir, corpusDistributor);

		InstanceProvider instanceProvider = new InstanceProvider(resultInstanceDir, corpusDistributor);

		int docID = 0;
		for (Instance instance : instanceProvider.getInstances()) {

			PrintStream psRDF = new PrintStream(
					new File(tmpUnrolledDir, "/" + instance.getName() + "_Jessica.n-triples"));
			PrintStream psAnnotation = new PrintStream(
					new File(tmpUnrolledDir, "/" + instance.getName() + "_Jessica.annodb"));
			PrintStream psDocument = new PrintStream(
					new File(tmpUnrolledDir, "/" + instance.getName() + "_export.csv"));

			List<AbstractAnnotation> annotations = unrollAnnotations(instance);

			/*
			 * Add Observations cause they are not directly connected to results.
			 */
			annotations.addAll(collectObservations(instanceProviderO, instance));

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

			collectRDF(annotation, collectRDF, "http://scio/data/", "http://psink.de/scio/");

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

						deepCopyOfResult.asInstanceOfEntityTemplate()
								.clearSlot(SlotType.get("hasPairwisedCompareGroups"));

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

	static private void collectRDF(AbstractAnnotation annotation, SantoAnnotations collectData, String dataNamespace,
			String classNamespace) {
		toRDFrec(collectData, annotation, annotation, null, dataNamespace, classNamespace);
	}

	static private SantoAnnotations toRDFrec(SantoAnnotations collectData, AbstractAnnotation parent,
			AbstractAnnotation child, SlotType origin, String dataNamespace, String classNamespace) {
		String rdf;
		if (child.isInstanceOfEntityTemplate()) {

			String rdfType = new StringBuilder("<").append(dataNamespace).append(child.getEntityType().name).append("_")
					.append(getID(child.asInstanceOfEntityTemplate())).append("> <")
					.append("http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <").append(classNamespace)
					.append(child.getEntityType().name).append("> .").toString();

			if (child.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
				DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfEntityTemplate().getRootAnnotation()
						.asInstanceOfDocumentLinkedAnnotation();
				collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdfType);
			}
			collectData.getRdf().add(rdfType);

			if (origin != null) {
				rdf = new StringBuilder("<").append(dataNamespace).append(parent.getEntityType().name).append("_")
						.append(getID(parent)).append("> <").append(classNamespace).append(origin.name).append("> <")
						.append(dataNamespace).append(child.getEntityType().name).append("_")
						.append(getID(child.asInstanceOfEntityTemplate())).append("> .").toString();
				if (child.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
					DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfEntityTemplate().getRootAnnotation()
							.asInstanceOfDocumentLinkedAnnotation();
					collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdf);
				}
				collectData.getRdf().add(rdf);
			}
		} else {
			if (child.getEntityType().isLiteral) {
				rdf = new StringBuilder("<").append(dataNamespace).append(parent.getEntityType().name).append("_")
						.append(getID(parent)).append("> <").append(classNamespace).append(origin.name).append("> \"")
						.append(child.asInstanceOfLiteralAnnotation().getSurfaceForm()).append("\" .").toString();
				if (child.isInstanceOfDocumentLinkedAnnotation()) {
					DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfDocumentLinkedAnnotation();
					collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdf);
				}
				collectData.getRdf().add(rdf);

			} else {
				rdf = new StringBuilder("<").append(dataNamespace).append(parent.getEntityType().name).append("_")
						.append(getID(parent)).append("> <").append(classNamespace).append(origin.name).append("> <")
						.append(classNamespace).append(child.getEntityType().name).append("> .").toString();
				if (child.isInstanceOfDocumentLinkedAnnotation()) {
					DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfDocumentLinkedAnnotation();
					collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdf);
				}
				collectData.getRdf().add(rdf);

			}

		}

		if (child.isInstanceOfEntityTemplate()) {

			final Map<SlotType, Set<AbstractAnnotation>> slots = child.asInstanceOfEntityTemplate().filter()
					.docLinkedAnnoation().entityTemplateAnnoation().entityTypeAnnoation().literalAnnoation().nonEmpty()
					.multiSlots().singleSlots().merge().build().getMergedAnnotations();

			for (Entry<SlotType, Set<AbstractAnnotation>> slotFillerSlot : slots.entrySet()) {
				for (AbstractAnnotation childAnnotation : slotFillerSlot.getValue()) {
					toRDFrec(collectData, child, childAnnotation, slotFillerSlot.getKey(), dataNamespace,
							classNamespace);
				}
			}
		}
		return collectData;
	}

	static private String toAnnotation(DocumentLinkedAnnotation docLinkedAnn) {
		String annotation = new StringBuilder(docLinkedAnn.getEntityType().name).append(", ")
				.append(docLinkedAnn.getStartDocCharOffset()).append(", ").append(docLinkedAnn.getEndDocCharOffset())
				.append(", \"").append(docLinkedAnn.getSurfaceForm()).append("\", \"\", ").toString();
		return annotation;
	}

	private static Map<AbstractAnnotation, Integer> rdfIDMap = new HashMap<AbstractAnnotation, Integer>();

	private static int getID(AbstractAnnotation from) {
		Integer id;
		if ((id = rdfIDMap.get(from)) == null) {
			id = new Integer(rdfIDMap.size());
			rdfIDMap.put(from, id);
		}
		return id.intValue();
	}
}
