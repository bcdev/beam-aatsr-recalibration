package org.esa.beam.aatsrrecalibration.operators;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests for class {@link RecalibrateAATSRReflectancesOp}.
 *
 * @author Ralf Quast
 * @version $Revision: 3053 $ $Date: 2008-09-15 09:00:36 +0200 (Mo, 15 Sep 2008) $
 */
public class RecalibrateAATSRReflectancesTest {

    private String gc1Filename = "ATS_GC1_AXVIEC20070720_093834_20020301_000000_20200101_000000";
    private String vc1Filename = "ATS_VC1_AXVIEC20080607_043326_20080605_062207_20080612_062207";

    Recalibration recalibration = new Recalibration(false, null);

    @Before
    public void setUp() {
        try {
            recalibration.readDriftTable();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetRemoveDriftCorrectionIndex() {
        int index = recalibration.getRemoveDriftCorrectionIndex(vc1Filename);
        assertEquals(2, index);

        String otherFilename = "ATS_VC1_AXVIEC20060103_043326_20060101_062207_20080612_062207";
        index = recalibration.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(1, index);

        otherFilename = "ATS_VC1_AXVIEC20040103_043326_20040101_062207_20080612_062207";
        index = recalibration.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(0, index);

        otherFilename = "ATS_VC1_AXVIEC20100412_043413_20100410_000100_20100417_000100";
        index = recalibration.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(0, index);

        otherFilename = "ATS_VC1_AXVIEC20100701_134433_20100628_065615_20100705_065615";
        index = recalibration.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(0, index);

        otherFilename = "ATS_VC1_AXVIEC20100726_083523_20100721_102646_20100721_134806";
        index = recalibration.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(2, index);

        otherFilename = "ATS_VC1_AXVIEC20100804_014039_20100801_173259_2010808_173259";
        index = recalibration.getRemoveDriftCorrectionIndex(otherFilename);
        assertEquals(2, index);
    }

    @Test
    public void testGetV16NonlinearityCorrectedReflectance() {
        double reflIn = 40.0;
        double reflOut = recalibration.getV16NonlinearityCorrectedReflectance(gc1Filename, reflIn);
        // no correction had been appield for this gc1 filename
        assertEquals(reflIn, reflOut, 1.0e-8);

        // correction had been appield for this gc1 filename:
        String otherFilename = "ATS_GC1_AXVIEC20020123_073430_20020101_000000_20200101_000000";
        reflOut = recalibration.getV16NonlinearityCorrectedReflectance(otherFilename, reflIn);
        // inversion of a correction: reflOut must be larger
        assertEquals(true, reflIn - reflOut < 0.0);
    }

    @Test
    public void testRemoveDriftCorrection() {
        int removeDriftCorrIndex = 2;
        double acquisitionTimeDiff = 2289;
        int iChannel = 3;

        double reflIn = 40.0;
        double reflOut = recalibration.removeDriftCorrection(iChannel, removeDriftCorrIndex,
                                                             acquisitionTimeDiff, reflIn);
        // inversion of a correction: reflOut must be larger
        assertEquals(true, reflIn - reflOut < 0.0);
    }

    @Test
    public void testApplyWholeCorrection() {
        int removeDriftCorrIndex = 2;
        double acquisitionTimeDiff = 2289;

        double sensingStartMillis = 1.21543247264E12;
        int ati = 2291;
        double atiPrev = 1.21551887264E12;
        double atiNext = 1.21560527264E12;
        int iChannel = 3;

        double reflIn = 40.0;
        double refl1 = recalibration.getV16NonlinearityCorrectedReflectance(gc1Filename, reflIn);
        double refl2 = recalibration.removeDriftCorrection(iChannel, removeDriftCorrIndex,
                                                           acquisitionTimeDiff, refl1);
        double reflOut = recalibration.applyDriftCorrection(sensingStartMillis, ati, atiPrev,
                                                            atiNext, iChannel, refl2);
        // just check order of magnitude from D. Smith's figure
        // reflOut must now be larger than reflIn
        assertEquals(true, reflIn - reflOut > 0.0);
        assertEquals(true, reflIn - reflOut < 2.0);
    }

    @Test
    public void testGetAcquisitionTimeIndex() {
        String timestring = "01-FEB-2002 00:00:00";
        assertEquals(-1, recalibration.getAcquisitionTimeIndex(timestring));

        timestring = "04-MAR-2002 00:00:00";
        assertEquals(3, recalibration.getAcquisitionTimeIndex(timestring));

        timestring = "04-MAR-2002 03:00:00";
        assertEquals(3, recalibration.getAcquisitionTimeIndex(timestring));

        timestring = "03-APR-2002 07:34:45";
        assertEquals(33, recalibration.getAcquisitionTimeIndex(timestring));

        timestring = "01-AUG-2010 10:00:00";
        assertEquals(-1, recalibration.getAcquisitionTimeIndex(timestring));
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testGetAcquisitionTimeIndex_TimeStringToShort() {
        recalibration.getAcquisitionTimeIndex("bla");
    }
}
