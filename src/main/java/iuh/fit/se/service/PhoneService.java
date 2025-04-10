package iuh.fit.se.service;

public interface PhoneService {
	boolean sendOtp(String phone);
	
	boolean verifyOtp(String phone, String otp);
}
