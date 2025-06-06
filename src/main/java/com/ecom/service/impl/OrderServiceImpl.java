package com.ecom.service.impl;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ecom.model.Cart;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.UserRepository;
import com.ecom.service.CartService;
import com.ecom.service.OrderService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

    private final UserRepository userRepository;
	
	@Autowired
	CartService cartservice;
	
	   @Autowired
      CommonUtil commonUtil;
	
	@Autowired
	ProductOrderRepository productOrderRepository;
	
	

    OrderServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

	@Override
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception {
		
		List<Cart> carts=cartservice.getCartsByUser(userid);
		
		for(Cart cart:carts)
		{
			ProductOrder productorder=new ProductOrder();
			
			productorder.setOrderId(UUID.randomUUID().toString());
			
			productorder.setOrderDate(LocalDate.now());
			
			productorder.setProduct(cart.getProduct());
			
			productorder.setPrice(cart.getProduct().getDiscountPrice());
			
			productorder.setQuantity(cart.getQuantity());
			
			productorder.setUser(cart.getUser());
			
			productorder.setStatus(OrderStatus.IN_PROGRESS.getName());
			
			productorder.setPaymentType(orderRequest.getPaymentType());
			
			OrderAddress orderaddress=new OrderAddress();
			
			orderaddress.setFirstName(orderRequest.getFirstName());
			
			orderaddress.setLastName(orderRequest.getLastName());
			
			orderaddress.setEmail(orderRequest.getEmail());
			
			orderaddress.setMobileNo(orderRequest.getMobileNo());
			
			orderaddress.setAddress(orderRequest.getAddress());
			
			orderaddress.setCity(orderRequest.getCity());
			
			orderaddress.setState(orderRequest.getState());
			
			orderaddress.setPincode(orderRequest.getPincode());
			
			productorder.setOrderAddress(orderaddress);
			
			ProductOrder saveorder=productOrderRepository.save(productorder);
			
			commonUtil.sendMailForProductOrder(saveorder, "success");
			
			
		}
		
		
		
	}

	@Override
	public List<ProductOrder> getOrdersByUser(Integer userId) {
		List<ProductOrder> orders=productOrderRepository.findByUserId(userId);
		return orders;
	}

	@Override
	public ProductOrder updateOrderStatus(Integer id, String status) {
		Optional<ProductOrder> productsorders=productOrderRepository.findById(id);
		
		if(productsorders.isPresent())
		{
			ProductOrder productOrder=productsorders.get();
			productOrder.setStatus(status);
			ProductOrder updateproductOrder=productOrderRepository.save(productOrder);
			return updateproductOrder;
			
		}
		
		
		return null;
	}

	@Override
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable=(Pageable) PageRequest.of(pageNo, pageSize);
		return productOrderRepository.findAll(pageable);
	}

	@Override
	public ProductOrder getOrdersByOrderId(String orderId) {
		
		return productOrderRepository.findByOrderId(orderId);
	}

}
