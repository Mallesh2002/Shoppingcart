package com.ecom.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.aspectj.apache.bcel.util.ClassPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;
import com.ecom.service.UserService;
import com.ecom.util.AppConstant;

@Service
public class USerServiceImpl implements UserService {

	 @Autowired
	 private UserRepository userRepo;
	 
	 @Autowired
	 private BCryptPasswordEncoder passwordEncoder;
	 
	@Override
	public UserDtls saveUser(UserDtls user) {
		// TODO Auto-generated method stub
		user.setRole("ROLE_USER");
		user.setIsEnable(true);
		user.setAccountNonLocked(true);
		user.setFailedAttempt(0);
	
		String encodePassword=passwordEncoder.encode(user.getPassword());
		user.setPassword(encodePassword);
		UserDtls saveuser=userRepo.save(user);
		return saveuser;
	}

	@Override
	public UserDtls getUserByEmail(String email) {
		// TODO Auto-generated method stub
		return userRepo.findByEmail(email);
	}


	@Override
	public List<UserDtls> getUsers(String role) {
		List<UserDtls> users=	userRepo.findByRole(role);
		return users;
	}

	@Override
	public Boolean updatedAccountStatus(int id, Boolean status) {
		Optional<UserDtls> findByUsers=userRepo.findById(id);
		
		if(findByUsers.isPresent())
		{
			UserDtls userDtls=findByUsers.get();
			userDtls.setIsEnable(status);
			userRepo.save(userDtls);
			return true;
		}
		return false;
	}

	@Override
	public void increaseFailedAttempt(UserDtls user) {
		int attempt=user.getFailedAttempt()+1;
		user.setFailedAttempt(attempt);
		userRepo.save(user);
		
	}

	@Override
	public void userAccountLock(UserDtls user) {
		// TODO Auto-generated method stub
		user.setAccountNonLocked(false);
		user.setLockTime(new Date());
		userRepo.save(user);
		
	}

	@Override
	public boolean unlockAccountTimeExpired(UserDtls user) {
		long lockTime = user.getLockTime().getTime();
		long unLockTime = lockTime + AppConstant.UNLOCK_DURATION_TIME;

		long currentTime = System.currentTimeMillis();

		if (unLockTime < currentTime) {
			user.setAccountNonLocked(true);
			user.setFailedAttempt(0);
			user.setLockTime(null);
			userRepo.save(user);
			return true;
		}

		return false;
	}

	@Override
	public void resetAttempt(int userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateUserResetToken(String email, String resetToken) {
		UserDtls userbyemail=userRepo.findByEmail(email);
		userbyemail.setResetToken(resetToken);
		userRepo.save(userbyemail);
		
	}

	@Override
	public UserDtls getUserByToken(String token) {
		// TODO Auto-generated method stub
		UserDtls userbytoken=userRepo.findByResetToken(token);
		return userbytoken;
	}

	@Override
	public UserDtls updateUser(UserDtls user) {
		return userRepo.save(user);
	}

	@Override
	public UserDtls updateUserProfile(UserDtls user, MultipartFile img) {

		UserDtls dbUser = userRepo.findById(user.getId()).get();

		if (!img.isEmpty()) {
			dbUser.setProfileImage(img.getOriginalFilename());
		}

		if (!ObjectUtils.isEmpty(dbUser)) {

			dbUser.setName(user.getName());
			dbUser.setMobileNumber(user.getMobileNumber());
			dbUser.setAddress(user.getAddress());
			dbUser.setCity(user.getCity());
			dbUser.setState(user.getState());
			dbUser.setPincode(user.getPincode());
			dbUser = userRepo.save(dbUser);
		}

		try {
			if (!img.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
						+ img.getOriginalFilename());

//			System.out.println(path);
				Files.copy(img.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dbUser;
	}

	@Override
	public UserDtls saveAdmin(UserDtls user) {
		user.setRole("ROLE_ADMIN");
		user.setIsEnable(true);
		user.setAccountNonLocked(true);
		user.setFailedAttempt(0);
		String encodepassword=passwordEncoder.encode(user.getPassword());
		user.setPassword(encodepassword);
		
		UserDtls saveuser=userRepo.save(user);
		
		return saveuser;
	}

	@Override
	public Boolean existsEmail(String email) {
		// TODO Auto-generated method stub
		return userRepo.existsByEmail(email);
	}

}
