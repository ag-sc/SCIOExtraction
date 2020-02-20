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
 * splits the mention into parts of size n
 */

public class BigramTemplate extends AbstractFeatureTemplate<BigramTemplate.BigramScope> {

    public BigramTemplate (boolean cache){
        super(cache);
    }

    public BigramTemplate() {
        super();
    }

    private static final int MAX_NGRAM_SIZE = 5; //5 seems to be the most efficient
    private static final int MIN_NGRAM_SIZE = 2;

    /**
     *
     */
    class BigramScope extends AbstractFactorScope {


        List<DocumentToken> tokens;
        final public EntityType entityType;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            BigramScope that = (BigramScope) o;
            return Objects.equals(tokens, that.tokens) &&
                    Objects.equals(entityType, that.entityType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), tokens, entityType);
        }

        public BigramScope(AbstractFeatureTemplate template, List<DocumentToken> tokens, EntityType entityType) {
            super(template);
            this.tokens = tokens;
            this.entityType = entityType;
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
    public List<BigramScope> generateFactorScopes(State state) {
        final List<BigramScope> factors = new ArrayList<>();
        for (DocumentLinkedAnnotation annotation: super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            EntityType entityType = annotation.getEntityType();
            factors.add(new BigramScope(this, annotation.relatedTokens, entityType));

        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<BigramScope> factor) {
        EntityType et = factor.getFactorScope().entityType;
        List<DocumentToken> tokens = factor.getFactorScope().tokens;

        for (DocumentToken token : tokens) {
            for(int i = 0; i < token.getText().length(); i++){
                for(int j = MIN_NGRAM_SIZE; j <= MAX_NGRAM_SIZE; j++) {
                    char[] bigramChars = new char[j];
                    if (token.getText().length() >= i + j) {
                        token.getText().getChars(i, i + j, bigramChars, 0);
                        String bigram = new String(bigramChars);
                        factor.getFeatureVector().set(et.name + " " + bigram, true);
                    }
                }
            }
        }
    }
}
