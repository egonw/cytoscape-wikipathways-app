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
package org.cytoscape.wikipathways.app.internal.io;

import java.io.File;
import java.io.IOException;

import org.cytoscape.wikipathways.app.internal.model.GPMLNetwork;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkImpl;
import org.cytoscape.wikipathways.app.internal.model.GPMLNetworkManager;
import org.cytoscape.work.TaskMonitor;
import org.pathvisio.core.model.Pathway;

/**
 * 
 * @author martinakutmon
 * Reads the GPML file and creates a GPMLNetwork 
 * TODO: currently network and pathway view are initialized --> setting!
 */
public class GpmlReaderImpl extends AbstractGpmlReader  {

	private GPMLNetworkManager gpmlMgr;
	
	private File inputFile;
	
	protected GpmlReaderImpl(File inputFile, GPMLNetworkManager gpmlMgr) {
		super(inputFile);
		this.gpmlMgr = gpmlMgr;
		this.inputFile = inputFile;
	}

	@Override
	public void run(TaskMonitor monitor) throws IOException {
		monitor.setProgress(0.0);
		System.out.println("Import GPML file");
		try {
			Pathway pathway = new Pathway();
			pathway.readFromXml(inputFile, true);
			
			System.out.println(pathway.getDataObjects().size());
			GPMLNetwork net = new GPMLNetworkImpl(pathway, gpmlMgr.getCyNetworkFactory());
			
			// TODO: decide which view should be created
			// currently both are initialized!
			gpmlMgr.addNetworkView(net);
			gpmlMgr.addPathwayView(net);
			
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}
		
		monitor.setProgress(1.0);
	}
}