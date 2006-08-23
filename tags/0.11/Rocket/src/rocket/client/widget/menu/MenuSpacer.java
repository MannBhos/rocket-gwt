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
package rocket.client.widget.menu;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * This widget represents a menuSpacer within a menulist.
 *
 * @author Miroslav Pokorny (mP)
 */
public class MenuSpacer extends AbstractMenuWidget implements MenuWidget {
    public MenuSpacer() {
        this.setWidget(this.createWidget());
    }

    // COMPOSITE :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Simply ignores all browser events.
     */
    public void onBrowserEvent(final Event event) {
    }

    protected Widget createWidget() {
        final HTML html = new HTML("<hr/>");
        html.addStyleName(MenuConstants.SPACER_STYLE);
        return html;
    }
}
