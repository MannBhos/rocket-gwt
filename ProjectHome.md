# ROCKET GWT FRAMEWORK #

The Rocket GWT library/framework consists of a number of components that can assist developers get more out of GWT.

Note that many of features below are quite old and have been significantly improved and replaced by GWT itself (eg image base64 embedding, templating). One should use the official GWT versions as a first preference. This project is also old and stuck on a much older GWT 1.5.x and thus is not usable as is but rather may be the source of fragments if one wishes to expand and bring some small portion up to date.

A variety of modules exist each performing a specialised task. Each module contains many tests that not only make sure it works but also provide a value source of info on how to use the said feature.

Each download includes a **demo.html** (_download,unzip and run_) page that lists all runnable demos.

| Module | Comments | Wiki | Tests/Demos |
|:-------|:---------|:-----|:------------|
| rocket.beans.Beans | Brings many of the Ioc features found in Spring to **clientside** GWT applications | Yes  | lots        |
| rocket.browser.Browser | A small collection of helper methods related to the browser | No   | none        |
| rocket.collection.Collection | Collection related helper methods, also contains some of the java.util.Collection methods not yet emulated. | no   | few         |
| rocket.dom.Dom | Mostly helper methods related to the dom not yet part of GWT's DOM class | no   | none        |
| rocket.dragndrop | Drag n Drop support | no   | yes a demo is available |
| rocket.events.Events | An exciting richer event model built ontop of GWT's Event | yes  | try `rocket.widget.test.basicwidgets.client.BasicWidgetsTest` |
| rocket.generator.Generator | A richer abstraction to generating class. No more printing of java source with this abstraction | yes  | lots        |
| rocket.json.Json | Serialize JSON to java objects and vice versa. | yes  | lots gwt unit tests |
| rocket.logging.Logging | A log4j like logging framework on the client, supporting loggers with individual levels and implementations as well as globally including or excluding all logging statements. | yes  | lots of tests |
| rocket.messaging.Messaging | A simple message delivery system supports topics, queues | no   | none        |
| rocket.remoting.Remoting | Support for comet(server push), json rpc, java rpc ((using rocket's serialization) | yes  | lots        |
| rocket.selection.Selection | Programmatic access to user browser selections ( selection portions of a web by mouse dragging ) | yes  | demo        |
| rocket.serialization.Serialization | A richer more easily extended java serialization framework | no   | lots        |
| rocket.style.Style | Support for cross browser inline, computed (runtime), rule style manipulation using standard css names | yes  | lots        |
| rocket.testing.Testing | Support for writing interactive function unit tests | no   | several demos |
| rocket.text.Text | Text related utilities | no   | some        |
| rocket.util.Util | Lots of misc utility methods including web mode stacktrace support | some | lots        |
| rocket.widget.Widgets | Lots of widgets and authoring aides | no   | lots of demos |

A bit more info about the Widgets.
  * Basic widgets - Does everything the equivalent GWT widgets plus also accept Elements from the DOM.
  * Template based widget and panel authoring. To hijack existing elements, sinking events, styling just override the appropriate method.
  * Calendar - A easy to use template based Calendar widget. Easy to right your own DatePicker etc.
  * HtmlTemplateFactory - hijack elements from the DOM, embed files, execute templates on the client.
  * ImageFactory - smart use of data urls for images whenever possible.
  * Menu - includes a context menu, control which way lists open up,down,left and right.
  * ResizablePanel a panel that has draggable edges that stretch the widget its holding.
  * Slider - slides which include support for a background widget.
  * SortableTable - a multi column sortable table that maps value objects to table rows.
  * TabPanel richer tab panels widget.
  * Viewport - create a google map tile based draggable viewport.

If anything doesnt work, has a bug, could be improved, or you have a suggestion or something to contribute feel free to post to the group or email.

Feel free to also contact me if you want to sponsor the project but taking advantage of our professional services.