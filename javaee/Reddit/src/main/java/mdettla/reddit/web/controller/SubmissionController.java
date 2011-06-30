package mdettla.reddit.web.controller;

import mdettla.reddit.domain.Submission;
import mdettla.reddit.service.SubmissionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/submissions")
public class SubmissionController {

	private final SubmissionService submissionService;

	@Autowired
	public SubmissionController(SubmissionService submissionService) {
		this.submissionService = submissionService;
	}

	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public void setupFormAdd(Model model) {
		model.addAttribute(new Submission());
	}

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public String processSubmitAdd(@ModelAttribute Submission submission) {
		submissionService.create(submission);
		return "redirect:/";
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable long id) {
		Submission submission = submissionService.findById(id);
		return new ModelAndView("submissions/details", "submission", submission);
	}
}