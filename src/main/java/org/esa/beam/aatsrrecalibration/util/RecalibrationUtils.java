package org.esa.beam.aatsrrecalibration.util;

import javax.swing.JOptionPane;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class RecalibrationUtils {

    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("aatsrrecalibration");

    public static void logInfoMessage(String msg) {
            if (System.getProperty("gpfMode") != null && System.getProperty("gpfMode").equals("GUI")) {
                JOptionPane.showOptionDialog(null, msg, "AATSR Recalibration - Info Message", JOptionPane.DEFAULT_OPTION,
                                             JOptionPane.INFORMATION_MESSAGE, null, null, null);
            } else {
                info(msg);
            }
        }

        public static void logErrorMessage(String msg) {
            if (System.getProperty("gpfMode") != null && System.getProperty("gpfMode").equals("GUI")) {
                JOptionPane.showOptionDialog(null, msg, "AATSR Recalibration - Error Message", JOptionPane.DEFAULT_OPTION,
                                             JOptionPane.ERROR_MESSAGE, null, null, null);
            } else {
                info(msg);
            }
        }


    public static void info(final String msg) {
        logger.info(msg);
        System.out.println(msg);
    }

}
