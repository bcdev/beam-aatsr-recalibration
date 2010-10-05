package org.esa.beam.aatsrrecalibration.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class RecalibrateAATSRReflectancesDialog extends SingleTargetProductDialog {
    private String operatorName;
        private Map<String, Object> parameterMap;
        private RecalibrateAATSRReflectancesForm form;
        private String targetProductNameSuffix;

        public static final int DIALOG_WIDTH = 500;
        public static final int DIALOG_HEIGHT = 360;
        private OperatorSpi operatorSpi;

        /**
         * MepixSingleTargetProductDialog constructor
         *
         * @param operatorName - operator name
         * @param appContext - application context
         * @param title - dialog title
         * @param helpID - the help ID
         */
        public RecalibrateAATSRReflectancesDialog(String operatorName, AppContext appContext, String title, String helpID, String targetProductNameSuffix) {
            super(appContext, title, helpID);
            this.operatorName = operatorName;
            this.targetProductNameSuffix = targetProductNameSuffix;
            System.setProperty("gpfMode", "GUI");
            initialize(operatorName, appContext);
        }

        @Override
        protected Product createTargetProduct() throws Exception {
            final HashMap<String, Product> sourceProducts = form.createSourceProductsMap();
            return GPF.createProduct(operatorName, parameterMap, sourceProducts);
        }

        @Override
        public int show() {
            form.initSourceProductSelectors();
            setContent(form);
            return super.show();
        }

        @Override
        public void hide() {
            form.releaseSourceProductSelectors();
            super.hide();
        }

        ///////////// END OF PUBLIC //////////////

        private void initialize(String operatorName, AppContext appContext) {

            operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
            if (operatorSpi == null) {
                throw new IllegalArgumentException("operatorName");
            }

            form = new RecalibrateAATSRReflectancesForm(appContext, operatorSpi, getTargetProductSelector(),
                    targetProductNameSuffix);

            parameterMap = new LinkedHashMap<String, Object>(17);

            // define new value containers for distribution of the target products to different tab panes.
            // for AATSR Reflectance Recalibration, we need just one tab pane
            final PropertyContainer propertyContainer1 = createPanelSpecificValueContainer(null);

            form.addParameterPane(propertyContainer1, "AATSR Reflectance Recalibration");
        }


        private PropertyContainer createPanelSpecificValueContainer(String panelId) {
            ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
            PropertyContainer pc = PropertyContainer.createMapBacked(parameterMap, operatorSpi.getOperatorClass(), parameterDescriptorFactory);

             try {
                pc.setDefaultValues();
            } catch (ValidationException e) {
                showErrorDialog(e.getMessage());
            }

            if (panelId != null && panelId.length() > 0) {
                for (Property property:pc.getProperties()) {
                    PropertyDescriptor propertyDescriptor = property.getDescriptor();
                    if (!propertyDescriptor.getName().startsWith(panelId)) {
                        removeProperty(pc, propertyDescriptor);
                    }
                }
            }
            return pc;
        }

        private void removeProperty(final PropertyContainer propertyContainer, PropertyDescriptor propertyDescriptor) {
            Property property = propertyContainer.getProperty(propertyDescriptor.getName());
            if (property != null)
                propertyContainer.removeProperty(property);
        }

}
