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
package rocket.beans.client.aop;

/**
 * Beans must implement this interface in order to provide aspect advise for
 * defined joinpoints.
 * 
 * @author Miroslav Pokorny
 */
public interface MethodInterceptor extends Advice {
	/**
	 * This method is invoked each time a proxy is invoked. The proxy is free to
	 * perform whatever advise it wants.
	 * 
	 * @param invocation
	 * @return
	 * @throws Throwable
	 */
	Object invoke(MethodInvocation invocation) throws Throwable;
}
