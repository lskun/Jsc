package net.spring.dao.imp;

import org.springframework.jdbc.core.JdbcTemplate;
import net.spring.bean.Person;
import net.spring.dao.PersonDAO;

public class PersonDAOIMP implements PersonDAO{

	JdbcTemplate jdbcTemplate;
	public void del(Person person) {
		// TODO Auto-generated method stub
			}

	public void save(Person person) {
		// TODO Auto-generated method stub
		jdbcTemplate.update("insert into person values(?,?,?)",
				new Object[]{person.getId(),person.getName(),person.getPassword()});
	}

	public void searchAll() {
		// TODO Auto-generated method stub
		
	}

	public void update(Person person) {
		// TODO Auto-generated method stub
		
	}
public JdbcTemplate getJdbcTemplate() {
return jdbcTemplate;
}
public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
this.jdbcTemplate = jdbcTemplate;
}
}

