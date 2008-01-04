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
package rocket.remoting.rebind.comet;

import java.io.InputStream;

import rocket.generator.rebind.codeblock.TemplatedCodeBlock;
import rocket.generator.rebind.codeblock.TemplatedCodeBlockException;
import rocket.generator.rebind.codeblock.TemplatedFileCodeBlock;
import rocket.generator.rebind.type.Type;
import rocket.util.client.ObjectHelper;

/**
 * An abstraction for the create-gwt rpc proxy template
 * 
 * @author Miroslav Pokorny
 */
class CreateGwtRpcProxyTemplatedFile extends TemplatedFileCodeBlock {

	public CreateGwtRpcProxyTemplatedFile() {
		super();
	}

	/**
	 * The service interface to be realised via deferred binding
	 */
	private Type type;

	protected Type getType() {
		ObjectHelper.checkNotNull("field:type", type);
		return this.type;
	}

	public void setType(final Type type) {
		ObjectHelper.checkNotNull("type:type", type);
		this.type = type;
	}

	protected String getResourceName() {
		return Constants.CREATE_GWT_RPC_PROXY_TEMPLATE;
	}

	protected Object getValue0(final String name) {
		Object value = null;
		while (true) {
			if (Constants.CREATE_PROXY_TYPE.equals(name)) {
				value = this.getType();
				break;
			}
			break;
		}
		return value;
	}
}
