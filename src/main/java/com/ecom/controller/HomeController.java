package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;

import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private CategoryService categoryservice;
	
	@Autowired
	private ProductService productservice;
	
	@Autowired
	private UserService userservice;
	
	@Autowired
	private CommonUtil commonutil;
	
	@Autowired
	private CartService cartservice;

    HomeController(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
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
	
	
	
    @GetMapping("/")
	public String index(Model m) {

		List<Category> allActiveCategory = categoryservice.getAllActiveCategory().stream()
				.sorted((c1, c2) -> c2.getId().compareTo(c1.getId())).limit(6).toList();
		List<Product> allActiveProducts = productservice.getAllActiveProducts("").stream()
				.sorted((p1, p2) -> p2.getId().compareTo(p1.getId())).limit(8).toList();
		m.addAttribute("category", allActiveCategory);
		m.addAttribute("products", allActiveProducts);
		return "index";
	}
	@GetMapping("/signin")
	public String  login() 
	{
		
		return "login";
	}
	@GetMapping("/register")
	public String  register() 
	{
		
		
		return "register";
	}
	
	@GetMapping("/products")
	public String products(Model m,@RequestParam(value="category",defaultValue = "")  String category,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(defaultValue = "") String ch){
		System.out.println("category"+category);
		List<Category> categories=categoryservice.getAllActiveCategory();
		m.addAttribute("paramValue", category);
		m.addAttribute("categories", categories);
//		List<Product> products=productservice.getAllActiveProducts(category);
//		m.addAttribute("products", products);
		//	System.out.println("products size "+products.size());
			Page<Product> page = null;
			if (StringUtils.isEmpty(ch)) {
				page = productservice.getAllActiveProductPagination(pageNo, pageSize, category);
				System.out.println("ch is empty");
				System.out.println(page.getSize());
				
		} else {
			page = productservice.searchActiveProductPagination(pageNo, pageSize, category, ch);
			System.out.println("ch is non empty");
		}
		
	List<Product> products = page.getContent();
//		if(products1.isEmpty()) {
//			System.out.println("products ia emoty");
//		}
//		else {
//			System.out.println(products1.get(0));
//		}
//	
		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		return "products";
	}
	
	
//	
//	@GetMapping("/products")
//	public String products(Model m, @RequestParam(value = "category", defaultValue = "") String category,
//			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
//			@RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize,
//			@RequestParam(defaultValue = "") String ch) {
//
//		List<Category> categories = categoryService.getAllActiveCategory();
//		m.addAttribute("paramValue", category);
//		m.addAttribute("categories", categories);
//
////		List<Product> products = productService.getAllActiveProducts(category);
////		m.addAttribute("products", products);
//		Page<Product> page = null;
//		if (StringUtils.isEmpty(ch)) {
//			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
//		} else {
//			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
//		}
//
//		List<Product> products = page.getContent();
//		m.addAttribute("products", products);
//		m.addAttribute("productsSize", products.size());
//
//		m.addAttribute("pageNo", page.getNumber());
//		m.addAttribute("pageSize", pageSize);
//		m.addAttribute("totalElements", page.getTotalElements());
//		m.addAttribute("totalPages", page.getTotalPages());
//		m.addAttribute("isFirst", page.isFirst());
//		m.addAttribute("isLast", page.isLast());
//
//		return "product";
//	}
	
	@GetMapping("/product/{id}")
	public String product(@PathVariable int id, Model m) 
	{
		Product productById = productservice.getProductById(id);
		m.addAttribute("product", productById);
		return "view_product";
	}

	
	@PostMapping("/saveUser")
	public String saveuser(@ModelAttribute UserDtls  user,@RequestParam("img") MultipartFile file,HttpSession session) throws IOException
	{
		
		
		Boolean existsEmail = userservice.existsEmail(user.getEmail());

		if (existsEmail) {
			session.setAttribute("errorMsg", "Email already exist");
		} else {
		String  imageName=file.isEmpty()? "default.jpg" :file.getOriginalFilename();
		user.setProfileImage(imageName);
		UserDtls saveUser=userservice.saveUser(user);
		if(!ObjectUtils.isEmpty(saveUser))
		{
			if(!file.isEmpty())
			{
				File saveFile=new ClassPathResource("static/img").getFile();
				Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+"profile_img"+File.separator+file.getOriginalFilename());
				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Saved successfully");
		}
		else {
			session.setAttribute("errorMsg", "something wrong on server");
		}
		}
		return "redirect:/register";
		
	}
	
   @GetMapping("/forgot-password")
	public String showforgotPassword() {
		return "forgot_password";
	}
   
   @PostMapping("/forgot-password")
  	public String processForgotPassword(@RequestParam String email,HttpSession session, HttpServletRequest request) throws UnsupportedEncodingException, MessagingException {
	   
	  UserDtls userByEmail= userservice.getUserByEmail(email);
	  if(ObjectUtils.isEmpty(userByEmail))
	  {
		  session.setAttribute("errorMsg", "invalid Email");
	  }
	  else {
		  String resetToken = UUID.randomUUID().toString();
		  userservice.updateUserResetToken(email, resetToken);

			// Generate URL :
			// http://localhost:8080/reset-password?token=sfgdbgfswegfbdgfewgvsrg

			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

			Boolean sendMail = commonutil.sendMail(url, email);
		 if(sendMail)
		 {
			  session.setAttribute("succMsg", "Please check your email..Password Rest link sent");
		 }
		 else{
			 session.setAttribute("errorMsg", "Something wromg on server ! Email is not send");
		 }
	  }
  		return "redirect:/forgot-password";
  	}
     
   
   @GetMapping("/reset-password")
  	public String resetPassword(@RequestParam String token,HttpSession session,Model m) {
	   
	   UserDtls user=userservice.getUserByToken(token);
	   
	   if(ObjectUtils.isEmpty(user))
	   {
		   m.addAttribute("errorMsg", "Your link is invalid or expired");
		   return "error";
	   }
		m.addAttribute("token", token);
  		return "reset_password";
  	}
   
   
   @PostMapping("/reset-password")
  	public String resetPassword(@RequestParam String token,@RequestParam String password ,Model m ) {
	   
	   UserDtls user=userservice.getUserByToken(token);
	   
	   if(ObjectUtils.isEmpty(user))
	   {
		   m.addAttribute("errorMsg", "Your link is invalid or expired");
		   return "error";
	   }
	   else {
		   user.setPassword(passwordEncoder.encode(password));
		   user.setResetToken(null);
		   userservice.updateUser(user);
		   m.addAttribute("errorMsg", "Password change successfully");

			return "error";
	   }
	
  	}
   
	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch, Model m) {
		List<Product> searchProducts = productservice.searchProduct(ch);
		m.addAttribute("products", searchProducts);
		List<Category> categories = categoryservice.getAllActiveCategory();
		m.addAttribute("categories", categories);
		return "products";

	}
}
