package de.uni.bielefeld.sc.hterhors.psink.scio.santo.tools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.EInstanceContext;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.init.reader.csv.CSVScopeReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.santo.converter.Santo2JsonConverter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrgModelSlotFilling;

public class EntityExtractor {

	private static final File entities = new File("src/main/resources/slotfilling/result/specifications/entities.csv");
	private static final File slots = new File("src/main/resources/slotfilling/result/specifications/slots.csv");
	private static final File structures = new File(
			"src/main/resources/slotfilling/result/specifications/structures.csv");
	private static final File hierarchies = new File(
			"src/main/resources/slotfilling/result/specifications/hierarchies.csv");

	public final static CSVScopeReader systemsScope = new CSVScopeReader(entities, hierarchies, slots, structures);

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(systemsScope).build();

		
		
		
		System.exit(1);

		Set<String> x = printrec(new HashSet<>(), new HashSet<>(), EntityType.get("InvestigationMethod"));

		x.forEach(System.out::println);

	}

	public static Set<String> printrec(Set<String> x, Set<EntityType> types, EntityType rootE) {

		if (types.contains(rootE))
			return x;

		types.add(rootE);
		x.add(rootE.entityName + "\t" + rootE.isLiteral);
		for (EntityType et : rootE.getHierarchicalEntityTypes()) {

			if (types.contains(et))
				continue;

			types.add(et);
			x.add(et.entityName + "\t" + et.isLiteral);

			for (SlotType st : et.getSlots()) {

				for (EntityType set : st.getSlotFillerEntityTypes()) {

					printrec(x, types, set);

				}

			}

		}
		return x;
	}
}
