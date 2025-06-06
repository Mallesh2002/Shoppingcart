package com.ecom.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Cart;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import com.ecom.service.CartService;

@Service
public class CartServiceImpl implements CartService{
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ProductRepository productRepository;

	@Override
	public Cart saveCart(Integer productId, Integer userId) {
		
		UserDtls userdtls = userRepository.findById(userId)
			    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

			Product product = productRepository.findById(productId)  // Ensure you're using productId, not userId
			    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
		
		Cart cartStatus=cartRepository.findByProductIdAndUserId(productId, userId);
		
		Cart cart=null;
		
		if(ObjectUtils.isEmpty(cartStatus))
		{
			cart=new Cart();
			cart.setUser(userdtls);
			cart.setProduct(product);
			cart.setQuantity(1);
			cart.setTotalPrice(1*product.getDiscountPrice());
			
		}
		else
		{
			cart=cartStatus;
			cart.setQuantity(cart.getQuantity()+1);
			cart.setTotalPrice(cart.getQuantity()*cart.getProduct().getDiscountPrice());
			
		}
		Cart savecart=cartRepository.save(cart);
		
		
		
		return savecart;
	}

	@Override
	public List<Cart> getCartsByUser(Integer userId) {
		List<Cart> carts=cartRepository.findByUserId(userId);

		Double totalOrderPrice = 0.0;
		
		List<Cart> updatecart=new ArrayList<>();
		for(Cart c:carts) {
			Double totalprice=(c.getProduct().getDiscountPrice()*c.getQuantity());
			c.setTotalPrice(totalprice);
			totalOrderPrice+=totalprice;
			c.setTotalOrderPrice(totalOrderPrice);
			updatecart.add(c);
			
		}
		return updatecart;
	}

	@Override
	public Integer getCountCart(Integer userId) {
		// TODO Auto-generated method stub
		Integer countByuserid=cartRepository.countByUserId(userId);
		
		return countByuserid;
	}

	@Override
	public void updateQuantity(String sy, Integer cid) {
		
		Cart cart=cartRepository.findById(cid).get();
		int updateQuantity;
		
		if(sy.equalsIgnoreCase("de"))
		{
			updateQuantity=cart.getQuantity()-1;
			if(updateQuantity<=0)
			{
				cartRepository.delete(cart);
			}
			{
				cart.setQuantity(updateQuantity);
				cartRepository.save(cart);
			}
		}
		else {
			updateQuantity=cart.getQuantity()+1;
			cart.setQuantity(updateQuantity);
			cartRepository.save(cart);
		}
		
	}

}
