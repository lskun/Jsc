package net.spring.controller;

import net.spring.bean.Person;
import net.spring.dao.PersonDAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.support.HttpRequestHandlerServlet;


@Controller
public class HelloWorldController {
	@Autowired
	PersonDAO personDAO;
	@RequestMapping(value="/add",method={RequestMethod.POST})
	public String helloWorld(Person person,ModelMap modelMap){
		System.out.println(person.getName());

		personDAO.save(person);
		System.out.println(person.getName());
		modelMap.put("person",person);
		
		return "/hello";
	}
}
