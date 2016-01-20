/*******************************************************************************
 *    Copyright 2016 University of Mannheim
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.lementation
 *******************************************************************************/
package de.unima.core.io.file.xes;

import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.deckfour.xes.model.XLog;

public class OntModelToXLogExporter {

	public Set<XLog> export(Model model) {
		LogsRetriever retriever = new LogsRetriever(model);
		return retriever.retrieve();
	}
}
