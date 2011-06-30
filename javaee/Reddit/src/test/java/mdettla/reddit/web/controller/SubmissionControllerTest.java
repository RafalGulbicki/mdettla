package mdettla.reddit.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import mdettla.reddit.domain.Submission;
import mdettla.reddit.service.InMemorySubmissionService;
import mdettla.reddit.service.SubmissionService;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

public class SubmissionControllerTest {

	private SubmissionService submissionService;
	private SubmissionController controller;

	@Before
	public void setUp() {
		submissionService = new InMemorySubmissionService();
		controller = new SubmissionController(submissionService);
	}

	@Test
	public void testSetupFormAdd() {
		// prepare
		Model model = new ExtendedModelMap();
		// test
		controller.setupFormAdd(model);
		// verify
		Submission submission = (Submission)model.asMap().get("submission");
		assertNotNull(submission);
	}

	@Test
	public void testProcessSubmitAdd() {
		// prepare
		Submission submission = new Submission();
		submission.setTitle("foo");
		// test
		String viewName = controller.processSubmitAdd(submission);
		// verify
		Submission expected = submissionService.findAll().iterator().next();
		assertNotNull(expected);
		assertEquals("foo", expected.getTitle());
		assertEquals("redirect:/", viewName);
	}

	@Test
	public void testDetails() {
		// prepare
		Submission submission = new Submission();
		submission.setId(5L);
		submission.setTitle("foo");
		submissionService.create(submission);
		// test
		ModelAndView modelAndView = controller.details(5L);
		// verify
		ModelMap model = modelAndView.getModelMap();
		String view = modelAndView.getViewName();
		assertEquals("submissions/details", view);
		Submission submissionInModel = (Submission)model.get("submission");
		assertNotNull(submissionInModel);
		assertEquals(submissionInModel.getId(), submission.getId());
		assertEquals(submissionInModel.getTitle(), submission.getTitle());
	}
}