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
package de.unima.core.domain.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.unima.core.domain.model.Project;
import de.unima.core.domain.model.Repository;
import de.unima.core.domain.model.Schema;

public class ProjectTest {

	@Test
	public void whenAllSchemasAreUnlinkedThenThereShouldBeNoLinkedSchemas(){
		final Project project = new Project("http://test.de/Project/1", "Test", new Repository("http://www.test.de/Repository/1"));
		final Schema linkedSchema = new Schema("http://test.de/Schema/1");
		project.linkSchema(linkedSchema);
		assertThat(project.isSchemaLinked(linkedSchema.getId()), is(true));
		project.unlinkAllSchemas();
		assertThat(project.isSchemaLinked(linkedSchema.getId()), is(not(true)));
	}
}
