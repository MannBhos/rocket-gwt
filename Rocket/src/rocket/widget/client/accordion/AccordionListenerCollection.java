/*
 * Copyright 2006 NSW Police Government Australia
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
package rocket.widget.client.accordion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rocket.util.client.ObjectHelper;

public class AccordionListenerCollection {
    public AccordionListenerCollection() {
        this.setListeners(new ArrayList());
    }

    /**
     * A list containing listeners to the various accordion events.
     */
    private List listeners;

    public List getListeners() {
        ObjectHelper.checkNotNull("field:listeners", listeners);
        return listeners;
    }

    public void setListeners(final List listeners) {
        ObjectHelper.checkNotNull("parameter:listeners", listeners);
        this.listeners = listeners;
    }

    public void add(final AccordionListener listener) {
        ObjectHelper.checkNotNull("parameter:listener", listener);

        this.getListeners().add(listener);
    }

    public void remove(final AccordionListener listener) {
        ObjectHelper.checkNotNull("parameter:listener", listener);

        this.getListeners().remove(listener);
    }

    public boolean fireBeforeAccordionSelected(final AccordionItem item) {
        ObjectHelper.checkNotNull("parameter:item", item);

        boolean doSelect = true;
        final Iterator listeners = this.getListeners().iterator();

        while (listeners.hasNext()) {
            final AccordionListener listener = (AccordionListener) listeners.next();
            if (false == listener.onBeforeItemSelected(item)) {
                doSelect = false;
                break;
            }
        }
        return doSelect;
    }

    public void fireAccordionSelected(final AccordionItem item) {
        ObjectHelper.checkNotNull("parameter:item", item);

        final Iterator listeners = this.getListeners().iterator();

        while (listeners.hasNext()) {
            final AccordionListener listener = (AccordionListener) listeners.next();
            listener.onItemSelected(item);
        }
    }
}
