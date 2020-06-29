package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.Word2VecClusterTemplate.W2VScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.helper.bow.BOWExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.helper.bow.TypedBOW;

public class Word2VecClusterTemplate extends AbstractFeatureTemplate<W2VScope> {
//	Set<String> terms = new HashSet<>();
//
//	terms.addAll(trainingInstances.stream().flatMap(t -> t.getDocument().tokenList.stream()).map(t -> t.getText())
//			.collect(Collectors.toSet()));
//	terms.addAll(devInstances.stream().flatMap(t -> t.getDocument().tokenList.stream()).map(t -> t.getText())
//			.collect(Collectors.toSet()));
//	terms.addAll(testInstances.stream().flatMap(t -> t.getDocument().tokenList.stream()).map(t -> t.getText())
//			.collect(Collectors.toSet()));
//
//	wordVecReduce(terms);
//	private void wordVecReduce(Set<String> terms) throws FileNotFoundException {
//		PrintStream ps = new PrintStream(new File("wordvector/small_kmeans++_200_ranking.vec"));
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(new File("wordvector/kmeans++_200_ranking.vec")));
//
//			String line = null;
//
//			while ((line = br.readLine()) != null) {
//
//				final String data[] = line.split(" ");
//
//				if (terms.contains(data[0])) {
//					ps.println(line);
//				}
//			}
//
//			br.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		ps.close();
//
//		System.exit(1);
//	}

	private static Logger log = LogManager.getFormatterLogger(Word2VecClusterTemplate.class.getName());
	private static final Set<String> SPECIAL_KEEPWORDS = new HashSet<>(
			Arrays.asList("media", "single", "alone", "only", "control", "sham", "low", "high"));

	private static final Set<String> ADDITIONAL_STOPWORDS = new HashSet<>(Arrays.asList("transplantation", "untreated",
			"is", "untrained", "blank", "transplanted", "transection", "grafts", "normal", "injection", "injections",
			"cultured", "cords", "uninfected", "injected", "additional", "ca", "observed", "grafted", "graft", "cells",
			"are", "effects", "gray", "cord", "spinal", "identifi", "cation", "n", "treated", "treatment", "",
			"received", "the", "injured", "all", "lesioned", "fi", "rst", "first", "second", "third", "fourth", "group",
			"animals", "rats", "in", "same", "individual", "groups", "were"));
	private static Set<String> ALL_STOPWORDS;

	static {
		try {
			ALL_STOPWORDS = new HashSet<>(Files.readAllLines(new File("src/main/resources/top1000.csv").toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		ALL_STOPWORDS.addAll(ADDITIONAL_STOPWORDS);
		ALL_STOPWORDS.removeAll(SPECIAL_KEEPWORDS);
	}
	final private Map<String, List<Integer>> clusters = new HashMap<>();
	final private Map<String, List<Double>> distances = new HashMap<>();

	@Override
	public void initalize(Object[] parameter) {
		try {
			BufferedReader br = new BufferedReader(new FileReader((File) parameter[0]));

			String line = null;

			while ((line = br.readLine()) != null) {

				final String data[] = line.split(" ");

				clusters.put(data[0],
						Arrays.stream(data).skip(1).map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList()));

			}

			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
//		try {
//			BufferedReader br = new BufferedReader(new FileReader((File) parameter[1]));
//
//			String line = null;
//
//			while ((line = br.readLine()) != null) {
//
//				final String data[] = line.split(" ");
//
//				distances.put(data[0], Arrays.stream(data).skip(1).map(s -> Double.parseDouble(s.trim()))
//						.collect(Collectors.toList()));
//
//			}
//
//			br.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	static class W2VScope extends AbstractFactorScope {

		public final Set<String> expGroupBOW;

		public final Set<TypedBOW> propertyBOW;
		public final SlotType slotTypeContext;

		public W2VScope(AbstractFeatureTemplate<?> template, SlotType slotType, Set<String> expGroupBOW,
				Set<TypedBOW> popertyBOW) {
			super(template);
			this.expGroupBOW = expGroupBOW;
			this.propertyBOW = popertyBOW;
			this.slotTypeContext = slotType;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((expGroupBOW == null) ? 0 : expGroupBOW.hashCode());
			result = prime * result + ((propertyBOW == null) ? 0 : propertyBOW.hashCode());
			result = prime * result + ((slotTypeContext == null) ? 0 : slotTypeContext.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			W2VScope other = (W2VScope) obj;
			if (expGroupBOW == null) {
				if (other.expGroupBOW != null)
					return false;
			} else if (!expGroupBOW.equals(other.expGroupBOW))
				return false;
			if (propertyBOW == null) {
				if (other.propertyBOW != null)
					return false;
			} else if (!propertyBOW.equals(other.propertyBOW))
				return false;
			if (slotTypeContext == null) {
				if (other.slotTypeContext != null)
					return false;
			} else if (!slotTypeContext.equals(other.slotTypeContext))
				return false;
			return true;
		}

	}

	@Override
	public List<W2VScope> generateFactorScopes(State state) {
		List<W2VScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			addSFSFactor(factors, experimentalGroup, SCIOSlotTypes.hasOrganismModel);
			addSFSFactor(factors, experimentalGroup, SCIOSlotTypes.hasInjuryModel);
			addMFSFactor(factors, experimentalGroup, SCIOSlotTypes.hasTreatmentType);

		}

		return factors;
	}

	private void addMFSFactor(List<W2VScope> factors, EntityTemplate experimentalGroup, SlotType slotType) {

		if (slotType.isExcluded())
			return;

		final MultiFillerSlot mfs = experimentalGroup.getMultiFillerSlot(slotType);

		if (!mfs.containsSlotFiller())
			return;

		for (AbstractAnnotation slotFillerAnnotation : mfs.getSlotFiller()) {

			final EntityTemplate property = slotFillerAnnotation.asInstanceOfEntityTemplate();

			final Set<String> expGroupBOW = BOWExtractor.getExpGroupPlusNameBOW(experimentalGroup);

			final Set<TypedBOW> propertyBOW = BOWExtractor.extractTypedBOW(property);

			factors.add(new W2VScope(this, slotType, expGroupBOW, propertyBOW));
		}
	}

	private void addSFSFactor(List<W2VScope> factors, EntityTemplate experimentalGroup, SlotType slotType) {

		if (slotType.isExcluded())
			return;

		final SingleFillerSlot sfs = experimentalGroup.getSingleFillerSlot(slotType);

		if (!sfs.containsSlotFiller())
			return;

		final EntityTemplate property = sfs.getSlotFiller().asInstanceOfEntityTemplate();

		final Set<String> expGroupBOW = BOWExtractor.getExpGroupPlusNameBOW(experimentalGroup);

		final Set<TypedBOW> propertyBOW = BOWExtractor.extractTypedBOW(property);

		factors.add(new W2VScope(this, slotType, expGroupBOW, propertyBOW));
	}

	@Override
	public void generateFeatureVector(Factor<W2VScope> factor) {

		for (String expBOWTerm : factor.getFactorScope().expGroupBOW) {

			if (ALL_STOPWORDS.contains(normalize(expBOWTerm)))
				continue;

			List<Integer> clustersI = clusters.getOrDefault(expBOWTerm, Arrays.asList(-1));

			if (clustersI.isEmpty())
				continue;

			for (TypedBOW typedBOW : factor.getFactorScope().propertyBOW) {
				for (String typedBOWTerm : typedBOW.bow) {

					if (ALL_STOPWORDS.contains(normalize(typedBOWTerm)))
						continue;

					List<Integer> clustersJ = clusters.getOrDefault(typedBOWTerm, Arrays.asList(-1));

					if (clustersJ.isEmpty())
						continue;

					for (int i = 0; i < 1 && i < clustersI.size(); i++) {

						Integer clusterI = clustersI.get(i);

						for (int j = 0; j < 1 && j < clustersJ.size(); j++) {

							Integer clusterJ = clustersJ.get(j);

//							factor.getFeatureVector().set("Context: " + factor.getFactorScope().slotTypeContext.slotName
//									+ ", " + clusterI + "\t" + clusterJ , true);
//							factor.getFeatureVector()
//									.set("Context: " + factor.getFactorScope().slotTypeContext.slotName + ", "
//											+ (clusterI == -1 ? expBOWTerm : clusterI) + "\t"
//											+ (clusterJ == -1 ? typedBOWTerm : clusterJ) + " : (" + i + "," + j + ")",
//											true);
//							factor.getFeatureVector()
//									.set("Context: " + factor.getFactorScope().slotTypeContext.slotName + ", " + "."
//											+ getOrigin(factor, typedBOW) + ","
//											+ (clusterI == -1 ? expBOWTerm : clusterI) + "\t"
//											+ (clusterJ == -1 ? typedBOWTerm : clusterJ) + " : (" + i + "," + j + ")",
//											true);
							factor.getFeatureVector()
									.set("Context: " + factor.getFactorScope().slotTypeContext.name + "."
											+ getOrigin(factor, typedBOW) + ", " + clusterI + "\t" + clusterJ + " : ("
											+ i + "," + j + ")", true);

						}
					}
				}
			}
		}
	}

	private String normalize(String expBOWTerm) {
		return toSingular(toLowerIfNotUpper(expBOWTerm));
	}

	private String toSingular(String finding) {
		if (finding.endsWith("s"))
			return finding.substring(0, finding.length() - 1);
		if (finding.endsWith("ies"))
			return finding.substring(0, finding.length() - 3) + "y";
		return finding;
	}

	/**
	 * Converts a string to lowercase if it is not in uppercase
	 * 
	 * @param s
	 * @return
	 */
	private String toLowerIfNotUpper(String s) {
		if (s.matches("[a-z]?[A-Z\\d\\W]+s?"))
			return s;

		return s.toLowerCase();
	}

	private String getOrigin(Factor<W2VScope> factor, TypedBOW typedBOW) {
		return typedBOW.slotType != null ? typedBOW.slotType.name : factor.getFactorScope().slotTypeContext.name;
	}

}
