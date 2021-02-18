/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   May 15, 2020 (hornm): created
 */
package org.knime.core.node.workflow.virtual.parchunk;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.FlowScopeContext;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.virtual.AbstractVirtualWorkflowNodeModel;

/**
 * Marks a virtual scope, i.e. a scope (a set of nodes) that is not permanently present and deleted after the execution
 * of all contained nodes.
 *
 * A virtual scope is marked by the {@link VirtualParallelizedChunkPortObjectInNodeModel} and
 * {@link VirtualParallelizedChunkPortObjectOutNodeModel}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class FlowVirtualScopeContext extends FlowScopeContext {

    private Consumer<Function<ExecutionContext, UUID>> m_portObjectIdConsumer;

    private NativeNodeContainer m_nc;


    /**
     * Allows one to register a node (whose node model is of type {@link AbstractVirtualWorkflowNodeModel}) with a
     * virtual scope context. This registered node is used to persist (and thus keep) port objects which would have
     * otherwise be gone once the virtual execution of the underlying workflow is finished successfully. See
     * {@link AbstractVirtualWorkflowNodeModel} for more details.
     *
     * This method needs to be called right before the nodes of this virtual scope are executed.
     *
     * @param hostNode the node used for persistence of selected port objects and to provide a file store handler (its
     *            node model needs to be of type {@link AbstractVirtualWorkflowNodeModel})
     * @param virtualInNode a node with a node model of type {@link VirtualParallelizedChunkPortObjectInNodeModel}, used
     *            to get the virtual scope from
     * @param exceptionOnAddingPortObjectToRepository called if an exception is thrown while a new port object is added
     *            to the port object repository (in order to communicate it to the host node and make it available to
     *            downstream nodes)
     * @param exec the host node's execution context, mainly used to copy port objects (which are then made available
     *            via the {@link PortObjectRepository})
     */
    public static void registerHostNodeForPortObjectPersistence(final NativeNodeContainer hostNode,
        final NativeNodeContainer virtualInNode, final Consumer<Exception> exceptionOnAddingPortObjectToRepository,
        final ExecutionContext exec) {
        if (!(hostNode.getNodeModel() instanceof AbstractVirtualWorkflowNodeModel)) {
            throw new IllegalArgumentException(
                "The host node model is not of type " + AbstractVirtualWorkflowNodeModel.class.getSimpleName());
        }
        if (!(virtualInNode.getNodeModel() instanceof VirtualParallelizedChunkPortObjectInNodeModel)) {
            throw new IllegalArgumentException("The virtual input node model is not of expected type "
                + VirtualParallelizedChunkPortObjectInNodeModel.class.getSimpleName());
        }

        FlowVirtualScopeContext virtualScope =
            virtualInNode.getOutgoingFlowObjectStack().peek(FlowVirtualScopeContext.class);
        AbstractVirtualWorkflowNodeModel vnm = (AbstractVirtualWorkflowNodeModel)hostNode.getNodeModel();
        virtualScope.m_portObjectIdConsumer = fct -> {
            try {
                UUID id = fct.apply(exec);
                vnm.addPortObjectId(id);
                vnm.addPortObject(PortObjectRepository.get(id).get());
            } catch (CompletionException e) { // NOSONAR
                exceptionOnAddingPortObjectToRepository.accept(e);
            }
        };

        // we need to keep a reference to the host node, e.g., because
        // all nodes in the scope will use its file store handler
        virtualScope.m_nc = hostNode;
    }

    /**
     * Allows one to communicate port object ids to an associated host node (whose node model is of type
     * {@link AbstractVirtualWorkflowNodeModel}) which in turn will persist (and later restore) those port objects (the
     * port objects available via the {@link PortObjectRepository}).
     *
     * The host node is registered via
     * {@link #registerHostNodeForPortObjectPersistence(NativeNodeContainer, NativeNodeContainer, Consumer, ExecutionContext)}.
     *
     * This method expects a function whose implementation adds a port object to the {@link PortObjectRepository} (which
     * might require to copy the port object by using the supplied {@link ExecutionContext}) and returns the final port
     * object id as provided by the port object repository (cp. e.g.
     * {@link PortObjectRepository#addCopy(PortObject, ExecutionContext)}).
     *
     * @param portObjectIdCreator a function that adds the port object to the {@link PortObjectRepository} and returns
     *            the obtained port object id
     *
     * @throws IllegalStateException if there is no host node associated with the virtual scope
     */
    public void publishPortObjectIdToHostNode(final Function<ExecutionContext, UUID> portObjectIdCreator) {
        if (m_portObjectIdConsumer == null) {
            throw new IllegalStateException("No host node to forward the port objects to set");
        }
        m_portObjectIdConsumer.accept(portObjectIdCreator);
    }

    /**
     * Returns the node container that is (indirectly) responsible for the creation of this virtual scope. The file
     * handlers of this node, e.g., will be used. Must be set before the scope can be executed.
     *
     * This node container does not need to be provided if the start node of this virtual scope is connected (upstream)
     * to another loop start (as it is the case with the parallel chunk loop start node). In all other case it must be
     * set.
     *
     * The node container is set via
     * {@link #registerHostNodeForPortObjectPersistence(NativeNodeContainer, NativeNodeContainer, Consumer, ExecutionContext)}.
     *
     * @return the node associated with this virtual scope or an empty optional if there is no node associated.
     */
    public Optional<NativeNodeContainer> getHostNode() {
        return Optional.ofNullable(m_nc);
    }

}
