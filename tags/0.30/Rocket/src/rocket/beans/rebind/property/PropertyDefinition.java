/*
 * Copyright Miroslav Pokorny
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rocket.beans.rebind.property;

import rocket.beans.rebind.values.PropertyValueDefinition;
import rocket.util.client.ObjectHelper;
import rocket.util.client.StringHelper;

/**
 * Represents a single property that belongs to a bean
 * 
 * @author Miroslav Pokorny
 */
public class PropertyDefinition {
	/**
	 * The property name
	 */
	private String name;

	public String getName() {
		StringHelper.checkNotEmpty("field:name", name);
		return name;
	}

	public void setName(final String name) {
		StringHelper.checkNotEmpty("parameter:name", name);
		this.name = name;
	}

	private PropertyValueDefinition propertyValueDefinition;

	public PropertyValueDefinition getPropertyValueDefinition() {
		ObjectHelper.checkNotNull("field:propertyValueDefinition", propertyValueDefinition);
		return this.propertyValueDefinition;
	}

	public void setPropertyValueDefinition(final PropertyValueDefinition propertyValueDefinition) {
		ObjectHelper.checkNotNull("parameter:propertyValueDefinition", propertyValueDefinition);
		this.propertyValueDefinition = propertyValueDefinition;
	}
}
