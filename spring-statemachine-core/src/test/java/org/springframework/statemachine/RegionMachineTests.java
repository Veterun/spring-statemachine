/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.statemachine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.statemachine.AbstractStateMachineTests.TestEntryAction;
import org.springframework.statemachine.AbstractStateMachineTests.TestEvents;
import org.springframework.statemachine.AbstractStateMachineTests.TestExitAction;
import org.springframework.statemachine.AbstractStateMachineTests.TestStates;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.region.Region;
import org.springframework.statemachine.state.DefaultPseudoState;
import org.springframework.statemachine.state.EnumState;
import org.springframework.statemachine.state.PseudoState;
import org.springframework.statemachine.state.PseudoStateKind;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.DefaultExternalTransition;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.EventTrigger;

/**
 * Statemachine tests using regions.
 *
 * @author Janne Valkealahti
 *
 */
public class RegionMachineTests {

	@Test
	public void testSimpleRegion() throws Exception {
		PseudoState pseudoState = new DefaultPseudoState(PseudoStateKind.INITIAL);
		TestEntryAction entryActionS1 = new TestEntryAction("S1");
		TestExitAction exitActionS1 = new TestExitAction("S1");
		Collection<Action<TestStates, TestEvents>> entryActionsS1 = new ArrayList<Action<TestStates, TestEvents>>();
		entryActionsS1.add(entryActionS1);
		Collection<Action<TestStates, TestEvents>> exitActionsS1 = new ArrayList<Action<TestStates, TestEvents>>();
		exitActionsS1.add(exitActionS1);


		State<TestStates,TestEvents> stateSI = new EnumState<TestStates,TestEvents>(TestStates.SI, pseudoState);
		State<TestStates,TestEvents> stateS1 = new EnumState<TestStates,TestEvents>(TestStates.S1, null, entryActionsS1, exitActionsS1);
		State<TestStates,TestEvents> stateS2 = new EnumState<TestStates,TestEvents>(TestStates.S2);
		State<TestStates,TestEvents> stateS3 = new EnumState<TestStates,TestEvents>(TestStates.S3);

		Collection<State<TestStates,TestEvents>> states = new ArrayList<State<TestStates,TestEvents>>();
		states.add(stateSI);
		states.add(stateS1);
		states.add(stateS2);
		states.add(stateS3);

		Collection<Transition<TestStates,TestEvents>> transitions = new ArrayList<Transition<TestStates,TestEvents>>();

		DefaultExternalTransition<TestStates,TestEvents> transitionFromSIToS1 =
				new DefaultExternalTransition<TestStates,TestEvents>(stateSI, stateS1, null, TestEvents.E1, null, new EventTrigger<TestStates,TestEvents>(TestEvents.E1));

		DefaultExternalTransition<TestStates,TestEvents> transitionFromS1ToS2 =
				new DefaultExternalTransition<TestStates,TestEvents>(stateS1, stateS2, null, TestEvents.E2, null, new EventTrigger<TestStates,TestEvents>(TestEvents.E2));

		DefaultExternalTransition<TestStates,TestEvents> transitionFromS2ToS3 =
				new DefaultExternalTransition<TestStates,TestEvents>(stateS2, stateS3, null, TestEvents.E3, null, new EventTrigger<TestStates,TestEvents>(TestEvents.E3));

		transitions.add(transitionFromSIToS1);
		transitions.add(transitionFromS1ToS2);
		transitions.add(transitionFromS2ToS3);

		SyncTaskExecutor taskExecutor = new SyncTaskExecutor();
		EnumStateMachine<TestStates, TestEvents> machine = new EnumStateMachine<TestStates, TestEvents>(states, transitions, stateSI, null);
		machine.setTaskExecutor(taskExecutor);
		machine.afterPropertiesSet();
		machine.start();

		Collection<Region<TestStates,TestEvents>> regions = new ArrayList<Region<TestStates,TestEvents>>();
		regions.add(machine);
		RegionState<TestStates,TestEvents> state = new RegionState<TestStates,TestEvents>(TestStates.S11, regions);

		assertThat(state.isSimple(), is(false));
		assertThat(state.isComposite(), is(true));
		assertThat(state.isOrthogonal(), is(false));
		assertThat(state.isSubmachineState(), is(false));

		assertThat(state.getIds(), contains(TestStates.SI));

		machine.sendEvent(TestEvents.E1);
		machine.sendEvent(TestEvents.E2);

		assertThat(entryActionS1.onExecuteLatch.await(1, TimeUnit.SECONDS), is(true));
		assertThat(exitActionS1.onExecuteLatch.await(1, TimeUnit.SECONDS), is(true));
		assertThat(entryActionS1.stateContexts.size(), is(1));
		assertThat(exitActionS1.stateContexts.size(), is(1));
	}

	@Test
	public void testMultiRegion() throws Exception {
		SyncTaskExecutor taskExecutor = new SyncTaskExecutor();
		PseudoState pseudoState = new DefaultPseudoState(PseudoStateKind.INITIAL);
		State<TestStates,TestEvents> stateSI = new EnumState<TestStates,TestEvents>(TestStates.SI);

		TestEntryAction entryActionS111 = new TestEntryAction("S111");
		TestExitAction exitActionS111 = new TestExitAction("S111");
		Collection<Action<TestStates, TestEvents>> entryActionsS111 = new ArrayList<Action<TestStates, TestEvents>>();
		entryActionsS111.add(entryActionS111);
		Collection<Action<TestStates, TestEvents>> exitActionsS111 = new ArrayList<Action<TestStates, TestEvents>>();
		exitActionsS111.add(exitActionS111);
		State<TestStates,TestEvents> stateS111 = new EnumState<TestStates,TestEvents>(TestStates.S111, null, entryActionsS111, exitActionsS111, pseudoState);

		TestEntryAction entryActionS112 = new TestEntryAction("S112");
		TestExitAction exitActionS112 = new TestExitAction("S112");
		Collection<Action<TestStates, TestEvents>> entryActionsS112 = new ArrayList<Action<TestStates, TestEvents>>();
		entryActionsS112.add(entryActionS112);
		Collection<Action<TestStates, TestEvents>> exitActionsS112 = new ArrayList<Action<TestStates, TestEvents>>();
		exitActionsS112.add(exitActionS112);
		State<TestStates,TestEvents> stateS112 = new EnumState<TestStates,TestEvents>(TestStates.S112, null, entryActionsS112, exitActionsS112);

		TestEntryAction entryActionS121 = new TestEntryAction("S121");
		TestExitAction exitActionS121 = new TestExitAction("S121");
		Collection<Action<TestStates, TestEvents>> entryActionsS121 = new ArrayList<Action<TestStates, TestEvents>>();
		entryActionsS111.add(entryActionS121);
		Collection<Action<TestStates, TestEvents>> exitActionsS121 = new ArrayList<Action<TestStates, TestEvents>>();
		exitActionsS111.add(exitActionS121);
		State<TestStates,TestEvents> stateS121 = new EnumState<TestStates,TestEvents>(TestStates.S121, null, entryActionsS121, exitActionsS121, pseudoState);

		Collection<State<TestStates,TestEvents>> states11 = new ArrayList<State<TestStates,TestEvents>>();
		states11.add(stateSI);
		states11.add(stateS111);
		states11.add(stateS112);
		Collection<Transition<TestStates,TestEvents>> transitions11 = new ArrayList<Transition<TestStates,TestEvents>>();
		DefaultExternalTransition<TestStates,TestEvents> transitionFromS111ToS112 =
				new DefaultExternalTransition<TestStates,TestEvents>(stateS111, stateS112, null, TestEvents.E2, null, new EventTrigger<TestStates,TestEvents>(TestEvents.E2));
		transitions11.add(transitionFromS111ToS112);
		EnumStateMachine<TestStates, TestEvents> machine11 = new EnumStateMachine<TestStates, TestEvents>(states11, transitions11, stateS111, null);
		machine11.setTaskExecutor(taskExecutor);
		machine11.afterPropertiesSet();

		Collection<State<TestStates,TestEvents>> states12 = new ArrayList<State<TestStates,TestEvents>>();
		states12.add(stateSI);
		states12.add(stateS121);
		Collection<Transition<TestStates,TestEvents>> transitions12 = new ArrayList<Transition<TestStates,TestEvents>>();
		DefaultExternalTransition<TestStates,TestEvents> transitionFromSIToS121 =
				new DefaultExternalTransition<TestStates,TestEvents>(stateSI, stateS111, null, TestEvents.E3, null, new EventTrigger<TestStates,TestEvents>(TestEvents.E3));
		transitions12.add(transitionFromSIToS121);
		EnumStateMachine<TestStates, TestEvents> machine12 = new EnumStateMachine<TestStates, TestEvents>(states12, transitions12, stateS121, null);
		machine12.setTaskExecutor(taskExecutor);
		machine12.afterPropertiesSet();

		Collection<Region<TestStates,TestEvents>> regions = new ArrayList<Region<TestStates,TestEvents>>();
		regions.add(machine11);
		regions.add(machine12);
		RegionState<TestStates,TestEvents> stateR = new RegionState<TestStates,TestEvents>(TestStates.S11, regions, null, null, null, pseudoState);

		Collection<State<TestStates,TestEvents>> states = new ArrayList<State<TestStates,TestEvents>>();
		states.add(stateR);
		Collection<Transition<TestStates,TestEvents>> transitions = new ArrayList<Transition<TestStates,TestEvents>>();
		DefaultExternalTransition<TestStates,TestEvents> transitionFromSIToRegionstate =
				new DefaultExternalTransition<TestStates,TestEvents>(stateSI, stateR, null, TestEvents.E1, null, new EventTrigger<TestStates,TestEvents>(TestEvents.E1));
		transitions.add(transitionFromSIToRegionstate);
		EnumStateMachine<TestStates, TestEvents> machine = new EnumStateMachine<TestStates, TestEvents>(states, transitions, stateR, null);

		machine.setTaskExecutor(taskExecutor);
		machine.afterPropertiesSet();
		machine.start();

		assertThat(entryActionS111.onExecuteLatch.await(1, TimeUnit.SECONDS), is(true));
		assertThat(exitActionS111.onExecuteLatch.await(1, TimeUnit.SECONDS), is(false));
		assertThat(entryActionS121.onExecuteLatch.await(1, TimeUnit.SECONDS), is(true));
		assertThat(exitActionS121.onExecuteLatch.await(1, TimeUnit.SECONDS), is(false));

		assertThat(entryActionS111.stateContexts.size(), is(1));
		assertThat(exitActionS111.stateContexts.size(), is(0));
		assertThat(entryActionS121.stateContexts.size(), is(1));
		assertThat(exitActionS121.stateContexts.size(), is(0));

		machine.sendEvent(TestEvents.E2);

		assertThat(entryActionS111.onExecuteLatch.await(1, TimeUnit.SECONDS), is(true));
		assertThat(exitActionS111.onExecuteLatch.await(1, TimeUnit.SECONDS), is(true));
		assertThat(entryActionS112.onExecuteLatch.await(1, TimeUnit.SECONDS), is(true));
		assertThat(exitActionS112.onExecuteLatch.await(1, TimeUnit.SECONDS), is(false));

		assertThat(entryActionS111.stateContexts.size(), is(1));
		assertThat(exitActionS111.stateContexts.size(), is(1));
		assertThat(entryActionS112.stateContexts.size(), is(1));
		assertThat(exitActionS112.stateContexts.size(), is(0));
	}

}
