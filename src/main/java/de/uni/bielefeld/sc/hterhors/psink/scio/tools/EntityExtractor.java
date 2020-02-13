package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;

public class EntityExtractor {

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build();

		System.exit(1);

		Set<String> x = printrec(new HashSet<>(), new HashSet<>(), SCIOEntityTypes.definedExperimentalGroup);

		x.forEach(System.out::println);

	}

	public static Set<String> printrec(Set<String> x, Set<EntityType> types, EntityType rootE) {

		if (types.contains(rootE))
			return x;

		types.add(rootE);
		x.add(rootE.name + "\t" + rootE.isLiteral);
		for (EntityType et : rootE.getHierarchicalEntityTypes()) {

			if (types.contains(et))
				continue;

			types.add(et);
			x.add(et.name + "\t" + et.isLiteral);

			for (SlotType st : et.getSlots()) {

				for (EntityType set : st.getSlotFillerEntityTypes()) {

					printrec(x, types, set);

				}

			}

		}
		return x;
	}
}
