package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * Checks for the words in between two mentions without last and first word in between.
 */
public class WBOTemplate extends AbstractFeatureTemplate<WBOTemplate.wboScope> {

    private static final int MAX_NUMBER_OF_WORDS = 6;  //6 Wörter sind deutlich erfolgreicher als 5, aber dann gibts eohl keine Steigerung mehr. Warum?


    static class wboScope
            extends AbstractFactorScope {

        public DocumentToken tokenOne;
        public DocumentToken tokenTwo;

        public EntityType typeOne;
        public EntityType typeTwo;

        public Document document;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            wboScope wboScope = (wboScope) o;
            return Objects.equals(tokenOne, wboScope.tokenOne) &&
                    Objects.equals(tokenTwo, wboScope.tokenTwo) &&
                    Objects.equals(typeOne, wboScope.typeOne) &&
                    Objects.equals(typeTwo, wboScope.typeTwo) &&
                    Objects.equals(document, wboScope.document);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), tokenOne, tokenTwo, typeOne, typeTwo, document);
        }

        public wboScope(AbstractFeatureTemplate template, DocumentToken tokenOne, DocumentToken tokenTwo, EntityType typeOne, EntityType typeTwo, Document document) {
            super(template);
            this.tokenOne = tokenOne;
            this.tokenTwo = tokenTwo;
            this.typeOne = typeOne;
            this.typeTwo = typeTwo;
            this.document = document;
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
    public List<wboScope> generateFactorScopes(State state) {
        List<wboScope> factors = new ArrayList<>();
        Document document = state.getInstance().getDocument();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
                if (!annotation.equals(annotation2)) {
                    DocumentToken tokenOne = annotation.relatedTokens.get(annotation.relatedTokens.size() - 1);
                    DocumentToken tokenTwo = annotation2.relatedTokens.get(0);
                    factors.add(new wboScope(this, tokenOne, tokenTwo, annotation.getEntityType(), annotation2.getEntityType(), document));
                }
            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<wboScope> factor) {
        String subtext;

        DocumentToken tokenOne = factor.getFactorScope().tokenOne;
        DocumentToken tokenTwo = factor.getFactorScope().tokenTwo;

        if (tokenTwo.getDocTokenIndex() - tokenOne.getDocTokenIndex() < MAX_NUMBER_OF_WORDS
                && tokenTwo.getDocTokenIndex() - tokenOne.getDocTokenIndex() > 3) {
            subtext = factor.getFactorScope().document.getContent(factor.getFactorScope().document.tokenList.get(
                    tokenOne.getDocTokenIndex() + 1), factor.getFactorScope().document.tokenList.get(
                    tokenTwo.getDocTokenIndex() - 1));
            factor.getFeatureVector().set("WBO <" + factor.getFactorScope().typeOne + ", " + factor.getFactorScope().typeTwo + "> "
                    + subtext, true);
        }
    }
}
