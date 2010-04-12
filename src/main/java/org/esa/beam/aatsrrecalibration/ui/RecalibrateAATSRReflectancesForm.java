package org.esa.beam.aatsrrecalibration.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import org.esa.beam.aatsrrecalibration.operators.Recalibration;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class RecalibrateAATSRReflectancesForm extends JTabbedPane {
    private List<SourceProductSelector> sourceProductSelectorList;
    private Map<Field, SourceProductSelector> sourceProductSelectorMap;

    private TargetProductSelector targetProductSelector;
    private OperatorSpi operatorSpi;
    private String targetProductNameSuffix;
    private AppContext appContext;
    private JCheckBox useOwnDriftTableCheckBox;
    private JTextField userDriftTablePathTextField;
    private JButton userDriftTablePathButton;

    public RecalibrateAATSRReflectancesForm(AppContext appContext, OperatorSpi operatorSpi, TargetProductSelector targetProductSelector,
                     String targetProductNameSuffix) {
        this.appContext = appContext;
        this.targetProductSelector = targetProductSelector;
        this.operatorSpi = operatorSpi;
        this.targetProductNameSuffix = targetProductNameSuffix;

        initComponents();
    }

    public void initComponents() {
        // Fetch source products
        setupSourceProductSelectorList(operatorSpi);
        if (sourceProductSelectorList.size() > 0) {
           setSourceProductSelectorToolTipTexts();
        }

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        JPanel ioParametersPanel = new JPanel(tableLayout);
        for (SourceProductSelector selector : sourceProductSelectorList) {
            ioParametersPanel.add(selector.createDefaultPanel());
        }
        ioParametersPanel.add(targetProductSelector.createDefaultPanel());
        ioParametersPanel.add(tableLayout.createVerticalSpacer());
        
        sourceProductSelectorList.get(0).addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
                targetProductSelectorModel.setProductName(selectedProduct.getName() + targetProductNameSuffix);
            }
            @Override
            public void selectionContextChanged(SelectionChangeEvent event) {
            }
        });

		this.setPreferredSize(new Dimension(RecalibrateAATSRReflectancesDialog.DIALOG_WIDTH, RecalibrateAATSRReflectancesDialog.DIALOG_HEIGHT));
        this.add("I/O Parameters", ioParametersPanel);
    }

    public void addParameterPane(PropertyContainer propertyContainer, String title) {
        BindingContext context = new BindingContext(propertyContainer);

//        ValueEditorsPane parametersPane = new ValueEditorsPane(context);
        PropertyPane parametersPane = new PropertyPane(context);
        JPanel paremetersPanel = parametersPane.createPanel();
        paremetersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        // setup interaction for user-defined drift tables
        // this fits to the component list created by parametersPane.createPanel() above.
        // adjust if parameter list changes!
        useOwnDriftTableCheckBox = (JCheckBox) paremetersPanel.getComponents()[0];
        JPanel userDriftTablePathPanel = (JPanel) paremetersPanel.getComponents()[2];
        userDriftTablePathTextField = (JTextField) userDriftTablePathPanel.getComponents()[0];
        userDriftTablePathButton = (JButton) userDriftTablePathPanel.getComponents()[1];

        setDefaultDriftTableUIState();

        ActionListener driftTableActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateDriftTableUIState();
            }
		};
        useOwnDriftTableCheckBox.addActionListener(driftTableActionListener);

        this.add(title, new JScrollPane(paremetersPanel));
    }

    public HashMap<String, Product> createSourceProductsMap() {
        final HashMap<String, Product> sourceProducts = new HashMap<String, Product>(8);
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String key = field.getName();
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.alias().isEmpty()) {
                key = annot.alias();
            }
            sourceProducts.put(key, selector.getSelectedProduct());
        }
        return sourceProducts;
    }

    public void initSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.initProducts();
        }
    }

    public void releaseSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.releaseProducts();
        }
    }

    ///////////// END OF PUBLIC //////////////

    private void setupSourceProductSelectorList(OperatorSpi operatorSpi) {
        sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
        sourceProductSelectorMap = new HashMap<Field, SourceProductSelector>(3);
        final Field[] fields = operatorSpi.getOperatorClass().getDeclaredFields();
        for (Field field : fields) {
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (annot != null) {
                final ProductFilter productFilter = new AnnotatedSourceProductFilter(annot);
                SourceProductSelector sourceProductSelector = new SourceProductSelector(appContext);
                sourceProductSelector.setProductFilter(productFilter);
                sourceProductSelectorList.add(sourceProductSelector);
                sourceProductSelectorMap.put(field, sourceProductSelector);
            }
        }
    }

    private void setSourceProductSelectorToolTipTexts() {
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);

            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            final String description = annot.description();
            if (!description.isEmpty()) {
                selector.getProductNameComboBox().setToolTipText(description);
            }
        }
    }

    private void updateDriftTableUIState() {
        if (useOwnDriftTableCheckBox != null) {
            boolean selected = useOwnDriftTableCheckBox.isSelected();
            if (selected) {
                userDriftTablePathTextField.setEnabled(true);
                userDriftTablePathTextField.setEditable(true);
                userDriftTablePathButton.setEnabled(true);
                userDriftTablePathTextField.setText("");
            } else {
                setDefaultDriftTableUIState();
            }
        }
    }

    private void setDefaultDriftTableUIState() {
        if (useOwnDriftTableCheckBox != null) {
            useOwnDriftTableCheckBox.setSelected(false);
        }
        if (userDriftTablePathTextField != null) {
            userDriftTablePathTextField.setEnabled(false);
            userDriftTablePathTextField.setEditable(false);
            userDriftTablePathTextField.setText(Recalibration.DRIFT_TABLE_DEFAULT_FILE_NAME);
        }
        if (userDriftTablePathButton != null) {
            userDriftTablePathButton.setEnabled(false);
        }
    }

    
    private static class AnnotatedSourceProductFilter implements ProductFilter {

        private final SourceProduct annot;

        public AnnotatedSourceProductFilter(SourceProduct annot) {
            this.annot = annot;
        }

        public boolean accept(Product product) {

            if (!annot.type().isEmpty() && !product.getProductType().matches(annot.type())) {
                return false;
            }

            for (String bandName : annot.bands()) {
                if (!product.containsBand(bandName)) {
                    return false;
                }
            }

            return true;
        }
    }

    

}
