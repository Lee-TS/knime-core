/* Created on Jun 23, 2006 1:24:01 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.ModelContent;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ModelOutputNodeModel extends MetaOutputModel {
    private ModelContent m_predictorParams;

    public ModelOutputNodeModel() {
        super(0, 1);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #saveSettingsTo(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {
        // nothing to do here

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #validateSettings(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        // nothing to do here

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        // nothing to do here

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #execute(de.unikn.knime.core.data.DataTable[],
     *      de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        return inData;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_predictorParams = null;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #configure(de.unikn.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[0];
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #loadPredictorParams(int,
     *      de.unikn.knime.core.node.ModelContent)
     */
    @Override
    protected void loadPredictorParams(final int index,
            final ModelContent predParams) throws InvalidSettingsException {
        m_predictorParams = predParams;
    }

    public ModelContent getPredictorParams() {
        return m_predictorParams;
    }
}
