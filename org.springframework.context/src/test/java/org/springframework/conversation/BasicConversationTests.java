/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.conversation;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.conversation.manager.ConversationManager;
import org.springframework.conversation.manager.ConversationStore;
import org.springframework.conversation.scope.ConversationResolver;

/**
 * Basic conversation management tests.
 * 
 * @author Micha Kiener
 * @since 3.1
 */
public class BasicConversationTests {
	private static ConfigurableApplicationContext context;
	private static ConversationManager manager;
	private static ConversationStore store;
	private static ConversationResolver resolver;

	@BeforeClass
	public static void setUp() {
		context = loadContext(getContextLocation());
		manager = context.getBean(ConversationManager.class);
		store = context.getBean(ConversationStore.class);
		resolver = context.getBean(ConversationResolver.class);
	}

	@Test
	public void testContext() {
		assertNotNull(context);
		assertNotNull(manager);
		assertNotNull(resolver);
		assertNotNull(store);
	}

	@Test
	public void testTemporaryConversation() {
		ConversationalBean bean = (ConversationalBean) context.getBean("testBean");
		assertNotNull(bean);
		assertNull(bean.getName());
		String id = resolver.getCurrentConversationId();
		assertNotNull(id);
		Conversation conversation = store.getConversation(id);
		assertNotNull(conversation);
		assertTrue(conversation.isTemporary());
		Object attribute = conversation.getAttribute("testBean");
		assertNotNull(attribute);
		assertSame(bean, attribute);

		conversation.end(ConversationEndingType.SUCCESS);
		assertTrue(conversation.isEnded());
		assertNull(resolver.getCurrentConversationId());
	}

	@Test
	public void testNewConversation() {
		Conversation conversation = manager.beginConversation(false, JoinMode.NEW);
		assertNotNull(conversation);
		assertFalse(conversation.isTemporary());
		assertSame(conversation, manager.getCurrentConversation());

		ConversationalBean bean = (ConversationalBean) context.getBean("testBean");
		assertNotNull(bean);

		conversation.end(ConversationEndingType.SUCCESS);
		assertTrue(conversation.isEnded());
		assertNull(resolver.getCurrentConversationId());
		assertNull(manager.getCurrentConversation());
	}

	@Test
	public void testRootConversation() {
		Conversation conversation = manager.beginConversation(false, JoinMode.ROOT);
		assertNotNull(conversation);
		assertFalse(conversation.isTemporary());
		assertSame(conversation, manager.getCurrentConversation());

		ConversationalBean bean = (ConversationalBean) context.getBean("testBean");
		assertNotNull(bean);

		conversation.end(ConversationEndingType.SUCCESS);
		assertTrue(conversation.isEnded());
		assertNull(resolver.getCurrentConversationId());
		assertNull(manager.getCurrentConversation());
	}

	@Test
	public void testRootConversationFailure() {
		Conversation conversation = manager.beginConversation(false, JoinMode.ROOT);
		assertNotNull(conversation);
		assertFalse(conversation.isTemporary());
		assertSame(conversation, manager.getCurrentConversation());

		try {
			manager.beginConversation(false, JoinMode.ROOT);
			fail("IllegalStateException must be thrown as there is a current conversation in place already.");
		} catch (IllegalStateException e) {
			// must happen!
		}

		conversation.end(ConversationEndingType.SUCCESS);
		assertTrue(conversation.isEnded());
		assertNull(resolver.getCurrentConversationId());
		assertNull(manager.getCurrentConversation());
	}

	@Test
	public void testNestedConversation() {
		Conversation conversation = manager.beginConversation(false, JoinMode.ROOT);
		assertNotNull(conversation);
		assertFalse(conversation.isTemporary());
		assertSame(conversation, manager.getCurrentConversation());

		ConversationalBean bean = (ConversationalBean) context.getBean("testBean");
		assertNotNull(bean);

		Conversation nestedConversation = manager.beginConversation(false, JoinMode.NESTED);
		assertNotNull(nestedConversation);
		assertSame(nestedConversation, manager.getCurrentConversation());
		assertNotSame(conversation, nestedConversation);

		assertSame(bean, context.getBean("testBean"));

		nestedConversation.end(ConversationEndingType.SUCCESS);
		assertSame(conversation, manager.getCurrentConversation());

		conversation.end(ConversationEndingType.SUCCESS);
		assertTrue(conversation.isEnded());
		assertTrue(nestedConversation.isEnded());
		assertNull(resolver.getCurrentConversationId());
		assertNull(manager.getCurrentConversation());
	}

	@Test
	public void testNestedConversationEndingFailure() {
		Conversation conversation = manager.beginConversation(false, JoinMode.ROOT);
		assertNotNull(conversation);
		assertFalse(conversation.isTemporary());
		assertSame(conversation, manager.getCurrentConversation());

		Conversation nestedConversation = manager.beginConversation(false, JoinMode.NESTED);
		assertNotNull(nestedConversation);
		assertNotSame(conversation, nestedConversation);

		try {
			conversation.end(ConversationEndingType.SUCCESS);
			fail("There must be an IllegalStateException as trying to end a parent conversation without having its nested conversation ended first");
		} catch (IllegalStateException e) {
			// must happen
		}

		nestedConversation.end(ConversationEndingType.SUCCESS);
		assertSame(conversation, manager.getCurrentConversation());

		conversation.end(ConversationEndingType.SUCCESS);
		assertTrue(conversation.isEnded());
		assertTrue(nestedConversation.isEnded());
		assertNull(resolver.getCurrentConversationId());
		assertNull(manager.getCurrentConversation());
	}

	protected static String getContextLocation() {
		return "org/springframework/conversation/conversationTestContext.xml";
	}

	protected static ConfigurableApplicationContext loadContext(String configLocation) {
		return new GenericXmlApplicationContext(getContextLocation());
	}
}