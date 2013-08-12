// WikiPathways App for Cytoscape
// opens pathways from WikiPathways as networks in Cytoscape
//
// Copyright 2013 WikiPathways.org
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.wikipathways.cytoscapeapp.internal.io;

import java.io.IOException;
import java.io.InputStream;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.pathvisio.core.model.Pathway;
import org.wikipathways.cytoscapeapp.internal.CyActivator;

/**
 * 
 * @author martinakutmon
 * Reads the GPML file and creates a GPMLNetwork 
 * TODO: currently network and pathway view are initialized --> setting!
 */
public class GpmlNetworkReader implements CyNetworkReader {

	InputStream input = null;
    final String fileName;
	
    @Tunable(description="Import as pathway? Deselect if you want to load network view.", groups={"WikiPathways App"})
    public boolean importAsPathway = true;
    
	protected GpmlNetworkReader(InputStream input, String fileName) {
        this.input = input;
        this.fileName = fileName;
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("Read GPML file " + fileName);
		monitor.setProgress(-1.0);

        monitor.setStatusMessage("Parsing GPML file");
        final Pathway pathway = new Pathway();
        pathway.readFromXml(input, true);
        input = null;

        monitor.setStatusMessage("Constructing network");
        final String name = pathway.getMappInfo().getMapInfoName();
        final CyNetworkView view = newNetwork(name);
        if(importAsPathway) {
        	(new GpmlToPathway(pathway, view)).convert();
        } else {
        	(new GpmlToNetwork(pathway, view)).convert();
        }
        updateNetworkView(view);
	}

    public void cancel() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {}
        }
    }

    public CyNetworkView buildCyNetworkView(CyNetwork network) {
        return null;
    }

    public CyNetwork[] getNetworks() {
        return new CyNetwork[0];
    }

    private static CyNetworkView newNetwork(final String name) {
        final CyNetwork net = CyActivator.netFactory.createNetwork();
        net.getRow(net).set(CyNetwork.NAME, name);
        CyActivator.netMgr.addNetwork(net);
        final CyNetworkView view = CyActivator.netViewFactory.createNetworkView(net);
        CyActivator.netViewMgr.addNetworkView(view);
        return view;
    }

    private static void updateNetworkView(final CyNetworkView netView) {
        GpmlVizStyle.get().apply(netView);
        netView.fitContent();
        netView.updateView();
    }
}
