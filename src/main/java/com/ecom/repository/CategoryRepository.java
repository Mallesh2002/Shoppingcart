package com.ecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.Category;
import java.util.List;


public interface CategoryRepository extends JpaRepository<Category, Integer>{
	
	public Boolean existsByName(String name);
	
	public List<Category> findByIsActiveTrue();
	

}
