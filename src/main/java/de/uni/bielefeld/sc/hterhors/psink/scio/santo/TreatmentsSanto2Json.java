package de.uni.bielefeld.sc.hterhors.psink.scio.santo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.EInstanceContext;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.santo.converter.Santo2JsonConverter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.specs.TreatmentSpecs;

public class TreatmentsSanto2Json {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	final static private String scioNameSpace = "http://psink.de/scio";
	final static private String resourceNameSpace = "http://scio/data";

	public static void main(String[] args) throws IOException {

		SystemScope scope = SystemScope.Builder.getScopeHandler().addScopeSpecification(TreatmentSpecs.systemsScope)
				.build();

		final String dir = "src/main/resources/current_raw_data/";
		Set<String> docs = new HashSet<>(
				Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath()));
		List<String> fileNames = Arrays.stream(new File(dir).listFiles()).filter(f -> f.getName().endsWith(".csv"))
				.map(f -> f.getName().substring(0, f.getName().length() - 11)).collect(Collectors.toList());
		Collections.sort(fileNames);

		Random random = new Random(10000L);

		Set<SlotType> slotTypes = new HashSet<>();
		slotTypes.add(SlotType.get("hasDeliveryMethod"));
		slotTypes.add(SCIOSlotTypes.hasCompound);
		slotTypes.add(SlotType.get("hasDirection"));
		slotTypes.add(SlotType.get("hasLocation"));
		slotTypes.add(SlotType.get("hasDosage"));

		for (String name : fileNames) {
			try {
				if (!docs.contains(name))
					continue;

				log.info(name + " convert...");
				Santo2JsonConverter converter = new Santo2JsonConverter(scope, name,
						new File("src/main/resources/current_raw_data/" + name + "_export.csv"),
						new File("src/main/resources/current_raw_data/" + name + "_Jessica.annodb"),
						new File("src/main/resources/current_raw_data/" + name + "_Jessica.n-triples"), scioNameSpace,
						resourceNameSpace);

				converter.addIgnoreProperty("<http://www.w3.org/2000/01/rdf-schema#comment>");
				converter.addIgnoreProperty("<http://www.w3.org/2000/01/rdf-schema#label>");

				double rand = random.nextDouble();

				EInstanceContext context = rand < 0.6 ? EInstanceContext.TRAIN
						: rand < 0.8 ? EInstanceContext.DEVELOPMENT : EInstanceContext.TEST;

				log.info("context = " + context);

				converter.convert(context, new File(
						"src/main/resources/slotfilling/treatment/corpus/instances/" + name + "_Treatment.json"),
						EntityType.get("Treatment"), true, true, true);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
