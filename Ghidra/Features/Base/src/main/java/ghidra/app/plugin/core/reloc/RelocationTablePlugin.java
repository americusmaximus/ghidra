/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.reloc;

import static ghidra.framework.model.DomainObjectEvent.*;
import static ghidra.program.util.ProgramEvent.*;

import docking.action.DockingAction;
import ghidra.app.CorePluginPackage;
import ghidra.app.events.ProgramActivatedPluginEvent;
import ghidra.app.events.ProgramLocationPluginEvent;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.services.GoToService;
import ghidra.framework.model.DomainObjectChangedEvent;
import ghidra.framework.model.DomainObjectListener;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;
import ghidra.util.table.SelectionNavigationAction;
import ghidra.util.table.actions.MakeProgramSelectionAction;

//@formatter:off
@PluginInfo(
	status = PluginStatus.RELEASED,
	packageName = CorePluginPackage.NAME,
	category = PluginCategoryNames.CODE_VIEWER,
	shortDescription = "Displays relocation information",
	description = "This plugin provides a component for displaying the reloction table. "
			+ "The table can be used to navigate in the code browser.",
	servicesRequired = { GoToService.class },
	eventsProduced = { ProgramLocationPluginEvent.class },
	eventsConsumed = { ProgramActivatedPluginEvent.class }
)
//@formatter:on
public class RelocationTablePlugin extends Plugin implements DomainObjectListener {

	private Program currentProgram;
	private RelocationProvider provider;

	public RelocationTablePlugin(PluginTool tool) {
		super(tool);
	}

	@Override
	protected void init() {
		provider = new RelocationProvider(this);
		createActions();
	}

	private void createActions() {

		DockingAction selectAction = new MakeProgramSelectionAction(this, provider.getTable());
		tool.addLocalAction(provider, selectAction);

		DockingAction navigationAction = new SelectionNavigationAction(this, provider.getTable());
		tool.addLocalAction(provider, navigationAction);
	}

	@Override
	public void dispose() {
		super.dispose();
		provider.dispose();
		currentProgram = null;
	}

	@Override
	public void processEvent(PluginEvent event) {
		if (event instanceof ProgramActivatedPluginEvent) {
			ProgramActivatedPluginEvent ev = (ProgramActivatedPluginEvent) event;
			Program oldProg = currentProgram;
			Program newProg = ev.getActiveProgram();
			if (oldProg != null) {
				programClosed();
			}
			if (newProg != null) {
				programOpened(newProg);
			}
		}
	}

	private void programOpened(Program p) {
		p.addListener(this);
		currentProgram = p;
		provider.setProgram(p);
	}

	private void programClosed() {
		currentProgram.removeListener(this);
		currentProgram = null;
		provider.setProgram(null);
	}

	@Override
	public void domainObjectChanged(DomainObjectChangedEvent ev) {
		if (ev.contains(IMAGE_BASE_CHANGED, RELOCATION_ADDED, RESTORED)) {
			provider.setProgram(currentProgram);
		}
	}
}
