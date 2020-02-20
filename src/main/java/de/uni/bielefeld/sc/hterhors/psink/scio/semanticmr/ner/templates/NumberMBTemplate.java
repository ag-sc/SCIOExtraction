package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * Checks the Number of other annotated mentions between two mentions
 */
public class NumberMBTemplate extends AbstractFeatureTemplate<NumberMBTemplate.NumberMBScope> {


    static class NumberMBScope
            extends AbstractFactorScope {

        int indexOne;
        int indexTwo;

        EntityType typeOne;
        EntityType typeTwo;

        List<DocumentLinkedAnnotation> annotationList;


        public NumberMBScope(
                AbstractFeatureTemplate<NumberMBScope> template, List<DocumentLinkedAnnotation> annotationList, int indexOne, int indexTwo,
                EntityType typeOne, EntityType typeTwo) {
            super(template);
            this.annotationList = annotationList;
            this.indexOne = indexOne;
            this.indexTwo = indexTwo;
            this.typeOne = typeOne;
            this.typeTwo = typeTwo;
        }

        @Override
        public int implementHashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            NumberMBScope that = (NumberMBScope) o;
            return indexOne == that.indexOne &&
                    indexTwo == that.indexTwo &&
                    Objects.equals(typeOne, that.typeOne) &&
                    Objects.equals(typeTwo, that.typeTwo) &&
                    Objects.equals(annotationList, that.annotationList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), indexOne, indexTwo, typeOne, typeTwo, annotationList);
        }

        @Override
        public boolean implementEquals(Object obj) {
            return false;
        }

    }

    @Override
    public List<NumberMBScope> generateFactorScopes(State state) {
        List<NumberMBScope> factors = new ArrayList<>();

        List<DocumentLinkedAnnotation> annotationList = super.<DocumentLinkedAnnotation>getPredictedAnnotations(state);

        for (DocumentLinkedAnnotation annotation : annotationList) {
            for (DocumentLinkedAnnotation annotation2 : annotationList) {
                int indexOne = annotation.relatedTokens.get(annotation.relatedTokens.size() - 1).getDocTokenIndex();
                int indexTwo = annotation2.relatedTokens.get(0).getDocTokenIndex();
                if (indexTwo > indexOne) {
                    EntityType typeOne = annotation.entityType;
                    EntityType typeTwo = annotation2.entityType;
                    factors.add(new NumberMBScope(this, annotationList, indexOne, indexTwo, typeOne, typeTwo));
                }
            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<NumberMBScope> factor) {
        int mentionCounter = 0;
        for (DocumentLinkedAnnotation annotation : factor.getFactorScope().annotationList) {
            if (annotation.relatedTokens.get(0).getDocTokenIndex() >
                    factor.getFactorScope().indexOne &&
                    annotation.relatedTokens.get(annotation.relatedTokens.size() - 1).getDocTokenIndex() < factor.getFactorScope().indexTwo) {
                mentionCounter++;
            }
        }
        factor.getFeatureVector().set(factor.getFactorScope().typeOne.name + ", " + factor.getFactorScope().typeTwo.name + " " +
                mentionCounter, true);

    }

}
