package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * Checks for Overlapping Words in Two Mentions.
 */
public class OverlappingTemplate extends AbstractFeatureTemplate<OverlappingTemplate.EmptyScope> {

    public OverlappingTemplate() {
        super();
    }

    public OverlappingTemplate(boolean cache){
        super(cache);
    }

    static class EmptyScope
            extends AbstractFactorScope {

        List<DocumentToken> tokensOne;
        List<DocumentToken> tokensTwo;

        EntityType typeOne;
        EntityType typeTwo;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            EmptyScope that = (EmptyScope) o;
            return Objects.equals(tokensOne, that.tokensOne) &&
                    Objects.equals(tokensTwo, that.tokensTwo) &&
                    Objects.equals(typeOne, that.typeOne) &&
                    Objects.equals(typeTwo, that.typeTwo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), tokensOne, tokensTwo, typeOne, typeTwo);
        }

        public EmptyScope(AbstractFeatureTemplate template, List<DocumentToken> tokensOne, List<DocumentToken> tokensTwo, EntityType typeOne, EntityType typeTwo) {

            super(template);
            this.tokensOne = tokensOne;
            this.tokensTwo = tokensTwo;
            this.typeOne = typeOne;
            this.typeTwo = typeTwo;
        }

        public EmptyScope(
                AbstractFeatureTemplate<EmptyScope> template) {
            super(template);
        }

        @Override
        public int implementHashCode() {
            return 0;
        }

        @Override
        public boolean implementEquals(Object obj) {
            return false;
        }

    }

    @Override
    public List<EmptyScope> generateFactorScopes(State state) {
        List<EmptyScope> factors = new ArrayList<>();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
                if(!annotation.equals(annotation2) && annotation.relatedTokens.get(0).getDocTokenIndex() <= annotation2.relatedTokens.get(0).getDocTokenIndex())
                factors.add(new EmptyScope(this, annotation.relatedTokens, annotation2.relatedTokens, annotation.getEntityType(),annotation2.getEntityType()));

            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<EmptyScope> factor) {
        int startOne = factor.getFactorScope().tokensOne.get(0).getDocTokenIndex();
        int startTwo = factor.getFactorScope().tokensTwo.get(0).getDocTokenIndex();
        int endOne = factor.getFactorScope().tokensOne.get(factor.getFactorScope().tokensOne.size()-1).getDocTokenIndex();
        int endTwo = factor.getFactorScope().tokensTwo.get(factor.getFactorScope().tokensTwo.size()-1).getDocTokenIndex();

        if(startTwo < endOne){
            if(endTwo < endOne){
                factor.getFeatureVector().set("Number of Overlapping Words in <"+ factor.getFactorScope().typeOne.name+ ", " +
                        factor.getFactorScope().typeTwo.name+">: "+ factor.getFactorScope().tokensTwo.size(),true);
            } else{
                factor.getFeatureVector().set("Number of Overlapping Words in <"+ factor.getFactorScope().typeOne.name+ ", " +
                        factor.getFactorScope().typeTwo.name+">: "+ (endOne-startTwo+1),true);
            }
        }



        // factor.getFeatureVector().set();

    }

}
