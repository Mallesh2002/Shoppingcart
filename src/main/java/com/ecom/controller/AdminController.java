package com.ecom.controller;



import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
	private final CommonUtil commonUtil = new CommonUtil();
	
	@Autowired
	CategoryService categoryservice;
	
	@Autowired
	ProductService productservice;
	
	@Autowired
	private UserService userservice;
	
	@Autowired
	private OrderService orderservice;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@ModelAttribute
	public void getUserDetails(Principal p,Model m)
	{
		if(p!=null)
		{
			String email=p.getName();
			UserDtls userdtls=userservice.getUserByEmail(email);
			m.addAttribute("user", userdtls);
		}
		List<Category> allActiveCategory = categoryservice.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}
	
	
	
	@GetMapping("/")
	public String index() {
		
		return "admin/index";
	}
	
	@GetMapping("/profile")
	public String profile() {
		
		return "/admin/profile";
	}
	
	@GetMapping("/loadAddProduct")
	public String loadAddproduct(Model m) {
		List<Category> categories=categoryservice.getAllCategory();
		m.addAttribute("categories", categories);
		
		return "admin/add_product";
	}
	
	@GetMapping("/category")
	public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "2") Integer pageSize) {
		// m.addAttribute("categorys", categoryService.getAllCategory());
		Page<Category> page = categoryservice.getAllCategorPagination(pageNo, pageSize);
		List<Category> categorys = page.getContent();
		m.addAttribute("categorys", categorys);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/category";
	}
	
	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException 
	{
		String imagename = file != null ? file.getOriginalFilename() : "default.jpg";
		category.setImageName(imagename);
		Boolean existCategory = categoryservice.existCategory(category.getName());
		if (existCategory) 
		{
			session.setAttribute("errorMsg", "Category Name already exists");
			System.out.println("Not saved !internal server error");
		} 
		else
		{
			Category saveCategory = categoryservice.saveCategory(category);
			if (ObjectUtils.isEmpty(saveCategory)) 
			{
				session.setAttribute("errorMsg", "Not saved !internal server error");
				System.out.println("Not saved !internal server error");
			} 
			else 
			{
				File savefile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());
				System.out.println(path);
				long l = Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println(l);
				session.setAttribute("succMsg", "Saved successfully");
				System.out.println("success");
			}
		}

		return "redirect:/admin/category";
	}
	
	@GetMapping("/search-order")
	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

		if (orderId != null && orderId.length() > 0) {

			ProductOrder order = orderservice.getOrdersByOrderId(orderId.trim());

			if (ObjectUtils.isEmpty(order)) {
				session.setAttribute("errorMsg", "Incorrect orderId");
				m.addAttribute("orderDtls", null);
			} else {
				m.addAttribute("orderDtls", order);
			}

			m.addAttribute("srch", true);
		} else {
//			List<ProductOrder> allOrders = orderService.getAllOrders();
//			m.addAttribute("orders", allOrders);
//			m.addAttribute("srch", false);

			Page<ProductOrder> page = orderservice.getAllOrdersPagination(pageNo, pageSize);
			m.addAttribute("orders", page);
			m.addAttribute("srch", false);

			m.addAttribute("pageNo", page.getNumber());
			m.addAttribute("pageSize", pageSize);
			m.addAttribute("totalElements", page.getTotalElements());
			m.addAttribute("totalPages", page.getTotalPages());
			m.addAttribute("isFirst", page.isFirst());
			m.addAttribute("isLast", page.isLast());

		}
		return "/admin/orders";

	}
	
	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id,HttpSession session)
	{
		Boolean deletecategory=categoryservice.deleteCategory(id);
		if(deletecategory)
		{
			session.setAttribute("succMsg", "category delete successfully");
		}
		else 
		{
		   session.setAttribute("errorMsg", "something wrong on server");
		}
		
		
		return "redirect:/admin/category";	
	}
	
	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id,Model m)
	{
		
		m.addAttribute("category", categoryservice.getCategoryById(id));
		return "admin/edit_category";
	}
	
	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category,@RequestParam("file") MultipartFile file,HttpSession session) throws IOException
	{
		Category oldcategory=categoryservice.getCategoryById(category.getId());
		String imageName=file!=null ? file.getOriginalFilename():oldcategory.getImageName();
		if(!ObjectUtils.isEmpty(oldcategory)) {
			oldcategory.setName(category.getName());
			oldcategory.setIsActive(category.getIsActive());
			oldcategory.setImageName(imageName);	
		}
		
		 Category updatecategory=categoryservice.saveCategory(oldcategory);
		 
		 if (!ObjectUtils.isEmpty(updatecategory)) {

				if (!file.isEmpty()) {
					File saveFile = new ClassPathResource("static/img").getFile();

					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
							+ file.getOriginalFilename());

					// System.out.println(path);
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				}

				session.setAttribute("succMsg", "Category update success");
			} else {
				session.setAttribute("errorMsg", "something wrong on server");
			}

		 
		return "redirect:/admin/loadEditCategory/"+category.getId();
	}
	
	@PostMapping("/saveProduct")
   public String saveproduct(@ModelAttribute Product product,@RequestParam("file") MultipartFile image,HttpSession session) throws IOException
   {
	 String imagename=image.isEmpty()? "default.jpg" :image.getOriginalFilename();
	 product.setImage(imagename);
	 product.setDiscount(0);
	 product.setDiscountPrice(product.getPrice());
	  Product saveproduct=productservice.saveProduct(product);
	  if(!ObjectUtils.isEmpty(saveproduct))
		 
	  {
		  File savefile=new ClassPathResource("static/img").getFile();
		  Path path=Paths.get(savefile.getAbsolutePath()+ File.separator+"product_img"+File.separator+image.getOriginalFilename());
		  System.out.println(path);
		  Files.copy(image.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
		  session.setAttribute("succMsg", "Product saved successfully");
	  }
	  else
	  {
		  session.setAttribute("errorMsg","Something wrong on server");
	  }
	   return "redirect:/admin/loadAddProduct";   
   }
	

	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

//		List<Product> products = null;
//		if (ch != null && ch.length() > 0) {
//			products = productService.searchProduct(ch);
//		} else {
//			products = productService.getAllProducts();
//		}
//		m.addAttribute("products", products);

		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {
			page = productservice.searchProductPagination(pageNo, pageSize, ch);
		} else {
			page = productservice.getAllProductsPagination(pageNo, pageSize);
		}
		m.addAttribute("products", page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/products";
	}

	
	
	@GetMapping("/deleteProduct/{id}")
	public String loadViewProductt(@PathVariable int id,HttpSession session)
	{
		Boolean deleteproduct=productservice.deleteProduct(id);
		if(deleteproduct)
		{
			session.setAttribute("succMsg", "Product delete success");
		}
		else
		{
			session.setAttribute("errorMsg","Something wrong on server");
		}	
		return "redirect:/admin/products";	
	}
	
	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id,Model m)
	{
		m.addAttribute("product",productservice.getProductById(id));
		m.addAttribute("categories", categoryservice.getAllCategory());
		return "admin/edit_product";	
	}
	
	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product,@RequestParam("file") MultipartFile image,HttpSession session,Model m)
	{
		Product updateproduct=productservice.updateProduct(product, image);
		if(!ObjectUtils.isEmpty(updateproduct))
		{
			session.setAttribute("succMsg", "Product update success");
		}
		else
		{
			session.setAttribute("errorMsg","Something wrong on server");
		}

		
		return "redirect:/admin/editProduct/"+product.getId();
	}
	
	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {
		List<UserDtls> users = null;
		if (type == 1) {
			users = userservice.getUsers("ROLE_USER");
		} else {
			users = userservice.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType",type);
		m.addAttribute("users", users);
		return "/admin/users";
	}

	
	@GetMapping("/updateSts")
	public String updateUserAccountStatus(@RequestParam Boolean status,@RequestParam int id,HttpSession session)
	{
		Boolean f=userservice.updatedAccountStatus(id,status);
		if(f)
		{
			session.setAttribute("succMsg", "Account status updated");
		}
		else {
			session.setAttribute("errorMsg", "something wrong on server");
		}
		
		return "redirect:/admin/users";
	}
	
	@GetMapping("/orders")
	public String getAllOrders(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {


		Page<ProductOrder> page = orderservice.getAllOrdersPagination(pageNo, pageSize);
		m.addAttribute("orders", page.getContent());
		m.addAttribute("srch", false);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "/admin/orders";
	}
	
	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id,@RequestParam Integer st,HttpSession session)
	{
		OrderStatus[] orderstatus=OrderStatus.values();
		
		String status=null;
		
		for(OrderStatus o:orderstatus)
		{
			if(o.getId().equals(st))
			{
				status=o.getName();
			}
		}
		
		ProductOrder updateOrder = orderservice.updateOrderStatus(id, status);
		
		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		} else {
			session.setAttribute("errorMsg", "status not updated");
		}
		
		return "redirect:/admin/orders";
	}
	
	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userservice.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		} else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/admin/profile";
	}
	
	private UserDtls getLoggedInUserDetails(Principal p) {
		UserDtls user=userservice.getUserByEmail(p.getName());
		return user;
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

		
	  return 	"redirect:/admin/profile";
	}
	
	@GetMapping("/add-admin")
	public String loadAdminAdd() {
		
		return "/admin/add_admin";
	}
	
	@PostMapping("save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user,@RequestParam("img") MultipartFile file,HttpSession session)
	{
		String imageName= file.isEmpty() ? "default.jpg" :file.getOriginalFilename();
		user.setProfileImage(imageName);
		UserDtls saveuser=userservice.saveAdmin(user);
		
		if(!ObjectUtils.isEmpty(saveuser))
		{
			if(!file.isEmpty())
			{
				File savefile;
				try {
					savefile = new ClassPathResource("static/img").getFile();
					Path path=Paths.get(savefile.getAbsolutePath()+File.separator+"profile_img"+File.separator+file.getOriginalFilename());
					Files.copy(file.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			session.setAttribute("succMsg", "Register successfully");
		}else {
			session.setAttribute("errorMsg", "something wrong on server");
		}
		return "redirect:/admin/add-admin";
		
		
	}


}
