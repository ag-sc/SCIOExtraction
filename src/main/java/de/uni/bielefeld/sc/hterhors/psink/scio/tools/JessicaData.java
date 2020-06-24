package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.io.File;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class JessicaData {

	public static void main(String[] args) {
		new JessicaData();
	}

	private final File instanceDirectory;
	public JessicaData() {
		SystemScope scope = SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Treatment")).build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();
	
		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.treatment);

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {

			for (AbstractAnnotation treatment : instance.getGoldAnnotations().getAnnotations()) {

				if (treatment.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasLocation)
						.containsSlotFiller()) {
					System.out.println(instance.getName());
				}
			}

		}

	}
}
