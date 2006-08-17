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

import rocket.client.widget.WidgetHelper;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public class Menu extends AbstractMenu {

    public Menu() {
        this.setWidget(this.createMenuList());
        this.sinkEvents(Event.BUTTON_LEFT | Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.MOUSEEVENTS);
    }

    protected Widget createMenuList() {
        WidgetHelper.checkNotAlreadyCreated("menuList", this.hasMenuList());

        final HorizontalMenuList list = new HorizontalMenuList();
        list.addStyleName(MenuConstants.MENU_BAR_STYLE);
        list.setMenu(this);

        this.setMenuList(list);
        return list;
    }
}
