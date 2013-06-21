package net.spring.dao;

import net.spring.bean.Person;

public interface PersonDAO {
	void save(Person person);
	void del(Person person);
	void update(Person person);
	void searchAll();
}