package com.ecom.service.impl;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Category;
import com.ecom.repository.CategoryRepository;
import com.ecom.service.CategoryService;

@Service
public class CategoryServiceimpl implements CategoryService{
	
	@Autowired
	public CategoryRepository categoryrepo;

	@Override
	public Category saveCategory(Category category) {
		// TODO Auto-generated method stub
		return categoryrepo.save(category);
	}

	@Override
	public List<Category> getAllCategory() {
		// TODO Auto-generated method stub
		return categoryrepo.findAll();
	}
	
	
	@Override
	public Boolean existCategory(String name) {
		return categoryrepo.existsByName(name);
	}

	@Override
	public Boolean deleteCategory(int id) {
		// TODO Auto-generated method stub
		Category category=categoryrepo.findById(id).orElse(null);
		
		if(!ObjectUtils.isEmpty(category)) {
			categoryrepo.delete(category);
			return true;
		}
		return false;
	
	}

	@Override
	public Category getCategoryById(int id) {
		Category category=categoryrepo.findById(id).orElse(null);
	
		return category;
	}

	@Override
	public List<Category> getAllActiveCategory() {
		List<Category> categories=categoryrepo.findByIsActiveTrue();
		return categories;
	}

	@Override
	public Page<Category> getAllCategorPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable=PageRequest.of(pageNo, pageSize);
		
		return categoryrepo.findAll(pageable);
	}



}
