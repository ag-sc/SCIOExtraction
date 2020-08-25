package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.AbstractEvaluator;

public class AnalyzeComplexity {
	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public static void analyze(Set<SlotType> slotTypesToConsider, List<Instance> instances,
			AbstractEvaluator abstractEvaluator) {
		Map<SlotType, Set<AbstractAnnotation>> countUnique = new HashMap<>();
		Map<SlotType, Integer> count = new HashMap<>();

		Map<String, Map<Integer, Integer>> histograms = new HashMap<>();
int countIndividuals=0;
int countInstances=0;
		for (Instance instance : instances) {

			for (AbstractAnnotation goldAnn : instance.getGoldAnnotations().getAbstractAnnotations()) {
				for (SlotType slotType : slotTypesToConsider) {

					if (slotType.isSingleValueSlot()) {

						if (!goldAnn.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
							continue;

						if (!goldAnn.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType).containsSlotFiller())
							continue;

						countUnique.putIfAbsent(slotType, new HashSet<>());

						if (!contains(countUnique.get(slotType),
								goldAnn.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType).getSlotFiller(),
								abstractEvaluator))
							countUnique.get(slotType).add(
									goldAnn.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType).getSlotFiller());

						count.put(slotType, count.getOrDefault(slotType, 0) + 1);

					} else {
						if (!goldAnn.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
							continue;

						if (!goldAnn.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType).containsSlotFiller())
							continue;

						histograms.putIfAbsent(slotType.name, new HashMap<>());
						histograms.get(slotType.name).put(
								goldAnn.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType).size(),
								histograms.get(slotType.name).getOrDefault(
										goldAnn.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType).size(), 0)
										+ 1);

						countUnique.putIfAbsent(slotType, new HashSet<>());

						for (AbstractAnnotation ann : goldAnn.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType)
								.getSlotFiller()) {

							if (!contains(countUnique.get(slotType), ann, abstractEvaluator))
								countUnique.get(slotType).add(ann);

							count.put(slotType, count.getOrDefault(slotType, 0) + 1);

						}
					}
				}
			}
			countInstances++;
			System.out
					.println(instance.getName() + "\t" + instance.getGoldAnnotations().getAbstractAnnotations().size());
		
			countIndividuals+= instance.getGoldAnnotations().getAbstractAnnotations().size();
			
			histograms.putIfAbsent("type", new HashMap<>());
			histograms.get("type").put(instance.getGoldAnnotations().getAbstractAnnotations().size(), histograms
					.get("type").getOrDefault(instance.getGoldAnnotations().getAbstractAnnotations().size(), 0) + 1);

		}
		for (Entry<String, Map<Integer, Integer>> histogram : histograms.entrySet()) {

			for (Entry<Integer, Integer> slotType : histogram.getValue().entrySet()) {
				System.out.println(histogram.getKey() + "\t" + slotType.getKey() + "\t" + slotType.getValue());
			}
		}
		for (SlotType slotType : slotTypesToConsider) {

			System.out
					.println(slotType.name + "\tentity\t" + slotType.getSlotFillerEntityTypes().size() + "\t"
							+ countUnique.get(slotType).size() + "\t" + count.get(slotType) + "\t"
							+ resultFormatter.format(
									100 * ((double) countUnique.get(slotType).size() / (double) count.get(slotType)))
							+ "%");

		}
		System.out.println("count individuals:" + countIndividuals);
		System.out.println("count instances:" + countInstances);
		
		System.exit(1);
	}

	public static boolean contains(Set<AbstractAnnotation> set, AbstractAnnotation slotFiller,
			AbstractEvaluator abstractEvaluator) {

		for (AbstractAnnotation abstractAnnotation : set) {
			if (abstractAnnotation.evaluateEquals(abstractEvaluator, slotFiller)) {
				return true;
			}
		}
		return false;
	}

}
