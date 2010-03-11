package org.esa.beam.aatsrrecalibration.operators;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * Tests for class {@link RecalibrateAATSRReflectancesOp}.
 *
 * @author Ralf Quast
 * @version $Revision: 3053 $ $Date: 2008-09-15 09:00:36 +0200 (Mo, 15 Sep 2008) $
 */
public class RecalibrateAATSRReflectancesTest extends TestCase {

    private String gc1Filename = "ATS_GC1_AXVIEC20070720_093834_20020301_000000_20200101_000000";
    private String vc1Filename = "ATS_VC1_AXVIEC20080607_043326_20080605_062207_20080612_062207";

    Recalibration objectUnderTest = new Recalibration(false, null);

    public void setUp() {
        try {
            objectUnderTest.readDriftTable();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testGetRemoveDriftCorrectionIndex() {
        int index = objectUnderTest.getRemoveDriftCorrectionIndex(vc1Filename);
        assertEquals(2, index);

        String otherFilename = "ATS_VC1_AXVIEC20060103_043326_20060101_062207_20080612_062207";
        index = objectUnderTest.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(1, index);

        otherFilename = "ATS_VC1_AXVIEC20040103_043326_20040101_062207_20080612_062207";
        index = objectUnderTest.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(0, index);
    }

    public void testGetV16NonlinearityCorrectedReflectance() {
        double reflIn = 40.0;
        double reflOut = objectUnderTest.getV16NonlinearityCorrectedReflectance(gc1Filename, reflIn);
        // no correction had been appield for this gc1 filename
        assertEquals(reflIn, reflOut);

        // correction had been appield for this gc1 filename:
        String otherFilename = "ATS_GC1_AXVIEC20020123_073430_20020101_000000_20200101_000000";
        reflOut = objectUnderTest.getV16NonlinearityCorrectedReflectance(otherFilename, reflIn);
        // inversion of a correction: reflOut must be larger
        assertEquals(true, reflIn-reflOut < 0.0);
    }

    public void testRemoveDriftCorrection() {
        int removeDriftCorrIndex = 2;
        double acquisitionTimeDiff = 2289;
        int iChannel = 3;

        double reflIn = 40.0;
        double reflOut = objectUnderTest.removeDriftCorrection(iChannel, removeDriftCorrIndex,
                                                             acquisitionTimeDiff, reflIn);
        // inversion of a correction: reflOut must be larger
        assertEquals(true, reflIn-reflOut < 0.0);
    }

    public void testApplyWholeCorrection() {
        int removeDriftCorrIndex = 2;
        double acquisitionTimeDiff = 2289;

        double sensingStartMillis = 1.21543247264E12;
        int ati = 2291;
        double atiPrev = 1.21551887264E12;
        double atiNext = 1.21560527264E12;
        int iChannel = 3;

        double reflIn = 40.0;
        double refl1 = objectUnderTest.getV16NonlinearityCorrectedReflectance(gc1Filename, reflIn);
        double refl2 = objectUnderTest.removeDriftCorrection(iChannel, removeDriftCorrIndex,
                                                             acquisitionTimeDiff, refl1);
        double reflOut = objectUnderTest.applyDriftCorrection(sensingStartMillis, ati, atiPrev,
                                                              atiNext, iChannel, refl2);
        // just check order of magnitude from D. Smith's figure
        // reflOut must now be larger than reflIn
        assertEquals(true, reflIn-reflOut > 0.0);
        assertEquals(true, reflIn-reflOut < 2.0);
    }
}