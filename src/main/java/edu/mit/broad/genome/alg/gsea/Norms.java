/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.ScoreMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.LabelledVector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class Norms {

    public static final String NONE = None.NAME;

    public static final String MEANDIV_POS_NEG_SEPERATE = MeanDivPosNegSeperate.NAME;

    // @maint need to keep in synch with above and below
    public static String[] createNormModeNames() {
        return new String[]{
                NONE,
                MEANDIV_POS_NEG_SEPERATE
        };
    }

    public static Norm createNorm(final String normModeName, final float realScore, final Vector rndScores) {

        if (normModeName == null) {
            throw new IllegalArgumentException("Param normModeName cannot be null");
        }

        if (normModeName.equals(NONE)) {
            return new None(realScore, rndScores);
        } else if (normModeName.equals(MEANDIV_POS_NEG_SEPERATE)) {
            return new Norms.MeanDivPosNegSeperate(realScore, rndScores);
        } else {
            throw new IllegalArgumentException("Unknown norm mode: " + normModeName);
        }
    }

    public static class Struc {
        public LabelledVector normReal;
        public Dataset normRnd;
    }

    // This is the key API
    public static Struc normalize(final String normName, final LabelledVector realScores, final Dataset rndScores_full) {

        final Vector normRealScores = new Vector(realScores.getSize());
        final String[] labels = new String[realScores.getSize()];
        final List<String> labels_list = new ArrayList<String>();

        // @note end ds may have more rows that in the real scores
        // we pick only those that we want

        final Matrix normRndScoresMatrix = new Matrix(realScores.getSize(), rndScores_full.getNumCol());

        for (int r = 0; r < realScores.getSize(); r++) {

            final String rowName = realScores.getLabel(r);
            final float real = realScores.getScore(rowName);
            final Norm norm = Norms.createNorm(normName, real, rndScores_full.getRow(rowName));

            // Note from Pablo:
            // I think the NES with NaN (because of the skewness) should ... 
            // be excluded from the computation of p-values or FDRs.
            
            normRndScoresMatrix.setRow(r, norm.getRandomNorm());
            normRealScores.setElement(r, norm.getRealNorm());
            labels[r] = rowName;
            labels_list.add(rowName);
        }

        Struc struc = new Struc();
        struc.normReal = new LabelledVector(realScores.getName() + "_norm", labels, normRealScores);
        struc.normRnd = new DefaultDataset("norm", normRndScoresMatrix, labels_list, rndScores_full.getColumnNames(), rndScores_full.getAnnot());

        return struc;
    }

    private abstract static class AbstractNormOne implements Norm {
        float real_orig;
        float realNorm;
        Vector rnd_orig;
        Vector rndNorm;
        String normModeName;

        AbstractNormOne(final String normModeName) {
            this.normModeName = normModeName;
        }

        public Vector getRandomNorm() {
            return rndNorm;
        }

        public float getRealNorm() {
            return realNorm;
        }
    } // End class One


    public static class None extends AbstractNormOne {
        private static String NAME = "None";

        public None(final float real, final Vector rnd) {
            super(NAME);
            this.real_orig = real;
            this.rnd_orig = rnd;
            this.rndNorm = rnd_orig;
            this.realNorm = real_orig;
        }

    } // End class VarMean2Sided

    // This is the GSEA pnas way
    public static class MeanDivPosNegSeperate extends AbstractNormOne {

        private static String NAME = "meandiv";

        int numRndPos;
        int numRndNeg;
        float meanPos_orig;
        float varPos_orig;

        float meanNeg_orig;
        float varNeg_orig;

        public MeanDivPosNegSeperate(final float real, final Vector rnd) {
            super(NAME);
            this.real_orig = real;
            this.rnd_orig = rnd;

            final Vector onlyPos = rnd_orig.extract(ScoreMode.POS_ONLY);
            final Vector onlyNeg = rnd_orig.extract(ScoreMode.NEG_ONLY);

            // Possible optimization here is to avoid the mean() calls and division operations
            // in the cases where we *know* up-front that the calculation will result in an
            // NaN or Infinity.  It is questionable whether that's actually faster than just
            // performing the operation, though.
            
            // NOTE: I think we need to avoid Infinity or do something about it.  Better off just
            // dealing with NaN values instead.  Can this be done?  Is that correct?
            // Or is it better to just flag all the troublesome items similarly and have callers
            // detect the flag?  Could maybe store them as Null and detect them that way.
            
            this.meanPos_orig = (float) onlyPos.mean();
            // Possible optimization: var value here is unused, though it may still be important
            // because of its side effects in the Vector instance.  *If* that's the case, then
            // find a more straightforward way to achieve those.
            // (Likewise for Neg).
            this.varPos_orig = (float) Math.sqrt(onlyPos.var(false, false));

            this.meanNeg_orig = (float) onlyNeg.mean();
            this.varNeg_orig = (float) Math.sqrt(onlyNeg.var(false, false));

            // first norm the rnds
            int rnd_origSize = rnd_orig.getSize();
			this.rndNorm = new Vector(rnd_origSize);
            for (int i = 0; i < rnd_origSize; i++) {
            	float orig = rnd_orig.getElement(i);
                if (XMath.isPositive(orig)) {
                	float score = orig / meanPos_orig;
                	rndNorm.setElement(i, score);
                    numRndPos++;
                } else {
                	float score = orig / Math.abs(meanNeg_orig); // @note abs
                    rndNorm.setElement(i, score);
                    numRndNeg++;
                }
            }

            // then norm the real
            if (XMath.isPositive(real_orig)) {
                this.realNorm = real_orig / meanPos_orig;
            } else {
                this.realNorm = real_orig / Math.abs(meanNeg_orig); // @note abs
            }
        }
    } // End class MeanDivPosNegSeperate
}
