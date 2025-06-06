package com.ecom.controller;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Cart;
import com.ecom.model.Category;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    private final CommonUtil commonUtil;

    private final BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private CategoryService categoryservice;
	
	@Autowired
	private UserService userservice;
	
	@Autowired
	private CartService cartservice;
	
	@Autowired
	private OrderService orderservice;
	

	private UserDtls userDtls;

    UserController(BCryptPasswordEncoder passwordEncoder, CommonUtil commonUtil) {
        this.passwordEncoder = passwordEncoder;
        this.commonUtil = commonUtil;
    }
	
	@GetMapping("/")
	public String home()
	{
		
		return "/user/home";
		
	}
	
	@ModelAttribute
	public void getUserDetails(Principal p,Model m)
	{
		if(p!=null)
		{
			String email=p.getName();
			UserDtls userdtls=userservice.getUserByEmail(email);
			m.addAttribute("user", userdtls);
			Integer countCart = cartservice.getCountCart(userdtls.getId());
			m.addAttribute("countCart", countCart);
		}
		List<Category> allActiveCategory = categoryservice.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}
	
	@GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid,@RequestParam Integer uid,HttpSession session)
	{
		System.out.println("inside save");
	Cart savecart=	cartservice.saveCart(pid, uid);
	if(ObjectUtils.isEmpty(savecart))
	{
		session.setAttribute("errorMsg", "product added to the cart is failed");
	}
	else {
		session.setAttribute("succMsg", "product added succesfully");
	}
		
		
		return "redirect:/product/"+pid;
	}
	
	
	@GetMapping("/cart")
	public String loadcourtpage(Principal p,Model m)
	{
		UserDtls user=getLoggedInUserDetails(p);
		
		List<Cart> carts=cartservice.getCartsByUser(user.getId());
		
		m.addAttribute("carts", carts);
		
		if(carts.size()>0)
		{
			Double totalOrderPrice=carts.get(carts.size()-1).getTotalOrderPrice();
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		
		return "/user/cart";
		
		
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		UserDtls user=userservice.getUserByEmail(p.getName());
		return user;
	}
	
	@GetMapping("/success")
	public String loadSuccess() {
		return "/user/success";
	}
	
	@GetMapping("/orders")
	public String orderpage(Principal p,Model m) {
		UserDtls user=getLoggedInUserDetails(p);
		List<Cart> carts=cartservice.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if(carts.size()>0)
		{
			Double orderPrice=carts.get(carts.size()-1).getTotalOrderPrice();
			Double totalOrderPrice=carts.get(carts.size()-1).getTotalOrderPrice()+250+100;
			m.addAttribute("orderPrice", orderPrice);
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/order";
	}
	
	
	@PostMapping("/save-order")
	public String  saveOrder(Principal p,@ModelAttribute  OrderRequest request) throws Exception {
		UserDtls user=getLoggedInUserDetails(p);
		orderservice.saveOrder(user.getId(), request);
		
		return "redirect:/user/success";
		
	}
	
	@GetMapping("/update-status")
	public String updateOrderStatus(@RequestParam Integer id,@RequestParam Integer st,HttpSession session)
	{
		OrderStatus[] orders=OrderStatus.values();
		String status=null;
		
		for(OrderStatus orderStatus:orders)
		{
		 	if(orderStatus.getId().equals(st))
		 	{
		 		status=orderStatus.getName();
		 	}
		}
		
		ProductOrder updateOrder= orderservice.updateOrderStatus(id, status);
		
		
		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		} else {
			session.setAttribute("errorMsg", "status not updated");
		}
		return "redirect:/users/user-orders";
	}
	
	
	@GetMapping("/profile")
	public String profile() {
		
		return "/user/profile";
	}
	
	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userservice.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		} else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/user/profile";
	}

	
	@GetMapping("/cartQuantityUpdate")
	public String quantityUpdate(String sy,int cid)
	{
		cartservice.updateQuantity(sy,  cid);
		
		return "redirect:/user/cart";
	}
	
	@GetMapping("/user-orders")
	public String myOrder(Principal p,Model m)
	{
		UserDtls loginuser=getLoggedInUserDetails(p);
		
		List<ProductOrder> orders= orderservice.getOrdersByUser(loginuser.getId());
		
		m.addAttribute("orders", orders);
		
		return "/user/my_orders";
		
	}
	@PostMapping("/change-password")
	public String changePassword(@RequestParam String currentPassword,@RequestParam String newPassword,Principal p,HttpSession session)
	{
		UserDtls user=getLoggedInUserDetails(p);
		
		Boolean matches=passwordEncoder.matches(currentPassword, user.getPassword());
		
		if(matches)
		
		{
			String encodepassword=passwordEncoder.encode(newPassword);
			user.setPassword(encodepassword);
			UserDtls updateUser=userservice.updateUser(user);
			if (ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Password not updated !! Error in server");
			} else {
				session.setAttribute("succMsg", "Password Updated sucessfully");
			}
		}
		else {
			session.setAttribute("errorMsg", "Current Password incorrect");
		}

		
	  return 	"redirect:/user/profile";
	}
	
}
