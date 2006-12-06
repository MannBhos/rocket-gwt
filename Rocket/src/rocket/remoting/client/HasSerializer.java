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
package rocket.remoting.client;

import com.google.gwt.user.client.rpc.impl.Serializer;

/**
 * THis interface is implemented by the customized ProxyCreator and added to all generated proxy classes adding a new method allowing access
 * to the accompanying Serializer. Without this new feature it was a pain to get the Serializer accompanying a Service.
 * 
 * @author Miroslav Pokorny (mP)
 */
public interface HasSerializer {
    Serializer getSerializer();
}
